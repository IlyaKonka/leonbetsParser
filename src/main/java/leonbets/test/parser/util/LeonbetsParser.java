package leonbets.test.parser.util;

import leonbets.test.parser.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class LeonbetsParser {

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

    @Value("${spring.parser.threads.count}")
    private int maxConcurrentTasks;

    @Value("${spring.parser.events.count}")
    private int eventsPerLeague;

    public void parse() {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Semaphore semaphore = new Semaphore(maxConcurrentTasks);

        try {
            List<String> chosenSports = List.of(sportList.split(SPLITTER));

            List<Sport> responseSports = webClient.mutate()
                    .baseUrl(baseUrl)
                    .build()
                    .get()
                    .uri(sportsEndpoint)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<Sport>() {
                    })
                    .collectList()
                    .block();

            if (CollectionUtils.isEmpty(responseSports)) {
                log.error("No sports found");
                return;
            }

            Map<String, Sport> sportMap = responseSports.stream()
                    .collect(Collectors.toMap(Sport::name, Function.identity()));

            CompletableFuture.allOf(
                    chosenSports.stream()
                            .map(sportMap::get)
                            .filter(Objects::nonNull)
                            .flatMap(sport -> processSport(sport, executor, semaphore).stream())
                            .toArray(CompletableFuture[]::new)
            ).join();

        } finally {
            executor.close();
        }
    }

    private List<CompletableFuture<Void>> processSport(Sport sport, ExecutorService executor, Semaphore semaphore) {
        if (CollectionUtils.isEmpty(sport.regions())) {
            log.error("No regions found for sport: {}", sport.name());
            return List.of();
        }

        return sport.regions().stream()
                .filter(region -> region.leagues() != null)
                .flatMap(region ->
                        region.leagues().stream()
                                .filter(League::top)
                                .map(league -> Map.entry(region, league))
                )
                .map(entry -> {
                    Region region = entry.getKey();
                    League league = entry.getValue();
                    return CompletableFuture.runAsync(() -> {
                        try {
                            semaphore.acquire();
                            try {
                                processLeague(sport.name(), region.name(), league);
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }, executor);
                })
                .collect(Collectors.toList());
    }

    private void processLeague(String sportName, String regionName, League league) {
        try {
            String url = baseUrl + String.format(eventsEndpoint, league.id());
            EventEnvelope eventEnvelope = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(EventEnvelope.class)
                    .block();

            if (eventEnvelope == null || CollectionUtils.isEmpty(eventEnvelope.events())) {
                log.error("No events found for sport='{}', league='{}' with id={}",
                        sportName, league.name(), league.id());
                return;
            }

            eventEnvelope.events().stream()
                    .limit(eventsPerLeague)
                    .map(event -> new LeonbetsPrinter(sportName, regionName, league.name(), event, null))
                    .forEach(this::processMarket);

        } catch (Exception e) {
            log.error("Failed to process league={} with id={} for sport={}: {}",
                    league.name(), league.id(), sportName, e.getMessage(), e);
        }
    }

    private void processMarket(LeonbetsPrinter printer) {
        try {
            String url = baseUrl + String.format(marketsEndpoint, printer.getEvent().id());
            MarketEnvelope marketEnvelope = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(MarketEnvelope.class)
                    .block();

            printer.setMarketEnvelope(marketEnvelope);

            printer.printToConsole();
            //printer.writeToFile(Path.of("leonbets.txt"));

        } catch (Exception e) {
            log.error("Failed to process market for eventId={}, league='{}', sport='{}': {}",
                    printer.getEvent().id(),
                    printer.getLeagueName(),
                    printer.getSportName(),
                    e.getMessage(), e);
        }
    }
}
