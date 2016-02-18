package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LocaleUtilsTest {

  @Test
  public void sillyJacoco() {
    new LocaleUtils();
  }

  @Test
  public void lang() {
    assertEquals("es", LocaleUtils.parse("es").iterator().next().getLanguage().toLowerCase());
  }

  @Test
  public void langCountry() {
    assertEquals("es", LocaleUtils.parse("es-ar").iterator().next().getLanguage().toLowerCase());
    assertEquals("ar", LocaleUtils.parse("es-ar").iterator().next().getCountry().toLowerCase());
  }

  @Test
  public void langCountryVariant() {
    assertEquals("ja", LocaleUtils.parse("ja-JP").iterator().next().getLanguage().toLowerCase());
    assertEquals("jp", LocaleUtils.parse("ja-JP").iterator().next().getCountry().toLowerCase());
  }
}
