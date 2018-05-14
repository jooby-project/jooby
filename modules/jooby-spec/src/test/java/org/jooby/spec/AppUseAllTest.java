package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;

import org.junit.Test;

import apps.AppUseAll;


public class AppUseAllTest extends RouteSpecTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void useAll() throws Exception {
    routes(new RouteProcessor().process(new AppUseAll(), basedir))
        .next(r -> {
          assertEquals("*", r.method());
          assertEquals("/admin", r.pattern());

          params(r.params());
        })
        .next(r -> {
          assertEquals("*", r.method());
          assertEquals("/admin", r.pattern());

          params(r.params());
        })
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/admin/:id", r.pattern());

          params(r.params());
        })
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/admin/mail/:id", r.pattern());

          params(r.params());
        });
  }
}
