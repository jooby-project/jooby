package org.jooby.issues;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue444 extends ServerFeature {

  {

    parser((type, ctxt) -> {
      if (type.getRawType() == ByteBuffer.class) {
        return ctxt.body(body -> {
          return ByteBuffer.wrap(body.bytes());
        });
      }
      return ctxt.next();
    });

    post("/444", req -> {
      ByteBuffer buffer = req.body().to(ByteBuffer.class);
      return buffer.remaining();
    });
  }

  @Test
  public void shouldAcceptPostWithoutContentType() throws URISyntaxException, Exception {
    request()
        .post("/444")
        .body("abc", null)
        .expect("3");
  }
}
