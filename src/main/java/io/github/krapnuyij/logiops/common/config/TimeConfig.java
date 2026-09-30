package io.github.krapnuyij.logiops.common.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

  private static final Duration DATABASE_TIMESTAMP_TICK = Duration.ofNanos(1_000);

  @Bean
  Clock clock() {
    return databasePrecisionClock(Clock.systemUTC());
  }

  static Clock databasePrecisionClock(Clock sourceClock) {
    return Clock.tick(sourceClock, DATABASE_TIMESTAMP_TICK);
  }
}
