package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.input.ScrollInput;
import com.mojang.blaze3d.platform.SDLEventHandler;
import org.lwjgl.sdl.SDL_MouseWheelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Adjusts wheel events before vanilla reads their offsets; see ScrollInput.
 *
 * <p>Hooks {@code SDLEventHandler#handleMouseWheelEvent} where it stores {@code event.wheel()}:
 * vanilla then reads {@code wheel.x()} and {@code wheel.y()}, applies the macOS Shift axis swap and
 * passes them to {@code MouseHandler#onScroll}. Once per wheel event.
 */
@Mixin(SDLEventHandler.class)
abstract class SDLEventHandlerMixin {
  @ModifyVariable(method = "handleMouseWheelEvent", at = @At("STORE"))
  private SDL_MouseWheelEvent applefix$adjustWheel(SDL_MouseWheelEvent wheel) {
    ScrollInput.adjust(wheel);
    return wheel;
  }
}
