package io.jooby;

import examples.MvcAttributes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RouteAttributeTest {

  @Test
  public void canRetrieveRouteAttributesForMvcAPI() {
    new JoobyRunner(app -> {

      app.decorator(next -> ctx -> {
        String role = (String) ctx.getRoute().attribute("Role");
        String level = (String) ctx.getRoute().attribute("Role.level");

        if (ctx.getRoute().getPattern().equals("/attr/secured/otherpath")) {
          assertEquals("User", role);
          assertEquals("one", level);
        }

        if (ctx.getRoute().getPattern().equals("/attr/secured/subpath")) {
          assertEquals("Admin", role);
          assertEquals("two", level);
        }

        return next.apply(ctx);
      });

      app.mvc(new MvcAttributes());

    }).ready(client -> {
      client.get("/attr/secured/otherpath", rsp -> assertEquals("OK!", rsp.body().string()));

      client.get("/attr/secured/subpath", rsp -> assertEquals("Got it!!", rsp.body().string()));
    });
  }

  @Test
  public void canRetrieveRouteAttributesForScriptAPI() {
    new JoobyRunner(app -> {
      app.decorator(next -> ctx -> {
        String foo = (String) ctx.getRoute().attribute("foo");
        assertEquals("bar", foo);

        return next.apply(ctx);
      });

      app.get("/fb", ctx -> "Hello World!").attribute("foo", "bar");

    }).ready(client -> {
      client.get("/fb", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
      });
    });
  }

}
