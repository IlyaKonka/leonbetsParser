package leonbets.test.parser.boot;

import leonbets.test.parser.util.LeonbetsParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ParserAutoStartConfig {

    private final LeonbetsParser parser;

    @Value("${spring.autostart.parse:true}")
    private boolean autostart;

    @Bean
    public ApplicationRunner parserAutoRunner() {
        return args -> {
            if (autostart) {
                log.info("Auto-starting LeonbetsParser on startup...");
                parser.parse();
            } else {
                log.info("LeonbetsParser autostart is disabled.");
            }
        };
    }
}
