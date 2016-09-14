package org.jooby.ftl;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.jooby.xss.XSS;
import org.junit.Test;

public class Issue476FtlXss extends ServerFeature {

  {
    use(new XSS());

    use(new Ftl());

    get("/", req -> Results.html("org/jooby/ftl/xss").put("input", "<script>alert('xss');</script>"));
  }

  @Test
  public void xssFn() throws Exception {
    request()
        .get("/")
        .expect("<!DOCTYPE html>\n" +
            "<html>\n" +
            "  <body><a href=\"javascript:hello('&#x5C;u003Cscript&#x5C;u003Ealert%28&#x5C;u0027xss&#x5C;u0027%29%3B&#x5C;u003C&#x5C;u002Fscript&#x5C;u003E')\"></a>\n" +
            "  </body>\n" +
            "</html>");
  }

}
