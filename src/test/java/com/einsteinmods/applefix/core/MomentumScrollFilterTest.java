package com.einsteinmods.applefix.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MomentumScrollFilterTest {
  private static final long FRAME_NANOS = 16_000_000L;
  private static final long LIFT_NANOS = 1_000_000_000L;

  private final MomentumScrollFilter filter = new MomentumScrollFilter();

  @Test
  void deviceWithoutTouchesNeverProducesMomentum() {
    assertFalse(filter.isMomentum(0L));
    assertFalse(filter.isMomentum(FRAME_NANOS));
    assertFalse(filter.hasFingersDown());
  }

  @Test
  void scrollingWithFingersDownIsNotMomentum() {
    filter.onFingerDown();
    filter.onFingerDown();

    assertTrue(filter.hasFingersDown());
    assertFalse(filter.isMomentum(LIFT_NANOS));
  }

  @Test
  void eventsRightAfterTheLastFingerLiftsAreMomentum() {
    filter.onFingerDown();
    filter.onFingerDown();
    filter.onFingerUp(LIFT_NANOS - FRAME_NANOS);
    assertFalse(filter.isMomentum(LIFT_NANOS - FRAME_NANOS / 2), "one finger still down");

    filter.onFingerUp(LIFT_NANOS);

    assertTrue(filter.isMomentum(LIFT_NANOS + FRAME_NANOS));
    assertTrue(filter.isMomentum(LIFT_NANOS + 2 * FRAME_NANOS));
  }

  @Test
  void aLongRunKeepsCoastingWhileEventsKeepComing() {
    filter.onFingerDown();
    filter.onFingerUp(LIFT_NANOS);

    long time = LIFT_NANOS;
    for (int i = 0; i < 200; i++) {
      time += FRAME_NANOS;
      assertTrue(filter.isMomentum(time), "event " + i);
    }
  }

  @Test
  void aPauseLongerThanTheGapEndsTheRun() {
    filter.onFingerDown();
    filter.onFingerUp(LIFT_NANOS);
    assertTrue(filter.isMomentum(LIFT_NANOS + FRAME_NANOS));

    long afterPause = LIFT_NANOS + FRAME_NANOS + MomentumScrollFilter.MOMENTUM_GAP_NANOS + 1;

    assertFalse(filter.isMomentum(afterPause), "a mouse wheel after the run counts");
    assertFalse(filter.isMomentum(afterPause + FRAME_NANOS), "the run does not restart");
  }

  @Test
  void aNewTouchEndsTheRun() {
    filter.onFingerDown();
    filter.onFingerUp(LIFT_NANOS);
    assertTrue(filter.isMomentum(LIFT_NANOS + FRAME_NANOS));

    filter.onFingerDown();

    assertFalse(filter.isMomentum(LIFT_NANOS + 2 * FRAME_NANOS));
  }

  @Test
  void extraReleasesDoNotHideLaterTouches() {
    filter.onFingerUp(LIFT_NANOS);
    filter.onFingerUp(LIFT_NANOS);

    filter.onFingerDown();

    assertTrue(filter.hasFingersDown());
    assertFalse(filter.isMomentum(LIFT_NANOS + FRAME_NANOS));
  }
}
