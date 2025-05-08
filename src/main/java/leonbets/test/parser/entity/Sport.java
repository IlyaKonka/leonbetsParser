package leonbets.test.parser.entity;

import java.util.List;

public record Sport(
        String name,
        List<Region> regions
) {
}
