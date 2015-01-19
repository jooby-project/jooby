package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LocaleUtilsTest {

  @Test
  public void lang() {
    assertEquals("es", LocaleUtils.toLocale("es").getLanguage().toLowerCase());
  }

  @Test
  public void langCountry() {
    assertEquals("es", LocaleUtils.toLocale("es-ar").getLanguage().toLowerCase());
    assertEquals("ar", LocaleUtils.toLocale("es-ar").getCountry().toLowerCase());
  }

  @Test
  public void langCountryVariant() {
    assertEquals("ja", LocaleUtils.toLocale("ja_JP_JPr").getLanguage().toLowerCase());
    assertEquals("jp", LocaleUtils.toLocale("ja_JP_JPr").getCountry().toLowerCase());
    assertEquals("jpr", LocaleUtils.toLocale("ja_JP_JPr").getVariant().toLowerCase());
  }
}
