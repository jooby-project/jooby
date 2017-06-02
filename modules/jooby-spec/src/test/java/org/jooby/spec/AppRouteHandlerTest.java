package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.Test;

import apps.AppRouteHandler;
import apps.model.Pet;

public class AppRouteHandlerTest extends RouteSpecTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void allowRouteHandler() throws Exception {

    routes(new RouteProcessor().process(new AppRouteHandler(), basedir))
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/var", r.pattern());
          assertEquals("doc1", r.doc().get());
          assertEquals(Optional.empty(), r.summary());

          params(r.params());

          assertEquals(Pet.class, r.response().type());
          assertEquals(Optional.empty(), r.response().doc());
        });
  }
}
