package leonbets.test.parser;

import leonbets.test.parser.controller.LeonbetsController;
import leonbets.test.parser.entity.Event;
import leonbets.test.parser.entity.MarketEnvelope;
import leonbets.test.parser.util.LeonbetsParserReactive;
import leonbets.test.parser.util.LeonbetsPrinter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest(LeonbetsController.class)
@Import(LeonbetsControllerMockTest.MockConfig.class)
class LeonbetsControllerMockTest {

    @TestConfiguration
    static class MockConfig {
        @Bean
        public LeonbetsParserReactive leonbetsParserReactive() {
            LeonbetsParserReactive mock = mock(LeonbetsParserReactive.class);

            Event mockEvent = new Event(1970326913799811L, "Team A vs Team B", System.currentTimeMillis());
            LeonbetsPrinter printer = new LeonbetsPrinter(
                    "Football", "England", "Premier League", 1970326913799814L, mockEvent, new MarketEnvelope(List.of())
            );

            when(mock.parse()).thenReturn(Flux.just(printer));

            return mock;
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void getParsedMarkets_returnsExpectedResults() {
        webTestClient.get()
                .uri("/api/sports")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(LeonbetsPrinter.class)
                .hasSize(1)
                .value(list -> {
                    LeonbetsPrinter result = list.getFirst();
                    assert result.sportName().equals("Football");
                });
    }
}