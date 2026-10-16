package com.einsteinmods.applefix.input;

import com.einsteinmods.applefix.core.MomentumScrollFilter;
import com.einsteinmods.applefix.core.ScrollPolicy;
import com.mojang.blaze3d.platform.MacosUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_MouseWheelEvent;
import org.lwjgl.sdl.SDL_TouchFingerEvent;

/**
 * Adjusts SDL wheel events before vanilla turns them into scroll offsets: reversing, trackpad speed
 * and momentum filtering on the hotbar, and locking while attacking or using.
 *
 * <p>Runs on the render thread, which polls SDL events and (being the game thread) handles them
 * inline. Per event: a few field reads and option lookups, no allocation; finger and wheel events
 * only, never per frame.
 */
public final class ScrollInput {
  private static final boolean IS_MAC = MacosUtil.IS_MACOS;
  private static final String HINT_ENABLED = "1";
  private static final double PERCENT = 100.0;
  private static final MomentumScrollFilter MOMENTUM = new MomentumScrollFilter();

  private ScrollInput() {}

  /**
   * Before SDL starts: lets macOS deliver momentum (inertial) wheel events when the option is on.
   * SDL reads {@code SDL_HINT_MAC_SCROLL_MOMENTUM} once, while initialising Cocoa (default "0": no
   * momentum). The options are already loaded: {@code Minecraft}'s constructor builds {@code
   * Options} before {@code RenderSystem#initBackendSystem}.
   */
  public static void beforeSdlInit() {
    if (IS_MAC && InputOptions.MOMENTUM_SCROLLING.get()) {
      SDLHints.SDL_SetHint(SDLHints.SDL_HINT_MAC_SCROLL_MOMENTUM, HINT_ENABLED);
    }
  }

  /**
   * Tracks trackpad touches from an SDL event vanilla does not handle itself. One type read off
   * macOS: nothing.
   */
  public static void onUnhandledEvent(long eventAddress) {
    if (!IS_MAC) {
      return;
    }
    int type = SDL_Event.ntype(eventAddress);
    if (type == SDLEvents.SDL_EVENT_FINGER_DOWN) {
      MOMENTUM.onFingerDown();
    } else if (type == SDLEvents.SDL_EVENT_FINGER_UP
        || type == SDLEvents.SDL_EVENT_FINGER_CANCELED) {
      MOMENTUM.onFingerUp(SDL_TouchFingerEvent.ntimestamp(eventAddress));
    }
  }

  /** Rewrites the offsets of a wheel event in place, before vanilla reads them. */
  public static void adjust(SDL_MouseWheelEvent wheel) {
    boolean momentum = IS_MAC && MOMENTUM.isMomentum(wheel.timestamp());
    Minecraft minecraft = Minecraft.getInstance();
    double factor = ScrollPolicy.gameFactor(InputOptions.REVERSE_SCROLLING.get());
    if (minecraft.gui.screen() == null) {
      factor *= hotbarFactor(minecraft.options, wheel, momentum);
    }
    if (factor != ScrollPolicy.UNCHANGED) {
      wheel.x((float) (wheel.x() * factor));
      wheel.y((float) (wheel.y() * factor));
    }
  }

  private static double hotbarFactor(Options options, SDL_MouseWheelEvent wheel, boolean momentum) {
    boolean locked =
        InputOptions.LOCK_HOTBAR_SCROLLING.get()
            && (options.keyAttack.isDown() || options.keyUse.isDown());
    boolean precise =
        IS_MAC && ScrollPolicy.isPrecise(wheel.x(), wheel.y(), MOMENTUM.hasFingersDown());
    return ScrollPolicy.hotbarFactor(
        locked,
        momentum,
        InputOptions.HOTBAR_MOMENTUM.get(),
        precise,
        InputOptions.TRACKPAD_HOTBAR_SPEED.get() / PERCENT,
        InputOptions.REVERSE_HOTBAR_SCROLLING.get());
  }
}
