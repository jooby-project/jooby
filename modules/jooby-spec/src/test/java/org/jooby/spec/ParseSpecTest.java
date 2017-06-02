package org.jooby.spec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.jooby.internal.spec.Context;
import org.jooby.internal.spec.ContextImpl;
import org.jooby.internal.spec.SourceResolverImpl;
import org.jooby.internal.spec.TypeResolverImpl;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ParseSpecTest {

  @Test
  public void parse() throws IOException, ClassNotFoundException {
    Context ctx = new ContextImpl(new TypeResolverImpl(getClass().getClassLoader()),
        new SourceResolverImpl(new File(System.getProperty("user.dir")).toPath()));
    List<RouteSpec> specs = ctx.parseSpec(new Type() {
      @Override
      public String getTypeName() {
        return "apps.App";
      }
    }).get();
    assertNotNull(specs);

    assertEquals(5, specs.size());

    RouteSpec route = specs.get(0);
    assertEquals("GET", route.method());
    assertEquals("/", route.pattern());
    assertEquals("Home page.", route.doc().get());
    assertEquals(Optional.empty(), route.summary());
    assertEquals(0, route.params().size());
    assertEquals(java.lang.String.class, route.response().type());

    route = specs.get(1);
    assertEquals("GET", route.method());
    assertEquals("/api/pets/:id", route.pattern());
    assertEquals("apps.LocalType", route.response().type().getTypeName());
    assertEquals("Get a pet by ID.", route.doc().get());
    assertEquals("Api summary.", route.summary().get());
    assertEquals(ImmutableMap.of(200, "Success", 404, "Not Found"), route.response().statusCodes());
    assertEquals(1, route.params().size());
    assertEquals("id", route.params().get(0).name());
    assertEquals(RouteParamType.PATH, route.params().get(0).paramType());
    assertEquals("int", route.params().get(0).type().getTypeName());
    assertEquals("A pet ID.", route.params().get(0).doc().get());
    assertEquals(null, route.params().get(0).value());

    route = specs.get(2);
    assertEquals("GET", route.method());
    assertEquals("/api/pets", route.pattern());
    assertEquals("java.util.List<apps.LocalType>", route.response().type().getTypeName());
    assertEquals("List all pets.", route.doc().get());
    assertEquals("Api summary.", route.summary().get());
    assertEquals(2, route.params().size());
    assertEquals("start", route.params().get(0).name());
    assertEquals(RouteParamType.QUERY, route.params().get(0).paramType());
    assertEquals("int", route.params().get(0).type().getTypeName());
    assertEquals("Start offset. Optional", route.params().get(0).doc().get());
    assertEquals(0, route.params().get(0).value());
    assertEquals("max", route.params().get(1).name());
    assertEquals(RouteParamType.QUERY, route.params().get(1).paramType());
    assertEquals("Max number of results. Optional", route.params().get(1).doc().get());
    assertEquals("int", route.params().get(1).type().getTypeName());
    assertEquals(200, route.params().get(1).value());

    route = specs.get(3);
    assertEquals("POST", route.method());
    assertEquals("/api/pets", route.pattern());
    assertEquals("Api summary.", route.summary().get());
    assertEquals("apps.LocalType", route.response().type().getTypeName());
    assertEquals(1, route.params().size());
    assertEquals("<body>", route.params().get(0).name());
    assertEquals(RouteParamType.BODY, route.params().get(0).paramType());
    assertEquals("apps.LocalType", route.params().get(0).type().getTypeName());

    route = specs.get(4);
    assertEquals("DELETE", route.method());
    assertEquals("/api/pets/:id", route.pattern());
    assertEquals("Api summary.", route.summary().get());
    assertEquals("apps.LocalType", route.response().type().getTypeName());
    assertEquals(1, route.params().size());
    assertEquals("id", route.params().get(0).name());
    assertEquals(RouteParamType.PATH, route.params().get(0).paramType());
    assertEquals("int", route.params().get(0).type().getTypeName());
    assertEquals(null, route.params().get(0).value());
  }
}
