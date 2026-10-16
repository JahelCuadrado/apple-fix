package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.input.DropShortcut;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Command + drop key throws the hovered stack in containers; see DropShortcut.
 *
 * <p>{@code AbstractContainerScreen#keyPressed} calls {@code KeyEvent#hasControlDown} only when the
 * drop key matches, to pick between throwing one item and the whole stack.
 */
@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin {
  @ModifyExpressionValue(
      method = "keyPressed",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/KeyEvent;hasControlDown()Z"))
  private boolean applefix$commandDropsStack(boolean controlDown) {
    return controlDown || DropShortcut.commandDropsStack();
  }
}
