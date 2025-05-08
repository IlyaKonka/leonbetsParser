package leonbets.test.parser.entity;

import java.util.List;

public record Market(
        String name,
        Boolean open,
        List<Runner> runners
) {
}
