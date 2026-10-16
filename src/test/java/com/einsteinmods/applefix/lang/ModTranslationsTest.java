package com.einsteinmods.applefix.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModTranslationsTest {
  private static final String ENGLISH = "en_us";
  private static final String SPANISH = "es_es";
  private static final String SAMPLE_KEY = "options.applefix.reverseScrolling";

  @Test
  void laterLanguagesOverrideEarlierOnes() {
    Map<String, String> english = load(List.of(ENGLISH));
    Map<String, String> spanish = load(List.of(ENGLISH, SPANISH));

    assertEquals("Reverse Scrolling", english.get(SAMPLE_KEY));
    assertEquals("Invertir desplazamiento", spanish.get(SAMPLE_KEY));
  }

  @Test
  void everyLanguageHasTheSameKeys() {
    assertEquals(load(List.of(ENGLISH)).keySet(), load(List.of(SPANISH)).keySet());
  }

  @Test
  void nothingIsAddedWhenAResourceLoaderAlreadyServesTheMod() {
    Map<String, String> translations = new HashMap<>();

    ModTranslations.appendTo(Set.of("applefix"), List.of(ENGLISH), translations);

    assertTrue(translations.isEmpty());
  }

  @Test
  void unknownOrUnsafeLanguageCodesAreSkipped() {
    assertTrue(load(List.of("xx_xx")).isEmpty());
    assertFalse(load(List.of("../../" + ENGLISH, ENGLISH)).isEmpty());
    assertEquals(load(List.of(ENGLISH)), load(List.of("../../" + ENGLISH, ENGLISH)));
  }

  private static Map<String, String> load(List<String> languageStack) {
    Map<String, String> translations = new HashMap<>();
    ModTranslations.appendTo(Set.of("minecraft"), languageStack, translations);
    return translations;
  }
}
