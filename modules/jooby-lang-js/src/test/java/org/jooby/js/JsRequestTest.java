package org.jooby.js;

import org.jooby.Request;
import org.jooby.internal.js.JsRequest;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class JsRequestTest {

  @Test(expected = UnsupportedOperationException.class)
  public void require() throws Exception {
    new MockUnit(Request.class)
        .run(unit -> {
          new JsRequest(unit.get(Request.class)).require((Object) null);
        });
  }
}
