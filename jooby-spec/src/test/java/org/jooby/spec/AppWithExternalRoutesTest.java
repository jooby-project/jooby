package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.Test;

import apps.AppWithExternalRoutes;
import apps.model.Pet;

public class AppWithExternalRoutesTest extends RouteSpecTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void shouldNotLoadExternalRoutes() throws Exception {

    routes(new RouteProcessor().process(new AppWithExternalRoutes(), basedir))
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/api/pets/:id", r.pattern());
          assertEquals(Optional.empty(), r.doc());
          assertEquals(Optional.empty(), r.summary());

          params(r.params()).next(p -> {
            assertEquals("id", p.name());
          });

          assertEquals(Pet.class, r.response().type());
          assertEquals(Optional.empty(), r.response().doc());
        })
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/api/pets", r.pattern());
          assertEquals(Optional.empty(), r.doc());
          assertEquals(Optional.empty(), r.summary());

          params(r.params()).next(p -> {
            assertEquals("start", p.name());
          }).next(p -> {
            assertEquals("max", p.name());
          });

          assertEquals("java.util.List<apps.model.Pet>", r.response().type().getTypeName());
          assertEquals(Optional.empty(), r.response().doc());
        }).next(r -> {
          assertEquals("POST", r.method());
          assertEquals("/api/pets", r.pattern());
          assertEquals(Optional.empty(), r.doc());
          assertEquals(Optional.empty(), r.summary());

          params(r.params()).next(p -> {
            assertEquals("pet", p.name());
          });

          assertEquals("apps.model.Pet", r.response().type().getTypeName());
          assertEquals(Optional.empty(), r.response().doc());
        });
  }
}
