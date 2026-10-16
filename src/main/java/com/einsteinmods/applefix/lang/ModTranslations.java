package com.einsteinmods.applefix.lang;

import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.locale.Language;
import org.slf4j.Logger;

/**
 * Adds the mod's lang files to vanilla's translations without Fabric API.
 *
 * <p>Vanilla only reads lang files from resource packs, and mod jars become resource packs only
 * through Fabric API's resource loader. When that loader already exposes the mod's namespace, its
 * files are loaded (and can be overridden by resource packs) and nothing is added here. Runs once
 * per language reload.
 */
public final class ModTranslations {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String NAMESPACE = "applefix";
  private static final String LANG_DIRECTORY = "/assets/" + NAMESPACE + "/lang/";
  private static final String LANG_EXTENSION = ".json";
  private static final Pattern LANGUAGE_CODE = Pattern.compile("[a-z0-9_]{2,16}");

  private ModTranslations() {}

  /**
   * Puts this mod's entries for every language of {@code languageStack} into {@code translations},
   * in stack order so later languages win, as vanilla does.
   */
  public static void appendTo(
      Set<String> loadedNamespaces, List<String> languageStack, Map<String, String> translations) {
    if (loadedNamespaces.contains(NAMESPACE)) {
      return;
    }
    for (String languageCode : languageStack) {
      if (LANGUAGE_CODE.matcher(languageCode).matches()) {
        load(languageCode, translations);
      }
    }
  }

  private static void load(String languageCode, Map<String, String> translations) {
    String path = LANG_DIRECTORY + languageCode + LANG_EXTENSION;
    try (InputStream stream = ModTranslations.class.getResourceAsStream(path)) {
      if (stream != null) {
        Language.loadFromJson(stream, translations::put);
      }
    } catch (IOException | JsonParseException e) {
      LOGGER.warn("Failed to load AppleFix translations from {}", path, e);
    }
  }
}
