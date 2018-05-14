package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.jooby.Jooby;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import apps.PetMvcApp;

public class MvcAppTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void mvcApp() throws Exception {

    RouteProcessor processor = new RouteProcessor();

    Jooby app = new PetMvcApp();

    List<RouteSpec> specs = processor.process(app, basedir);

    assertEquals(2, specs.size());

    RouteSpec route = specs.get(0);
    assertEquals("GET", route.method());
    assertEquals("/api/pets/:id", route.pattern());
    assertEquals("Get a pet by ID.", route.doc().get());
    assertEquals("apps.model.Pet", route.response().type().getTypeName());
    assertEquals("Api summary.", route.summary().get());
    assertEquals(ImmutableMap.of(200, "Success", 404, "Not Found"), route.response().statusCodes());
    assertEquals("A {@link Pet} with a <code>200</code> code or <code>404</code>.",
        route.response().doc().get());

    assertEquals(1, route.params().size());
    assertEquals("id", route.params().get(0).name());
    assertEquals(RouteParamType.PATH, route.params().get(0).paramType());
    assertEquals("int", route.params().get(0).type().getTypeName());
    assertEquals("A pet ID.", route.params().get(0).doc().get());
    assertEquals(null, route.params().get(0).value());

    route = specs.get(1);
    assertEquals("GET", route.method());
    assertEquals("/api/pets", route.pattern());
    assertEquals("List pets.", route.doc().get());
    assertEquals("Api summary.", route.summary().get());
    assertEquals("java.util.List<apps.model.Pet>", route.response().type().getTypeName());
    assertEquals("List of pets in\ntwo lines.", route.response().doc().get());

    assertEquals(2, route.params().size());
    assertEquals("start", route.params().get(0).name());
    assertEquals(RouteParamType.QUERY, route.params().get(0).paramType());
    assertEquals("java.util.Optional<java.lang.Integer>",
        route.params().get(0).type().getTypeName());
    assertEquals("Start offset.\nOptional", route.params().get(0).doc().get());
    assertEquals(null, route.params().get(0).value());

    assertEquals("max", route.params().get(1).name());
    assertEquals(RouteParamType.QUERY, route.params().get(1).paramType());
    assertEquals("java.util.Optional<java.lang.Integer>",
        route.params().get(1).type().getTypeName());
    assertEquals(null, route.params().get(1).value());
    assertEquals("Max number of results. Optional", route.params().get(1).doc().get());

  }
}
