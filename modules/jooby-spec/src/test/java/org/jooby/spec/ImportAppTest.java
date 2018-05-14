package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.jooby.Jooby;
import org.junit.Test;

import apps.PetApp;

public class ImportAppTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void importApp() throws Exception {

    RouteProcessor processor = new RouteProcessor();

    Jooby app = new PetApp();

    List<RouteSpec> specs = processor.process(app, basedir);

    assertEquals(2, specs.size());

    RouteSpec route = specs.get(0);
    assertEquals("GET", route.method());
    assertEquals("/api/blogs/:id", route.pattern());
    assertEquals("Get by ID.", route.doc().get());

    assertEquals(1, route.params().size());
    assertEquals("id", route.params().get(0).name());
    assertEquals(RouteParamType.PATH, route.params().get(0).paramType());
    assertEquals("int", route.params().get(0).type().getTypeName());
    assertEquals(null, route.params().get(0).value());

    route = specs.get(1);
    assertEquals("GET", route.method());
    assertEquals("/api/pets/:id", route.pattern());
    assertEquals("apps.model.Pet", route.response().type().getTypeName());
    assertEquals("Get a pet by ID.", route.doc().get());

    assertEquals(1, route.params().size());
    assertEquals("id", route.params().get(0).name());
    assertEquals(RouteParamType.PATH, route.params().get(0).paramType());
    assertEquals("int", route.params().get(0).type().getTypeName());
    assertEquals(null, route.params().get(0).value());

  }
}
