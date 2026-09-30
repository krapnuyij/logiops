package io.github.krapnuyij.logiops.common.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeConfigTest {

  @Test
  void truncatesApplicationTimeToMicrosecondPrecision() {
    Clock nanosecondClock = Clock.fixed(
        Instant.parse("2026-09-30T01:02:03.123456789Z"),
        ZoneOffset.UTC
    );

    Clock databaseClock = TimeConfig.databasePrecisionClock(nanosecondClock);

    assertThat(databaseClock.instant())
        .isEqualTo(Instant.parse("2026-09-30T01:02:03.123456Z"));
  }
}
