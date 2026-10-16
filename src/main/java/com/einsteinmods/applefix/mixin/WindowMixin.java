package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.input.ScrollInput;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.sdl.SDL_Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Sees trackpad finger events; see ScrollInput.
 *
 * <p>{@code SDLEventHandler#pollEvents} passes every event it does not handle (finger events
 * included) to {@code Window#handleEvent}, which ignores finger events. Once per such event, never
 * for keyboard, mouse or wheel events.
 */
@Mixin(Window.class)
abstract class WindowMixin {
  @ModifyVariable(method = "handleEvent", at = @At("HEAD"), argsOnly = true)
  private SDL_Event applefix$trackFingers(SDL_Event event) {
    ScrollInput.onUnhandledEvent(event.address());
    return event;
  }
}
