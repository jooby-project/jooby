/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1599 {

  @ServerTest
  public void issue1599(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.path(
                      "/1599",
                      () -> {
                        app.get(
                                "/",
                                ctx -> {
                                  return toMap(ctx.getRoute());
                                })
                            .tags("local");

                        app.get(
                                "/{id}",
                                ctx -> {
                                  return toMap(ctx.getRoute());
                                })
                            .setAttribute("foo", "foo");
                      })
                  .produces(MediaType.html)
                  .setAttribute("foo", "bar")
                  .tags("top")
                  .summary("1599 API");
            })
        .ready(
            client -> {
              client.get(
                  "/1599",
                  rsp -> {
                    assertEquals(
                        "{foo=bar, produces=[text/html], consumes=[], tags=[local, top],"
                            + " summary=null}",
                        rsp.body().string());
                  });

              client.get(
                  "/1599/123",
                  rsp -> {
                    assertEquals(
                        "{foo=foo, produces=[text/html], consumes=[], tags=[top], summary=null}",
                        rsp.body().string());
                  });
            });
  }

  private Object toMap(Route route) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.putAll(route.getAttributes());
    map.put("produces", route.getProduces());
    map.put("consumes", route.getConsumes());
    map.put("tags", route.getTags());
    map.put("summary", route.getSummary());
    return map;
  }
}
