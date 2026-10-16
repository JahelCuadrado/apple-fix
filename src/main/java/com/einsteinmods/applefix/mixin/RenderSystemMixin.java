package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.input.ScrollInput;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sets SDL hints that must be set before {@code SDL_Init}; see ScrollInput#beforeSdlInit.
 *
 * <p>{@code RenderSystem#initBackendSystem} sets vanilla's hints and calls {@code SDL_Init}. Once
 * per launch.
 */
@Mixin(RenderSystem.class)
abstract class RenderSystemMixin {
  @Inject(method = "initBackendSystem", at = @At("HEAD"))
  private static void applefix$beforeSdlInit(CallbackInfoReturnable<?> cir) {
    ScrollInput.beforeSdlInit();
  }
}
