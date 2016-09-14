package org.jooby.issues;

import org.jooby.csl.XSS;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue475 extends ServerFeature {

  {
    use(new XSS());

    get("/475/text", req -> {
      return req.param("text", "html").value();
    });

    get("/475/js", req -> {
      return req.param("text", "js").value();
    });
  }

  @Test
  public void escapeHtml() throws Exception {
    request()
        .get("/475/text?text=%3Ch1%3EX%3C/h1%3E")
        .expect("&lt;h1&gt;X&lt;&#x2F;h1&gt;");
  }

  @Test
  public void escapeJs() throws Exception {
    request()
        .get("/475/js?text=%3Cscript%3Ealert(%27xss%27)%3C/script%3E")
        .expect("\\u003Cscript\\u003Ealert(\\u0027xss\\u0027)\\u003C\\u002Fscript\\u003E");
  }

}
