package org.jooby;

import java.nio.charset.Charset;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ResponseCharsetFeature extends ServerFeature {

  {
    get("/charset", (req, rsp) -> {
      Charset charset = req.param("charset").toOptional(Charset.class).orElse(rsp.charset());
      rsp.charset(charset).send("text");
    });
  }

  @Test
  public void defCharset() throws Exception {
    request()
        .get("/charset")
        .header("content-type", "text/html;charset=UTF-8")
        .expect(200);
  }

  @Test
  public void customCharset() throws Exception {
    request()
        .get("/charset?charset=UTF-16")
        .header("content-type", "text/html;charset=UTF-16")
        .expect(200);

  }

}
