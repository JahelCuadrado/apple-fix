package com.einsteinmods.applefix.core;

/**
 * Decides how much of a wheel event reaches the game: the factor its x and y offsets are multiplied
 * by. Pure functions over primitives: no state, no allocation.
 */
public final class ScrollPolicy {
  /** Leaves the event as it is. */
  public static final double UNCHANGED = 1.0;

  /** Discards the event. */
  public static final double DISCARDED = 0.0;

  private static final double REVERSED = -1.0;

  private ScrollPolicy() {}

  /** Factor for every wheel event, in menus and in the world. */
  public static double gameFactor(boolean reverseScrolling) {
    return reverseScrolling ? REVERSED : UNCHANGED;
  }

  /**
   * Factor for a wheel event that reaches the hotbar (no screen open).
   *
   * @param locked the player is attacking or using an item and scroll locking is enabled
   * @param momentum the event is inertial scrolling after the fingers left the trackpad
   * @param momentumAllowed momentum scrolling is enabled for the hotbar
   * @param precise the event comes from a trackpad or another precise scrolling device
   * @param trackpadSpeed multiplier for precise events, in (0, 1]
   * @param reverseHotbar hotbar scrolling is reversed
   */
  public static double hotbarFactor(
      boolean locked,
      boolean momentum,
      boolean momentumAllowed,
      boolean precise,
      double trackpadSpeed,
      boolean reverseHotbar) {
    if (locked || (momentum && !momentumAllowed)) {
      return DISCARDED;
    }
    double speed = precise ? trackpadSpeed : UNCHANGED;
    return reverseHotbar ? -speed : speed;
  }

  /**
   * Whether a wheel event comes from a precise scrolling device. On macOS, SDL rounds the offsets
   * of line-based mice up to whole ticks and passes fractional pixel offsets for trackpads and the
   * Magic Mouse, so a fractional offset, or fingers on the trackpad, means a precise device.
   */
  public static boolean isPrecise(float x, float y, boolean fingersDown) {
    return fingersDown || isFractional(x) || isFractional(y);
  }

  private static boolean isFractional(float value) {
    return value != Math.rint(value);
  }
}
