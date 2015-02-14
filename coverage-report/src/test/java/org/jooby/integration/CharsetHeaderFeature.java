package org.jooby.integration;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class CharsetHeaderFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> rsp.send(req.charset()));
  }

  @Test
  public void utf8() throws Exception {
    request()
        .get("/")
        .expect("UTF-8");
  }

  @Test
  public void iso88591() throws Exception {
    request()
        .get("/")
        .header("Content-Type", "text/html;charset=ISO-8859-1")
        .expect("ISO-8859-1");
  }

  @Test
  public void utf16() throws Exception {
    request()
        .get("/")
        .header("Content-Type", "text/html; charset=utf16")
        .expect("UTF-16");
  }
}
