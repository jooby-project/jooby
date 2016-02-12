package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;

import org.junit.Test;

import apps.AppGenericHandler;

public class AppGenericHandlerTest extends RouteSpecTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void genericHandler() throws Exception {

    routes(new RouteProcessor().process(new AppGenericHandler(), basedir))
        .next(r -> {
          assertEquals("*", r.method());
          assertEquals("/1", r.pattern());

          params(r.params());
        })
        .next(r -> {
          assertEquals("*", r.method());
          assertEquals("/2", r.pattern());

          params(r.params());
        });
  }
}
