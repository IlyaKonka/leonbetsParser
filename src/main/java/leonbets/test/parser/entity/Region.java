package leonbets.test.parser.entity;

import java.util.List;

public record Region(
        String name,
        List<League> leagues
) {
}
