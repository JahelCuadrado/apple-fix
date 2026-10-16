package com.einsteinmods.applefix.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.platform.SDLEventHandler;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.resources.language.ClientLanguage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks that every input hook is merged into its vanilla target (outside the mixin package: mixin
 * packages cannot load other classes). Loading each target makes Mixin apply its hooks, failing on
 * a missing injection point ({@code defaultRequire} 1). Runs on Fabric Loader's Knot class loader
 * (fabric-loader-junit, client environment), as in game.
 */
class InputHooksTest {
  private static final String MOD_ID = "applefix$";

  @ParameterizedTest
  @ValueSource(
      classes = {
        SDLEventHandler.class,
        Window.class,
        RenderSystem.class,
        Options.class,
        ControlsScreen.class,
        Minecraft.class,
        AbstractContainerScreen.class,
        ClientLanguage.class
      })
  void everyTargetHasExactlyOneMergedHook(Class<?> target) {
    Method[] methods = target.getDeclaredMethods();

    long hooks = Arrays.stream(methods).filter(m -> m.getName().contains(MOD_ID)).count();

    assertEquals(1, hooks, () -> target.getSimpleName() + " merged methods: " + hooks);
  }
}
