package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue453 extends ServerFeature {

  public static class Form {
    public String text;
  }

  {
    get("/453", req -> {
      return req.param("text", "html").value();
    });

    get("/453/h", req -> {
      return req.header("text", "html").value();
    });

    get("/453/escape-params", req -> {
      return req.params(Form.class, req.param("xss").value("html")).text;
    });

    get("/453/escape-form", req -> {
      return req.form(Form.class, req.param("xss").value("html")).text;
    });

    get("/453/to-escape-form", req -> {
      return req.params(req.param("xss").value("html")).to(Form.class).text;
    });

    err((req, rsp, x) -> {
      rsp.send(x.toMap().get("message"));
    });
  }

  @Test
  public void escape() throws Exception {
    request()
        .get("/453?text=%3Ch1%3EX%3C/h1%3E")
        .expect("&lt;h1&gt;X&lt;/h1&gt;");

    request()
        .get("/453/h")
        .header("text", "<h1>X</h1>")
        .expect("&lt;h1&gt;X&lt;/h1&gt;");
  }

  @Test
  public void escapeForm() throws Exception {
    request()
        .get("/453/escape-form?text=%3Ch1%3EX%3C/h1%3E")
        .expect("&lt;h1&gt;X&lt;/h1&gt;");

    request()
        .get("/453/escape-params?text=%3Ch1%3EX%3C/h1%3E")
        .expect("&lt;h1&gt;X&lt;/h1&gt;");

    request()
        .get("/453/escape-form?text=%3Ch1%3EX%3C/h1%3E&xss=none")
        .expect("<h1>X</h1>");

    request()
        .get("/453/to-escape-form?text=%3Ch1%3EX%3C/h1%3E")
        .expect("&lt;h1&gt;X&lt;/h1&gt;");
  }

}
