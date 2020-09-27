package io.jooby;

import io.jooby.internal.LocaleUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocaleUtilsTest {

  @Test
  public void shouldNotThrow() {
    assertEquals(Optional.empty(), LocaleUtils.parseLocales(null));
    assertEquals(Optional.empty(), LocaleUtils.parseLocales(""));
    assertEquals(Optional.empty(), LocaleUtils.parseLocales("some garbage"));
    assertEquals(Optional.empty(), LocaleUtils.parseLocales("hu-HU, and some garbage"));

    assertEquals(Optional.empty(), LocaleUtils.parseRanges(null));
    assertEquals(Optional.empty(), LocaleUtils.parseRanges(""));
    assertEquals(Optional.empty(), LocaleUtils.parseRanges("some garbage"));
    assertEquals(Optional.empty(), LocaleUtils.parseRanges("hu-HU, and some garbage"));
  }

  @Test
  public void shouldParseRangesCorrectly() {
    String in = "fr-CH, en;q=0.8, de;q=0.7, *;q=0.5, fr;q=0.9";

    Optional<List<Locale.LanguageRange>> opt = LocaleUtils.parseRanges(in);
    assertTrue(opt.isPresent());

    List<Locale.LanguageRange> ranges = opt.get();
    assertEquals(5, ranges.size());

    assertEquals("fr-ch", ranges.get(0).getRange());
    assertEquals(1d, ranges.get(0).getWeight());

    assertEquals("fr", ranges.get(1).getRange());
    assertEquals(0.9d, ranges.get(1).getWeight());

    assertEquals("en", ranges.get(2).getRange());
    assertEquals(0.8d, ranges.get(2).getWeight());

    assertEquals("de", ranges.get(3).getRange());
    assertEquals(0.7d, ranges.get(3).getWeight());

    assertEquals("*", ranges.get(4).getRange());
    assertEquals(0.5d, ranges.get(4).getWeight());
  }

  @Test
  public void shouldParseLocalesCorrectly() {
    String in = "de;q=0.7, de-AT;q=0.6, en;q=0.9, fr;q=0.8, fr-CH";

    Optional<List<Locale>> opt = LocaleUtils.parseLocales(in);
    assertTrue(opt.isPresent());

    List<Locale> locales = opt.get();

    // fr-CH must be the first since it has q=1
    // then all english locales
    // then all french locales
    // then all german locales
    assertEquals(new Locale("fr", "CH"), locales.get(0));
    assertEquals(Arrays.asList("en", "fr", "de"), locales.stream()
        .skip(1) // skip fr-CH
        .map(Locale::getLanguage)
        .distinct()
        .collect(toList()));
  }
}
