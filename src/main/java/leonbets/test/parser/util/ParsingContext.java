package leonbets.test.parser.util;

import leonbets.test.parser.entity.Event;
import leonbets.test.parser.entity.League;
import leonbets.test.parser.entity.MarketEnvelope;

record ParsingContext(String sport, String region, League league) {
    LeonbetsPrinter map(Event event, MarketEnvelope marketEnvelope) {
        return new LeonbetsPrinter(sport, region, league.name(), league.id(), event, marketEnvelope);
    }
}
