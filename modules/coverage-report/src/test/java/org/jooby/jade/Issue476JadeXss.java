package org.jooby.jade;

import org.jooby.Results;
import org.jooby.csl.XSS;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue476JadeXss extends ServerFeature {

  {
    use(new XSS());

    use(new Jade(".html"));

    get("/",
        req -> Results.html("org/jooby/jade/xss").put("input", "<script>alert('xss');</script>"));
  }

  @Test
  public void xssFn() throws Exception {
    request()
        .get("/")
        .expect("<!DOCTYPE html>\n" +
            "<html>\n" +
            "  <body><a href=\"javascript:hello('&amp;#x5C;u003Cscript&amp;#x5C;u003Ealert%28&amp;#x5C;u0027xss&amp;#x5C;u0027%29%3B&amp;#x5C;u003C&amp;#x5C;u002Fscript&amp;#x5C;u003E')\"></a></body>\n" +
            "</html>");
  }

}
