package org.jooby.issues;

import org.jooby.Cookie;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class Issue649 {

  @Test
  public void emptyCookie() {
    assertTrue(Cookie.URL_DECODER.apply("foo=").isEmpty());
    assertTrue(Cookie.URL_DECODER.apply("foo").isEmpty());
    assertTrue(Cookie.URL_DECODER.apply(null).isEmpty());
  }
}
