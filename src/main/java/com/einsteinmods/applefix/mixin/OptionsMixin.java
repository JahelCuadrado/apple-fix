package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.input.InputOptions;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Loads and saves the mod's options in {@code options.txt}.
 *
 * <p>{@code Options#processOptions} visits every stored option, both in {@code Options#load} and
 * {@code Options#save}. {@code Options.FieldAccess} is made accessible by the class tweaker.
 */
@Mixin(Options.class)
abstract class OptionsMixin {
  @Inject(method = "processOptions", at = @At("TAIL"))
  private void applefix$processInputOptions(Options.FieldAccess access, CallbackInfo ci) {
    InputOptions.process(access);
  }
}
