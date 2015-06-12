package org.jooby.pac4j;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.core.context.WebContext;

import com.fasterxml.jackson.databind.SerializationFeature;

public class WebContextFeature extends ServerFeature {

  {
    use(new Jackson().doWith(mapper -> mapper.enable(SerializationFeature.INDENT_OUTPUT)));

    get("/auth/ctx", req -> {
      WebContext ctx = req.require(WebContext.class);
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("fullRequestURL", ctx.getFullRequestURL());
      map.put("requestMethod", ctx.getRequestMethod());
      map.put("requestParameters", ctx.getRequestParameters());
      map.put("scheme", ctx.getScheme());
      map.put("serverName", ctx.getServerName());
      map.put("serverPort", ctx.getServerPort());
      map.put("toString", ctx.toString());
      return map;
    });

    get("/auth/session", req -> {
      WebContext ctx = req.require(WebContext.class);
      ctx.setSessionAttribute("x", "y");
      return req.session().get("x").value();
    });

    use(new Auth().basic());
  }

  @Test
  public void webContext() throws Exception {
    request()
        .get("/auth/ctx?p1=v1")
        .expect("{\n" +
            "  \"fullRequestURL\" : \"http://localhost:" + port + "/auth/ctx\",\n" +
            "  \"requestMethod\" : \"GET\",\n" +
            "  \"requestParameters\" : {\n" +
            "    \"p1\" : [ \"v1\" ]\n" +
            "  },\n" +
            "  \"scheme\" : \"http\",\n" +
            "  \"serverName\" : \"localhost\",\n" +
            "  \"serverPort\" : " + port + ",\n" +
            "  \"toString\" : \"GET /auth/ctx\\n  pattern: /auth/ctx\\n  name: anonymous\\n  vars: {}\\n  consumes: [*/*]\\n  produces: [*/*]\\n\"\n" +
            "}");
  }

  @Test
  public void session() throws Exception {
    request()
        .get("/auth/session")
        .expect("\"y\"");
  }
}
