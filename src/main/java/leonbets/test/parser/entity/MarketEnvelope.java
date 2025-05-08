package leonbets.test.parser.entity;

import java.util.List;

public record MarketEnvelope(
        List<Market> markets
) {
}
