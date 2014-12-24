package org.jooby.internal;

import java.util.Locale;

public class LocaleUtils {

  public static Locale toLocale(final String locale, final String separator) {
    final String[] parts = locale.split(separator);
    if (parts.length == 1) {
      return new Locale(locale, "");
    } else if (parts.length == 2) {
      return new Locale(parts[0], parts[1]);
    } else {
      return new Locale(parts[0], parts[1], parts[2]);
    }
  }
}
