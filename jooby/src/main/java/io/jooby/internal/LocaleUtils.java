/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class LocaleUtils {

  public static Optional<List<Locale.LanguageRange>> parseRanges(final String value) {
    // remove trailing ';' well-formed vs ill-formed
    String wellFormed = value;
    if (wellFormed.endsWith(";")) {
      wellFormed = wellFormed.substring(0, wellFormed.length() - 1);
    }

    try {
      return Optional.of(Locale.LanguageRange.parse(wellFormed)
          .stream()
          .sorted(comparing(Locale.LanguageRange::getWeight).reversed())
          .collect(toList()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public static Optional<List<Locale>> parseLocales(final String value) {
    return parseRanges(value).map(l -> l.stream()
        .map(r -> Locale.forLanguageTag(r.getRange()))
        .collect(toList()));
  }
}
