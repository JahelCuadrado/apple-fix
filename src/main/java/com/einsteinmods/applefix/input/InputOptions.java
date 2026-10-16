package com.einsteinmods.applefix.input;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

/**
 * The mod's input options, edited on the AppleFix settings page and stored in vanilla's {@code
 * options.txt} next to the vanilla options (see OptionsMixin). Their captions are the names the
 * settings page shows.
 */
public final class InputOptions {
  /** Lang key prefix of every option name; the description adds {@code .description}. */
  public static final String CAPTION_PREFIX = "options.applefix.";

  private static final String STORAGE_PREFIX = "applefix.";
  private static final String PERCENT_LABEL = "options.percent_value";

  public static final String REVERSE_SCROLLING_NAME = "reverseScrolling";
  public static final String REVERSE_HOTBAR_NAME = "reverseHotbarScrolling";
  public static final String LOCK_HOTBAR_NAME = "lockHotbarScrolling";
  public static final String MOMENTUM_NAME = "momentumScrolling";
  public static final String HOTBAR_MOMENTUM_NAME = "hotbarMomentum";
  public static final String TRACKPAD_SPEED_NAME = "trackpadHotbarSpeed";

  public static final boolean DEFAULT_REVERSE_SCROLLING = false;
  public static final boolean DEFAULT_REVERSE_HOTBAR_SCROLLING = false;
  public static final boolean DEFAULT_LOCK_HOTBAR_SCROLLING = false;
  public static final boolean DEFAULT_MOMENTUM_SCROLLING = true;
  public static final boolean DEFAULT_HOTBAR_MOMENTUM = false;

  public static final int MIN_TRACKPAD_SPEED_PERCENT = 5;
  public static final int MAX_TRACKPAD_SPEED_PERCENT = 100;
  public static final int DEFAULT_TRACKPAD_SPEED_PERCENT = 25;

  /** Reverses scrolling everywhere, menus included. */
  public static final OptionInstance<Boolean> REVERSE_SCROLLING =
      bool(REVERSE_SCROLLING_NAME, DEFAULT_REVERSE_SCROLLING);

  /** Reverses the direction scrolling moves the hotbar selection. */
  public static final OptionInstance<Boolean> REVERSE_HOTBAR_SCROLLING =
      bool(REVERSE_HOTBAR_NAME, DEFAULT_REVERSE_HOTBAR_SCROLLING);

  /** Ignores hotbar scrolling while the attack or use key is held. */
  public static final OptionInstance<Boolean> LOCK_HOTBAR_SCROLLING =
      bool(LOCK_HOTBAR_NAME, DEFAULT_LOCK_HOTBAR_SCROLLING);

  /**
   * macOS: lets SDL deliver inertial scrolling (the scroll keeps going after the fingers lift).
   * Read once, before SDL starts, so changes apply after a restart.
   */
  public static final OptionInstance<Boolean> MOMENTUM_SCROLLING =
      bool(MOMENTUM_NAME, DEFAULT_MOMENTUM_SCROLLING);

  /** macOS: lets inertial scrolling move the hotbar selection too. */
  public static final OptionInstance<Boolean> HOTBAR_MOMENTUM =
      bool(HOTBAR_MOMENTUM_NAME, DEFAULT_HOTBAR_MOMENTUM);

  /** macOS: how much of a trackpad (precise) scroll reaches the hotbar, in percent. */
  public static final OptionInstance<Integer> TRACKPAD_HOTBAR_SPEED =
      new OptionInstance<>(
          CAPTION_PREFIX + TRACKPAD_SPEED_NAME,
          OptionInstance.noTooltip(),
          (caption, percent) -> Component.translatable(PERCENT_LABEL, caption, percent),
          new OptionInstance.IntRange(MIN_TRACKPAD_SPEED_PERCENT, MAX_TRACKPAD_SPEED_PERCENT),
          DEFAULT_TRACKPAD_SPEED_PERCENT,
          OptionInstance.NO_ACTION);

  private InputOptions() {}

  /**
   * Loads or saves every option through vanilla's {@code Options#processOptions} visitor. All are
   * stored on every system, so a shared {@code options.txt} keeps the Mac-only values.
   */
  public static void process(Options.FieldAccess access) {
    access.process(STORAGE_PREFIX + REVERSE_SCROLLING_NAME, REVERSE_SCROLLING);
    access.process(STORAGE_PREFIX + REVERSE_HOTBAR_NAME, REVERSE_HOTBAR_SCROLLING);
    access.process(STORAGE_PREFIX + LOCK_HOTBAR_NAME, LOCK_HOTBAR_SCROLLING);
    access.process(STORAGE_PREFIX + MOMENTUM_NAME, MOMENTUM_SCROLLING);
    access.process(STORAGE_PREFIX + HOTBAR_MOMENTUM_NAME, HOTBAR_MOMENTUM);
    access.process(STORAGE_PREFIX + TRACKPAD_SPEED_NAME, TRACKPAD_HOTBAR_SPEED);
  }

  private static OptionInstance<Boolean> bool(String name, boolean initialValue) {
    return OptionInstance.createBoolean(CAPTION_PREFIX + name, initialValue);
  }
}
