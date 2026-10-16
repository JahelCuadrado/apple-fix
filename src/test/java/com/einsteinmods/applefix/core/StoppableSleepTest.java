package com.einsteinmods.applefix.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class StoppableSleepTest {
  private static final long SHORT_MILLIS = 120L;
  private static final long LONG_MILLIS = 5_000L;

  @Test
  void sleepsTheWholeTimeWithoutAStop() throws InterruptedException {
    long start = System.nanoTime();

    StoppableSleep.sleep(SHORT_MILLIS, () -> false);

    assertTrue(System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(SHORT_MILLIS));
  }

  @Test
  void stopsWithinOneSliceOnceTheConditionHolds() throws InterruptedException {
    AtomicBoolean stop = new AtomicBoolean();
    Thread.ofPlatform().start(() -> stop.set(true));
    long start = System.nanoTime();

    StoppableSleep.sleep(LONG_MILLIS, stop::get);

    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    assertTrue(elapsedMillis < LONG_MILLIS / 10, () -> "slept " + elapsedMillis + " ms");
  }

  @Test
  void interruptStillThrowsLikeThreadSleep() {
    Thread.currentThread().interrupt();

    assertThrows(InterruptedException.class, () -> StoppableSleep.sleep(LONG_MILLIS, () -> false));
    assertFalse(Thread.interrupted());
  }

  @Test
  void negativeTimeStillFailsLikeThreadSleep() {
    assertThrows(IllegalArgumentException.class, () -> StoppableSleep.sleep(-1L, () -> false));
  }
}
