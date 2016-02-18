package org.jooby.spec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jooby.Jooby;
import org.junit.Test;

import apps.App;
import apps.BodyParam;
import apps.DefBool;
import apps.DefDouble;
import apps.DefInt;
import apps.DefStr;
import apps.NoArgHandler;
import apps.ParamTo;
import apps.ParamToCollection;
import apps.ParamToOptional;
import apps.ReturnNewGenericObjectVar;
import apps.ReturnNewObjectInline;
import apps.ReturnNewObjectVar;
import apps.RouteGroup;

public class RouteProcessorTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath().resolve("src")
      .resolve("test").resolve("java");

  @Test
  public void bodyParam() throws Exception {

    Jooby app = new BodyParam();

    RouteProcessor processor = new RouteProcessor();
    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("POST", route.method());
    assertEquals("/create", route.pattern());
    assertEquals(1, route.params().size());
    assertEquals("<body>", route.params().get(0).name());
    assertEquals(RouteParamType.BODY, route.params().get(0).paramType());
    assertEquals("apps.LocalType", route.params().get(0).type().getTypeName());
  }

  @Test
  public void noArgHandler() throws Exception {
    RouteProcessor processor = new RouteProcessor();

    Jooby app = new NoArgHandler();

    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("GET", route.method());
    assertEquals("/", route.pattern());
    assertEquals("int", route.response().type().getTypeName());

    assertEquals(0, route.params().size());
  }

  @Test
  public void routeGroup() throws Exception {
    RouteProcessor processor = new RouteProcessor();

    Jooby app = new RouteGroup();

    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("GET", route.method());
    assertEquals("/group", route.pattern());
    assertEquals("int", route.response().type().getTypeName());

    assertEquals(0, route.params().size());
  }

  @Test
  public void returnNewObjectInline() throws Exception {
    RouteProcessor processor = new RouteProcessor();

    Jooby app = new ReturnNewObjectInline();

    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("GET", route.method());
    assertEquals("/", route.pattern());
    assertEquals("apps.model.Pet", route.response().type().getTypeName());

    assertEquals(0, route.params().size());
  }

  @Test
  public void returnNewObjectVar() throws Exception {
    RouteProcessor processor = new RouteProcessor();

    Jooby app = new ReturnNewObjectVar();

    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("GET", route.method());
    assertEquals("/", route.pattern());
    assertEquals("apps.LocalType", route.response().type().getTypeName());

    assertEquals(0, route.params().size());
  }

  @Test
  public void returnNewGenericObjectVar() throws Exception {
    RouteProcessor processor = new RouteProcessor();

    Jooby app = new ReturnNewGenericObjectVar();

    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("GET", route.method());
    assertEquals("/", route.pattern());
    assertEquals("java.util.List<apps.LocalType>", route.response().type().getTypeName());

    assertEquals(0, route.params().size());
  }

  @Test
  public void defInt() throws Exception {
    RouteProcessor processor = new RouteProcessor();

    Jooby app = new DefInt();

    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("GET", route.method());
    assertEquals("/", route.pattern());
    assertEquals("int", route.response().type().getTypeName());

    assertEquals(2, route.params().size());
    assertEquals("start", route.params().get(0).name());
    assertEquals(RouteParamType.QUERY, route.params().get(0).paramType());
    assertEquals("int", route.params().get(0).type().getTypeName());
    assertEquals(0, route.params().get(0).value());
    assertTrue(route.params().get(0).optional());

    assertEquals("max", route.params().get(1).name());
    assertEquals(RouteParamType.QUERY, route.params().get(1).paramType());
    assertEquals("int", route.params().get(1).type().getTypeName());
    assertEquals(200, route.params().get(1).value());
    assertTrue(route.params().get(1).optional());
  }

  @Test
  public void defStr() throws Exception {
    RouteProcessor processor = new RouteProcessor();

    Jooby app = new DefStr();

    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("GET", route.method());
    assertEquals("/", route.pattern());
    assertEquals("java.lang.String", route.response().type().getTypeName());

    assertEquals(1, route.params().size());
    assertEquals("value", route.params().get(0).name());
    assertEquals(RouteParamType.QUERY, route.params().get(0).paramType());
    assertEquals("java.lang.String", route.params().get(0).type().getTypeName());
    assertEquals("value", route.params().get(0).value());
    assertTrue(route.params().get(0).optional());
  }

  @Test
  public void defBool() throws Exception {
    RouteProcessor processor = new RouteProcessor();

    Jooby app = new DefBool();

    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("GET", route.method());
    assertEquals("/", route.pattern());
    assertEquals("boolean", route.response().type().getTypeName());

    assertEquals(2, route.params().size());
    assertEquals("t", route.params().get(0).name());
    assertEquals(RouteParamType.QUERY, route.params().get(0).paramType());
    assertEquals("boolean", route.params().get(0).type().getTypeName());
    assertEquals(true, route.params().get(0).value());
    assertTrue(route.params().get(0).optional());

    assertEquals("f", route.params().get(1).name());
    assertEquals(RouteParamType.QUERY, route.params().get(1).paramType());
    assertEquals("boolean", route.params().get(1).type().getTypeName());
    assertEquals(false, route.params().get(1).value());
    assertTrue(route.params().get(1).optional());
  }

  @Test
  public void defDouble() throws Exception {
    RouteProcessor processor = new RouteProcessor();

    Jooby app = new DefDouble();

    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("GET", route.method());
    assertEquals("/", route.pattern());
    assertEquals("double", route.response().type().getTypeName());

    assertEquals(1, route.params().size());
    assertEquals("d", route.params().get(0).name());
    assertEquals(RouteParamType.QUERY, route.params().get(0).paramType());
    assertEquals("double", route.params().get(0).type().getTypeName());
    assertEquals(3.7, route.params().get(0).value());
    assertTrue(route.params().get(0).optional());
  }

  @Test
  public void paramToOptional() throws Exception {
    RouteProcessor processor = new RouteProcessor();

    Jooby app = new ParamToOptional();

    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("GET", route.method());
    assertEquals("/SingleGetOptionalParam", route.pattern());
    assertEquals("java.lang.String", route.response().type().getTypeName());
    assertEquals(Optional.empty(), route.doc());

    assertEquals(3, route.params().size());

    assertEquals("i", route.params().get(0).name());
    assertEquals(RouteParamType.QUERY, route.params().get(0).paramType());
    assertEquals("java.util.Optional<java.lang.Integer>",
        route.params().get(0).type().getTypeName());
    assertTrue(route.params().get(0).optional());

    assertEquals("s", route.params().get(1).name());
    assertEquals(RouteParamType.QUERY, route.params().get(1).paramType());
    assertEquals("java.util.Optional<java.lang.String>",
        route.params().get(1).type().getTypeName());
    assertTrue(route.params().get(1).optional());

    assertEquals("l", route.params().get(2).name());
    assertEquals(RouteParamType.QUERY, route.params().get(2).paramType());
    assertEquals("java.util.Optional<apps.LocalType>",
        route.params().get(2).type().getTypeName());
    assertTrue(route.params().get(2).optional());
  }

  @Test
  public void paramTo() throws Exception {
    RouteProcessor processor = new RouteProcessor();

    Jooby app = new ParamTo();

    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("GET", route.method());
    assertEquals("/SingleGetParam", route.pattern());
    assertEquals(Optional.empty(), route.doc());
    assertEquals("java.lang.String", route.response().type().getTypeName());

    assertEquals(5, route.params().size());

    assertEquals("i", route.params().get(0).name());
    assertEquals(RouteParamType.QUERY, route.params().get(0).paramType());
    assertEquals("java.lang.Integer", route.params().get(0).type().getTypeName());

    assertEquals("s", route.params().get(1).name());
    assertEquals(RouteParamType.QUERY, route.params().get(1).paramType());
    assertEquals("java.lang.String", route.params().get(1).type().getTypeName());

    assertEquals("l", route.params().get(2).name());
    assertEquals(RouteParamType.QUERY, route.params().get(2).paramType());
    assertEquals("apps.LocalType", route.params().get(2).type().getTypeName());

    assertEquals("q", route.params().get(3).name());
    assertEquals(RouteParamType.QUERY, route.params().get(3).paramType());
    assertEquals("java.util.Calendar", route.params().get(3).type().getTypeName());

    assertEquals("u", route.params().get(4).name());
    assertEquals(RouteParamType.FILE, route.params().get(4).paramType());
    assertEquals("org.jooby.Upload", route.params().get(4).type().getTypeName());
  }

  @Test
  public void paramToCollection() throws Exception {
    RouteProcessor processor = new RouteProcessor();

    Jooby app = new ParamToCollection();

    List<RouteSpec> routes = processor.process(app, basedir);
    assertEquals(1, routes.size());
    RouteSpec route = routes.get(0);
    assertEquals("GET", route.method());
    assertEquals("/SingleGetCollectionParam", route.pattern());
    assertEquals("java.lang.String", route.response().type().getTypeName());
    assertEquals(Optional.empty(), route.doc());

    assertEquals(8, route.params().size());

    assertEquals("li", route.params().get(0).name());
    assertEquals(RouteParamType.QUERY, route.params().get(0).paramType());
    assertEquals("java.util.List<java.lang.Integer>", route.params().get(0).type().getTypeName());

    assertEquals("ls", route.params().get(1).name());
    assertEquals(RouteParamType.QUERY, route.params().get(1).paramType());
    assertEquals("java.util.List<java.lang.String>", route.params().get(1).type().getTypeName());

    assertEquals("ll", route.params().get(2).name());
    assertEquals(RouteParamType.QUERY, route.params().get(2).paramType());
    assertEquals("java.util.List<apps.LocalType>",
        route.params().get(2).type().getTypeName());

    assertEquals("si", route.params().get(3).name());
    assertEquals(RouteParamType.QUERY, route.params().get(3).paramType());
    assertEquals("java.util.Set<java.lang.Integer>", route.params().get(3).type().getTypeName());

    assertEquals("ss", route.params().get(4).name());
    assertEquals(RouteParamType.QUERY, route.params().get(4).paramType());
    assertEquals("java.util.Set<java.lang.String>", route.params().get(4).type().getTypeName());

    assertEquals("sl", route.params().get(5).name());
    assertEquals(RouteParamType.QUERY, route.params().get(5).paramType());
    assertEquals("java.util.Set<apps.LocalType>",
        route.params().get(5).type().getTypeName());

    assertEquals("ssi", route.params().get(6).name());
    assertEquals(RouteParamType.QUERY, route.params().get(6).paramType());
    assertEquals("java.util.SortedSet<java.lang.Integer>",
        route.params().get(6).type().getTypeName());

    assertEquals("sss", route.params().get(7).name());
    assertEquals(RouteParamType.QUERY, route.params().get(7).paramType());
    assertEquals("java.util.SortedSet<java.lang.String>",
        route.params().get(7).type().getTypeName());
  }

  @Test
  public void app() throws Exception {

    RouteProcessor processor = new RouteProcessor();

    Jooby app = new App();

    List<RouteSpec> specs = processor.process(app, basedir);

    assertEquals(5, specs.size());

    RouteSpec route = specs.get(0);
    assertEquals("GET", route.method());
    assertEquals("/", route.pattern());
    assertEquals(0, route.params().size());

    route = specs.get(1);
    assertEquals("GET", route.method());
    assertEquals("/api/pets/:id", route.pattern());
    assertEquals("apps.LocalType", route.response().type().getTypeName());
    assertEquals(1, route.params().size());
    assertEquals("id", route.params().get(0).name());
    assertEquals(RouteParamType.PATH, route.params().get(0).paramType());
    assertEquals("int", route.params().get(0).type().getTypeName());
    assertEquals(null, route.params().get(0).value());

    route = specs.get(2);
    assertEquals("GET", route.method());
    assertEquals("/api/pets", route.pattern());
    assertEquals("java.util.List<apps.LocalType>", route.response().type().getTypeName());
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
    assertEquals("apps.LocalType", route.response().type().getTypeName());
    assertEquals(1, route.params().size());
    assertEquals("<body>", route.params().get(0).name());
    assertEquals(RouteParamType.BODY, route.params().get(0).paramType());
    assertEquals("apps.LocalType", route.params().get(0).type().getTypeName());

    route = specs.get(4);
    assertEquals("DELETE", route.method());
    assertEquals("/api/pets/:id", route.pattern());
    assertEquals("apps.LocalType", route.response().type().getTypeName());
    assertEquals(1, route.params().size());
    assertEquals("id", route.params().get(0).name());
    assertEquals(RouteParamType.PATH, route.params().get(0).paramType());
    assertEquals("int", route.params().get(0).type().getTypeName());
    assertEquals(null, route.params().get(0).value());
  }

}
