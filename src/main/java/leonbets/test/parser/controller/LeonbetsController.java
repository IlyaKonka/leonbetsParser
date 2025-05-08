package leonbets.test.parser.controller;

import leonbets.test.parser.util.LeonbetsParserReactive;
import leonbets.test.parser.util.LeonbetsPrinter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class LeonbetsController {
    private final LeonbetsParserReactive parser;

    @GetMapping("/api/sports")
    public Flux<LeonbetsPrinter> getParsedSports() {
        return parser.parse();
    }
}
