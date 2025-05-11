package leonbets.test.parser.util;

import leonbets.test.parser.entity.Event;
import leonbets.test.parser.entity.Market;
import leonbets.test.parser.entity.MarketEnvelope;
import leonbets.test.parser.entity.Runner;
import lombok.AllArgsConstructor;
import lombok.Data;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.lang.String.format;

@AllArgsConstructor
@Data
public class LeonbetsPrinter {
    private String sportName;
    private String regionName;
    private String leagueName;
    private Event event;
    private MarketEnvelope marketEnvelope;

    private static final Map<Path, Object> fileLocks = new ConcurrentHashMap<>();
    private static final String HEADER_FORMAT = "%s, %s %s%n";
    private static final String EVENT_INFO_FORMAT = "\t%s, %s UTC, %d%n";
    private static final String MARKET_NAME_FORMAT = "\t\t%s%n";
    private static final String RUNNER_FORMAT = "\t\t\t%s, %s, %d%n";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    public void printToConsole() {
        synchronized (System.out) {
            System.out.println(generateFormattedOutput());
        }
    }

    public void writeToFile(Path filePath) {
        Object lock = fileLocks.computeIfAbsent(filePath, p -> new Object());
        synchronized (lock) {
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
    }

    private String generateFormattedOutput() {
        return new StringBuilder()
//                .append("[Thread: ").append(Thread.currentThread()).append("]\n")
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
