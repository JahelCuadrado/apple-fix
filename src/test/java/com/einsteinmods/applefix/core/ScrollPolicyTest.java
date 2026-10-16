package com.einsteinmods.applefix.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScrollPolicyTest {
  private static final double SPEED = 0.25;

  @Test
  void gameFactorReversesOnlyWhenAsked() {
    assertEquals(ScrollPolicy.UNCHANGED, ScrollPolicy.gameFactor(false));
    assertEquals(-1.0, ScrollPolicy.gameFactor(true));
  }

  @Test
  void mouseWheelReachesTheHotbarUnchanged() {
    assertEquals(
        ScrollPolicy.UNCHANGED,
        ScrollPolicy.hotbarFactor(false, false, false, false, SPEED, false));
  }

  @Test
  void trackpadEventsAreSlowedDown() {
    assertEquals(SPEED, ScrollPolicy.hotbarFactor(false, false, false, true, SPEED, false));
  }

  @Test
  void reversingTheHotbarFlipsTheSign() {
    assertEquals(-1.0, ScrollPolicy.hotbarFactor(false, false, false, false, SPEED, true));
    assertEquals(-SPEED, ScrollPolicy.hotbarFactor(false, false, false, true, SPEED, true));
  }

  @Test
  void momentumIsDiscardedUnlessAllowed() {
    assertEquals(
        ScrollPolicy.DISCARDED, ScrollPolicy.hotbarFactor(false, true, false, true, SPEED, false));
    assertEquals(SPEED, ScrollPolicy.hotbarFactor(false, true, true, true, SPEED, false));
  }

  @Test
  void lockingDiscardsEverything() {
    assertEquals(
        ScrollPolicy.DISCARDED, ScrollPolicy.hotbarFactor(true, false, true, false, SPEED, true));
  }

  @Test
  void wholeTicksWithoutFingersAreALineBasedMouse() {
    assertFalse(ScrollPolicy.isPrecise(0f, 1f, false));
    assertFalse(ScrollPolicy.isPrecise(-3f, 0f, false));
  }

  @Test
  void fractionalOffsetsOrFingersMeanAPreciseDevice() {
    assertTrue(ScrollPolicy.isPrecise(0f, 0.3f, false));
    assertTrue(ScrollPolicy.isPrecise(-1.5f, 0f, false));
    assertTrue(ScrollPolicy.isPrecise(0f, 1f, true));
  }
}
