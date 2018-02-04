package org.jooby.internal.pac4j2;

import org.jooby.Err;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class Pac4jActionAdapterTest {

  @Test(expected = Err.class)
  public void shouldThrowError401() {
    throwError(401);
  }

  @Test(expected = Err.class)
  public void shouldThrowError403() {
    throwError(403);
  }

  @Test
  public void shouldIgnoreSuccessCode() {
    assertEquals(null, new Pac4jActionAdapter().adapt(200, null));
  }

  @Test
  public void shouldIgnoreRedirect() {
    assertEquals(null, new Pac4jActionAdapter().adapt(302, null));
  }

  private void throwError(int statusCode) {
    try {
      new Pac4jActionAdapter().adapt(statusCode, null);
    } catch (Err x) {
      assertEquals(statusCode, x.statusCode());
      throw x;
    }
  }
}
