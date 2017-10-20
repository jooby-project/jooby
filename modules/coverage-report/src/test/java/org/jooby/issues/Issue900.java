package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.net.URLEncoder;

public class Issue900 extends ServerFeature {

  {
    caseSensitiveRouting(false);

    get("/900/:code", req -> req.param("code").value());
  }

  @Test
  public void shouldNotEncodingPlusSign() throws Exception {
    int ch = '+';
    System.out.println(Integer.toHexString(ch));
    request().get("/900/a+b")
        .expect("a+b");
  }

}
