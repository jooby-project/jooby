package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jooby.Jooby;
import org.junit.Test;

import apps.CompiledApp;

public class CompiledAppTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void appWithDoc() throws Exception {

    RouteProcessor processor = new RouteProcessor();

    Jooby app = new CompiledApp();

    List<RouteSpec> specs = processor.compile(app, basedir, basedir.resolve("target"));

    assertEquals(5, specs.size());

    RouteSpec route = specs.get(0);
    assertEquals("GET", route.method());
    assertEquals("/", route.pattern());
    assertEquals("Home page.", route.doc().get());
    assertEquals(Optional.empty(), route.summary());
    assertEquals(0, route.params().size());
    assertEquals(java.lang.String.class, route.response().type());
    assertEquals(Optional.empty(), route.response().doc());

    route = specs.get(1);
    assertEquals("GET", route.method());
    assertEquals("/api/pets/:id", route.pattern());
    assertEquals("apps.model.Pet", route.response().type().getTypeName());
    assertEquals("Get a Pet by ID.", route.doc().get());
    assertEquals("API pets.", route.summary().get());
    assertEquals(1, route.params().size());
    assertEquals("id", route.params().get(0).name());
    assertEquals(RouteParamType.PATH, route.params().get(0).paramType());
    assertEquals("int", route.params().get(0).type().getTypeName());
    assertEquals(null, route.params().get(0).value());

    route = specs.get(2);
    assertEquals("GET", route.method());
    assertEquals("/api/pets", route.pattern());
    assertEquals("java.util.List<apps.model.Pet>", route.response().type().getTypeName());
    assertEquals(2, route.params().size());
    assertEquals("start", route.params().get(0).name());
    assertEquals(RouteParamType.QUERY, route.params().get(0).paramType());
    assertEquals("int", route.params().get(0).type().getTypeName());
    assertEquals(0, route.params().get(0).value());
    assertEquals("max", route.params().get(1).name());
    assertEquals(RouteParamType.QUERY, route.params().get(1).paramType());
    assertEquals("int", route.params().get(1).type().getTypeName());
    assertEquals(200, route.params().get(1).value());

    route = specs.get(3);
    assertEquals("POST", route.method());
    assertEquals("/api/pets", route.pattern());
    assertEquals("apps.model.Pet", route.response().type().getTypeName());
    assertEquals(1, route.params().size());
    assertEquals("<body>", route.params().get(0).name());
    assertEquals(RouteParamType.BODY, route.params().get(0).paramType());
    assertEquals("apps.model.Pet", route.params().get(0).type().getTypeName());

    route = specs.get(4);
    assertEquals("DELETE", route.method());
    assertEquals("/api/pets/:id", route.pattern());
    assertEquals("apps.model.Pet", route.response().type().getTypeName());
    assertEquals(1, route.params().size());
    assertEquals("id", route.params().get(0).name());
    assertEquals(RouteParamType.PATH, route.params().get(0).paramType());
    assertEquals("int", route.params().get(0).type().getTypeName());
    assertEquals(null, route.params().get(0).value());
  }
}
