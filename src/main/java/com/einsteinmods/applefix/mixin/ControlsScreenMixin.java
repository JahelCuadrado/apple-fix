package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.settings.AppleFixSettingsScreen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the "AppleFix..." button to Options, Controls, on its own row right below the "Mouse
 * Settings..." and "Key Binds..." pair.
 *
 * <p>{@code ControlsScreen#addOptions} adds that pair with {@code
 * OptionsList#addSmall(AbstractWidget, AbstractWidget)}, then the vanilla options. Once per screen
 * opening or resize.
 */
@Mixin(ControlsScreen.class)
abstract class ControlsScreenMixin extends OptionsSubScreen {
  private ControlsScreenMixin(Screen lastScreen, Options options, Component title) {
    super(lastScreen, options, title);
  }

  @Inject(
      method = "addOptions",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/components/OptionsList;addSmall(Lnet/minecraft/client/gui/components/AbstractWidget;Lnet/minecraft/client/gui/components/AbstractWidget;)V",
              shift = At.Shift.AFTER))
  private void applefix$addSettingsButton(CallbackInfo ci) {
    if (list != null) {
      list.addSmall(AppleFixSettingsScreen.openButton(this), null);
    }
  }
}
