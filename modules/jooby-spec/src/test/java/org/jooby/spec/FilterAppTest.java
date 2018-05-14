package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.Test;

import apps.FilterApp;
import apps.LocalType;

public class FilterAppTest extends RouteSpecTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void shouldProcessFilters() throws Exception {

    routes(new RouteProcessor().process(new FilterApp(), basedir))
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/", r.pattern());
          assertEquals("Home page.", r.doc().get());
          assertEquals(Optional.empty(), r.summary());

          params(r.params());

          assertEquals(String.class, r.response().type());
          assertEquals(Optional.empty(), r.response().doc());
        })
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/api/pets/:id", r.pattern());
          assertEquals("Get a Pet by ID.", r.doc().get());
          assertEquals("API pets.", r.summary().get());

          params(r.params()).next(p -> {
            assertEquals("id", p.name());
          });

          assertEquals(LocalType.class, r.response().type());
          assertEquals(Optional.empty(), r.response().doc());
        })
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/api/pets", r.pattern());
          assertEquals(Optional.empty(), r.doc());
          assertEquals("API pets.", r.summary().get());

          params(r.params()).next(p -> {
            assertEquals("start", p.name());
          }).next(p -> {
            assertEquals("max", p.name());
          });

          assertEquals("java.util.List<apps.LocalType>", r.response().type().getTypeName());
          assertEquals(Optional.empty(), r.response().doc());
        })
        .next(r -> {
          assertEquals("POST", r.method());
          assertEquals("/api/pets", r.pattern());
          assertEquals(Optional.empty(), r.doc());
          assertEquals("API pets.", r.summary().get());

          params(r.params()).next(p -> {
            assertEquals("<body>", p.name());
          });

          assertEquals(LocalType.class, r.response().type());
          assertEquals(Optional.empty(), r.response().doc());
        })
        .next(r -> {
          assertEquals("DELETE", r.method());
          assertEquals("/api/pets/:id", r.pattern());
          assertEquals(Optional.empty(), r.doc());
          assertEquals("API pets.", r.summary().get());

          params(r.params()).next(p -> {
            assertEquals("id", p.name());
          });

          assertEquals(204, r.response().statusCode());
          assertEquals(LocalType.class, r.response().type());
          assertEquals(Optional.empty(), r.response().doc());
        });
  }
}
