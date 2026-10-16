package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.input.DropShortcut;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Command + drop key throws the whole stack in the world; see DropShortcut.
 *
 * <p>{@code Minecraft#handleKeybinds} calls {@code Minecraft#hasControlDown} once, for each drop
 * key click: {@code gameMode.dropItem(player, hasControlDown())}.
 */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
  @ModifyExpressionValue(
      method = "handleKeybinds",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;hasControlDown()Z"))
  private boolean applefix$commandDropsStack(boolean controlDown) {
    return controlDown || DropShortcut.commandDropsStack();
  }
}
