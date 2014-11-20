package org.jooby;

import static org.junit.Assert.assertEquals;

import org.jooby.Cookie.Signature;
import org.junit.Test;

public class CookieSignatureTest {

  @Test
  public void sign() throws Exception {
    assertEquals("jooby|qAlLNkSRVE4aZb+tz6avvkVIEmmR30BH8cpr3x9ZdFA",
        Signature.sign("jooby", "124Qwerty"));
  }

  @Test
  public void unsign() throws Exception {
    assertEquals("jooby|qAlLNkSRVE4aZb+tz6avvkVIEmmR30BH8cpr3x9ZdFA",
        Signature.unsign("jooby|qAlLNkSRVE4aZb+tz6avvkVIEmmR30BH8cpr3x9ZdFA", "124Qwerty"));
  }

  @Test
  public void valid() throws Exception {
    assertEquals(true,
        Signature.valid("jooby|qAlLNkSRVE4aZb+tz6avvkVIEmmR30BH8cpr3x9ZdFA", "124Qwerty"));
  }

  @Test
  public void invalid() throws Exception {
    assertEquals(false,
        Signature.valid("jooby|QAlLNkSRVE4aZb+tz6avvkVIEmmR30BH8cpr3x9ZdFA", "124Qwerty"));

    assertEquals(false,
        Signature.valid("joobi|qAlLNkSRVE4aZb+tz6avvkVIEmmR30BH8cpr3x9ZdFA", "124Qwerty"));

    assertEquals(false,
        Signature.valid("joobi#qAlLNkSRVE4aZb+tz6avvkVIEmmR30BH8cpr3x9ZdFA", "124Qwerty"));
  }

}
