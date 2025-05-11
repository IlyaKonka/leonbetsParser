package leonbets.test.parser.util;

import leonbets.test.parser.entity.EventEnvelope;
import leonbets.test.parser.entity.League;
import leonbets.test.parser.entity.MarketEnvelope;
import leonbets.test.parser.entity.Sport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;


@Component
@Slf4j
@RequiredArgsConstructor
public class LeonbetsParserReactive {

    private static final String SPLITTER = ",";

    private final WebClient webClient;

    @Value("${spring.parser.url.base}")
    private String baseUrl;

    @Value("${spring.parser.url.sports}")
    private String sportsEndpoint;

    @Value("${spring.parser.url.events}")
    private String eventsEndpoint;

    @Value("${spring.parser.url.markets}")
    private String marketsEndpoint;

    @Value("${spring.parser.sports}")
    private String sportList;

    @Value("${spring.parser.events.count}")
    private int eventsPerLeague;

    @Value("${spring.parser.threads.count}")
    private int maxConcurrentTasks;

    public Flux<LeonbetsPrinter> parse() {
        List<String> chosenSports = List.of(sportList.split(SPLITTER));

        return webClient.get()
                .uri(baseUrl + sportsEndpoint)
                .retrieve()
                .bodyToFlux(Sport.class)
                .collectMap(Sport::name)
                .flatMapMany(sportMap ->
                        Flux.fromIterable(chosenSports)
                                .map(sportMap::get)
                                .filter(Objects::nonNull)
                                .concatMap(sport -> processSport(sport)
                                        .subscribeOn(Schedulers.boundedElastic()), maxConcurrentTasks)
                )
                .onErrorContinue((e, o) -> log.error("Failed processing object: {}", o, e));
    }

    private Flux<LeonbetsPrinter> processSport(Sport sport) {
        if (CollectionUtils.isEmpty(sport.regions())) {
            log.error("No regions for sport: {}", sport.name());
            return Flux.empty();
        }

        return Flux.fromIterable(sport.regions())
                .filter(region -> region.leagues() != null)
                .flatMap(region ->
                        Flux.fromIterable(region.leagues())
                                .filter(League::top)
                                .flatMap(league -> processLeague(sport.name(), region.name(), league))
                );
    }

    private Flux<LeonbetsPrinter> processLeague(String sportName, String regionName, League league) {
        String url = baseUrl + String.format(eventsEndpoint, league.id());

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(EventEnvelope.class)
                .flatMapMany(env -> {
                    if (env == null || CollectionUtils.isEmpty(env.events())) {
                        log.error("No events for sport={}, league={}", sportName, league.name());
                        return Flux.empty();
                    }

                    return Flux.fromIterable(env.events())
                            .take(eventsPerLeague)
                            .flatMap(event -> processMarket(
                                    new LeonbetsPrinter(
                                            sportName,
                                            regionName,
                                            league.name(),
                                            event,
                                            null
                                    )));
                });
    }

    private Mono<LeonbetsPrinter> processMarket(LeonbetsPrinter printer) {
        String url = baseUrl + String.format(marketsEndpoint, printer.getEvent().id());

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(MarketEnvelope.class)
                .doOnNext(printer::setMarketEnvelope)
                .map(marketEnvelope -> printer)
                .onErrorResume(e -> {
                    log.error("Failed to fetch market for eventId={}: {}", printer.getEvent().id(), e.getMessage());
                    return Mono.empty();
                });
    }
}
