package leonbets.test.parser.entity;

public record Runner(
        Long id,
        String name,
        Boolean open,
        String priceStr
) {
}
