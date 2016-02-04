package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.Test;

import apps.SimilarSig;

public class SimilarSigTest extends RouteSpecTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void shouldNotLoadExternalRoutes() throws Exception {

    routes(new RouteProcessor().process(new SimilarSig(), basedir))
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/1", r.pattern());
          assertEquals("doc1", r.doc().get());
          assertEquals(Optional.empty(), r.summary());

          params(r.params());

          assertEquals(Object.class, r.response().type());
          assertEquals(Optional.empty(), r.response().doc());
        })
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/2", r.pattern());
          assertEquals("doc2", r.doc().get());
          assertEquals(Optional.empty(), r.summary());

          params(r.params());

          assertEquals(Object.class, r.response().type());
          assertEquals(Optional.empty(), r.response().doc());
        });
  }
}
