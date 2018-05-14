package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.jooby.Jooby;
import org.junit.Test;

import apps.AppOverride;

public class AppOverrideTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void appOverride() throws Exception {

    RouteProcessor processor = new RouteProcessor();

    Jooby app = new AppOverride();

    List<RouteSpec> specs = processor.process(app, basedir);

    assertEquals(2, specs.size());

    RouteSpec route = specs.get(0);
    assertEquals("DELETE", route.method());
    assertEquals("/:id", route.pattern());
    assertEquals("void", route.response().type().getTypeName());
    assertEquals(204, route.response().statusCode());

    route = specs.get(1);
    assertEquals("POST", route.method());
    assertEquals("/", route.pattern());
    assertEquals("apps.model.Pet", route.response().type().getTypeName());
    assertEquals(201, route.response().statusCode());

  }
}
