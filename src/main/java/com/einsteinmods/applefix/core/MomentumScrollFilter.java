package com.einsteinmods.applefix.core;

/**
 * Tells inertial (momentum) scroll events apart from scrolling with the fingers on a trackpad.
 *
 * <p>SDL's wheel events carry no scroll phase, but SDL reports trackpad touches as finger events.
 * Momentum events are the wheel events that keep arriving after the last finger left the trackpad:
 * a coasting run that ends at the first gap longer than {@link #MOMENTUM_GAP_NANOS} or at the next
 * touch. Devices that report no touches (mice, Magic Mouse) never start a run, so their events are
 * never classified as momentum.
 *
 * <p>Not thread-safe: fed from the single thread that polls SDL events. No allocation.
 */
public final class MomentumScrollFilter {
  /**
   * Longest pause between two momentum events of one coasting run. Momentum events come once per
   * display refresh (8-17 ms), so a pause this long means the run is over.
   */
  public static final long MOMENTUM_GAP_NANOS = 150_000_000L;

  private int fingersDown;
  private boolean coasting;
  private long lastCoastingEventNanos;

  /** A finger touched the trackpad: whatever scroll follows is driven by the fingers. */
  public void onFingerDown() {
    fingersDown++;
    coasting = false;
  }

  /**
   * A finger left the trackpad (lifted or cancelled). The count never goes negative, so a lost or
   * repeated release cannot hide later touches.
   */
  public void onFingerUp(long timestampNanos) {
    if (fingersDown > 0) {
      fingersDown--;
    }
    if (fingersDown == 0) {
      coasting = true;
      lastCoastingEventNanos = timestampNanos;
    }
  }

  /** Whether at least one finger is on the trackpad. */
  public boolean hasFingersDown() {
    return fingersDown > 0;
  }

  /**
   * Classifies one wheel event. Call once per wheel event, in event order.
   *
   * @return true if the event is momentum generated after the fingers left the trackpad
   */
  public boolean isMomentum(long timestampNanos) {
    if (!coasting || fingersDown > 0) {
      return false;
    }
    if (timestampNanos - lastCoastingEventNanos > MOMENTUM_GAP_NANOS) {
      coasting = false;
      return false;
    }
    lastCoastingEventNanos = timestampNanos;
    return true;
  }
}
