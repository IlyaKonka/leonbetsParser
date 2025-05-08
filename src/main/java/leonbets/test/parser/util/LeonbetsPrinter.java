package leonbets.test.parser.util;

import leonbets.test.parser.entity.Event;
import leonbets.test.parser.entity.Market;
import leonbets.test.parser.entity.MarketEnvelope;
import leonbets.test.parser.entity.Runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.lang.String.format;

public record LeonbetsPrinter(
        String sportName,
        String regionName,
        String leagueName,
        Long leagueId,
        Event event,
        MarketEnvelope marketEnvelope) {

    private static final String HEADER_FORMAT = "%s, %s %s%n";
    private static final String EVENT_INFO_FORMAT = "\t%s, %s UTC, %d%n";
    private static final String MARKET_NAME_FORMAT = "\t\t%s%n";
    private static final String RUNNER_FORMAT = "\t\t\t%s, %s, %d%n";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    public void printToConsole() {
        System.out.println(generateFormattedOutput());
    }

    public void writeToFile(Path filePath) {
        try {
            Files.writeString(
                    filePath,
                    generateFormattedOutput() + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            throw new RuntimeException("Could not write to file: " + filePath, ex);
        }
    }

    private String generateFormattedOutput() {
        return new StringBuilder()
                .append(formatHeader())
                .append(formatEventDetails())
                .append(formatMarkets())
                .toString();
    }

    private String formatHeader() {
        return format(HEADER_FORMAT, sportName, regionName, leagueName);
    }

    private String formatEventDetails() {
        String startTime = DATE_FORMATTER.format(Instant.ofEpochMilli(event.kickoff()));
        return format(EVENT_INFO_FORMAT, event.name(), startTime, event.id());
    }

    private String formatMarkets() {
        StringBuilder marketBuilder = new StringBuilder();
        Map<String, List<Runner>> markets = collectRunnersByMarket(marketEnvelope);

        for (Map.Entry<String, List<Runner>> entry : markets.entrySet()) {
            String marketName = entry.getKey();
            List<Runner> openRunners = entry.getValue().stream()
                    .filter(Runner::open)
                    .toList();

            if (!openRunners.isEmpty()) {
                marketBuilder.append(format(MARKET_NAME_FORMAT, marketName));
                openRunners.forEach(runner ->
                        marketBuilder.append(format(RUNNER_FORMAT,
                                runner.name(), runner.priceStr(), runner.id())));
            }
        }

        return marketBuilder.toString();
    }

    private Map<String, List<Runner>> collectRunnersByMarket(MarketEnvelope marketEnvelope) {
        return marketEnvelope.markets().stream()
                .filter(Market::open)
                .flatMap(market -> market.runners().stream()
                        .map(runner -> Map.entry(market.name(), runner)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        TreeMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }
}
