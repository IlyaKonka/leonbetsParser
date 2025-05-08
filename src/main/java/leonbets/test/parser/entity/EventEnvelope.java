package leonbets.test.parser.entity;

import java.util.List;

public record EventEnvelope(
        List<Event> events
) {
}
