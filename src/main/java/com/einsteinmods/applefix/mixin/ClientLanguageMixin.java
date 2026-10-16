package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.lang.ModTranslations;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Loads the mod's lang files without Fabric API; see ModTranslations.
 *
 * <p>{@code ClientLanguage#loadFrom} fills a map from every resource pack and then freezes it with
 * {@code Map.copyOf(translations)}: the mod's entries go into that map right before. Once per
 * language reload.
 */
@Mixin(ClientLanguage.class)
abstract class ClientLanguageMixin {
  @ModifyArg(
      method = "loadFrom",
      at = @At(value = "INVOKE", target = "Ljava/util/Map;copyOf(Ljava/util/Map;)Ljava/util/Map;"))
  private static Map<String, String> applefix$addModTranslations(
      Map<String, String> translations,
      @Local(argsOnly = true) ResourceManager resourceManager,
      @Local(argsOnly = true) List<String> languageStack) {
    ModTranslations.appendTo(resourceManager.getNamespaces(), languageStack, translations);
    return translations;
  }
}
