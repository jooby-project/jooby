package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;

import org.junit.Test;

import apps.App2Handler;


public class App2HandlerTest extends RouteSpecTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void genericHandler() throws Exception {
    routes(new RouteProcessor().process(new App2Handler(), basedir))
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/p1", r.pattern());

          params(r.params());
        })
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/p2", r.pattern());

          params(r.params());
        })
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/p3", r.pattern());

          params(r.params());
        })
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/p4", r.pattern());

          params(r.params());
        })
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/p5", r.pattern());

          params(r.params());
        });
  }
}
