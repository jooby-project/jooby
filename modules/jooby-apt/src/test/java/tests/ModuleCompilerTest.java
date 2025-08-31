/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests;

import static io.jooby.ParamSource.COOKIE;
import static io.jooby.ParamSource.FLASH;
import static io.jooby.ParamSource.HEADER;
import static io.jooby.ParamSource.PATH;
import static io.jooby.ParamSource.QUERY;
import static io.jooby.ParamSource.SESSION;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.ParamSource;
import io.jooby.Route;
import io.jooby.StatusCode;
import io.jooby.apt.ProcessorRunner;
import io.jooby.exception.MissingValueException;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;
import source.ArrayRoute;
import source.Bean;
import source.GetPostRoute;
import source.JavaBeanParam;
import source.MinRoute;
import source.NoPathRoute;
import source.ParamSourceCheckerContext;
import source.RouteAttributes;
import source.RouteDispatch;
import source.RouteWithMimeTypes;
import source.RouteWithParamLookup;
import source.Routes;
import source.VoidRoute;

public class ModuleCompilerTest {
  @Test
  public void minRoute() throws Exception {
    new ProcessorRunner(new MinRoute())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              assertEquals("/mypath", router.get("/mypath").value());
            });
  }

  @Test
  public void arrayRoute() throws Exception {
    new ProcessorRunner(new ArrayRoute())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);

              Object value = router.get("/mypath").value();
              assertTrue(value instanceof String[]);
              assertArrayEquals(new String[] {"/mypath1", "/mypath2"}, (String[]) value);

              value = router.get("/mybeans").value();
              assertTrue(value instanceof Bean[]);
              assertArrayEquals(new Bean[] {new Bean(1), new Bean(2)}, (Bean[]) value);

              value = router.get("/mymultibeans").value();
              assertTrue(value instanceof Bean[][]);
              assertArrayEquals(new Bean[][] {{new Bean(1)}, {new Bean(2)}}, (Bean[][]) value);
            });
  }

  @Test
  public void routes() throws Exception {
    new ProcessorRunner(new Routes(), Map.of("jooby.returnType", true))
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              assertEquals("/path", router.get("/path").value());
              assertEquals(Arrays.asList("/path/subpath"), router.get("/path/subpath").value());
              assertTrue(router.get("/path/object").value() instanceof Context);
              assertTrue(router.post("/path/post").value() instanceof JavaBeanParam);

              assertEquals(
                  "/path/pathAttributeWork", router.get("/path/pathAttributeWork").value());
              assertEquals("/path/path", router.get("/path/path").value());
              assertEquals("/path/value", router.get("/path/value").value());

              assertEquals("/path/path1", router.get("/path/path1").value());
              assertEquals("/path/path2", router.get("/path/path2").value());

              assertEquals("/path/path1", router.post("/path/path1").value());
              assertEquals("/path/path2", router.post("/path/path2").value());
            });
  }

  @Test
  public void routesWithMimeTypes() throws Exception {
    new ProcessorRunner(new RouteWithMimeTypes())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              assertEquals("/consumes", router.get("/consumes").value());
              assertEquals("/consumes2", router.get("/consumes2").value());
              assertEquals("/produces", router.get("/produces").value());
              assertEquals("/consumes/produces", router.get("/consumes/produces").value());
              assertEquals("/method/produces", router.get("/method/produces").value());
              assertEquals("/class/produces", router.get("/class/produces").value());
              assertEquals("/method/consumes", router.get("/method/consumes").value());
              assertEquals("/class/consumes", router.get("/class/consumes").value());
            });
  }

  @Test
  public void routesWithParamLookup() throws Exception {
    new ProcessorRunner(new RouteWithParamLookup())
        .withRouter(
            (app, source) -> {
              MockRouter router = new MockRouter(app);

              Throwable t =
                  assertThrows(
                      MissingValueException.class, () -> router.get("/lookup/no-sources").value());

              assertEquals(t.getMessage(), "Missing value: 'myParam'");

              {
                MockContext context =
                    new ParamSourceCheckerContext(
                        sources -> assertArrayEquals(sources, new ParamSource[] {PATH}));
                router.get("/lookup/source-num-1", context).value();
              }

              {
                MockContext context =
                    new ParamSourceCheckerContext(
                        sources -> assertArrayEquals(sources, new ParamSource[] {PATH, HEADER}));
                router.get("/lookup/source-num-2", context).value();
              }

              {
                MockContext context =
                    new ParamSourceCheckerContext(
                        sources ->
                            assertArrayEquals(sources, new ParamSource[] {PATH, HEADER, COOKIE}));
                router.get("/lookup/source-num-3", context).value();
              }

              {
                MockContext context =
                    new ParamSourceCheckerContext(
                        sources ->
                            assertArrayEquals(
                                sources, new ParamSource[] {PATH, HEADER, COOKIE, FLASH}));
                router.get("/lookup/source-num-4", context).value();
              }

              {
                MockContext context =
                    new ParamSourceCheckerContext(
                        sources ->
                            assertArrayEquals(
                                sources, new ParamSource[] {PATH, HEADER, COOKIE, FLASH, SESSION}));
                router.get("/lookup/source-num-5", context).value();
              }

              {
                MockContext context =
                    new ParamSourceCheckerContext(
                        sources ->
                            assertArrayEquals(
                                sources,
                                new ParamSource[] {PATH, HEADER, COOKIE, FLASH, SESSION, QUERY}));
                router.get("/lookup/source-num-6", context).value();
              }

              {
                MockContext context =
                    new ParamSourceCheckerContext(
                        sources ->
                            assertArrayEquals(
                                sources,
                                new ParamSource[] {PATH, HEADER, COOKIE, FLASH, SESSION, QUERY}));
                router.get("/lookup/source-num-6plus", context).value();
              }

              {
                MockContext context = new MockContext();
                context.setQueryString("?myParam=69");
                assertEquals("69", router.get("/lookup/query-path/42", context).value());
              }

              {
                MockContext context = new MockContext();
                context.setQueryString("?myParam=69");
                assertEquals("42", router.get("/lookup/path-query/42", context).value());
              }

              assertEquals("null", router.get("/lookup/missing").value());
            });
  }

  @Test
  public void voidRoutes() throws Exception {
    new ProcessorRunner(new VoidRoute())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              router.delete(
                  "/void",
                  rsp -> {
                    assertEquals(StatusCode.NO_CONTENT, rsp.getStatusCode());
                  });
            });
  }

  @Test
  public void getPostRoutes() throws Exception {
    new ProcessorRunner(new GetPostRoute())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              router.get(
                  "/",
                  rsp -> {
                    assertEquals("Got it!", rsp.value());
                  });
              router.post(
                  "/",
                  rsp -> {
                    assertEquals("Got it!", rsp.value());
                  });
            });
  }

  @Test
  public void noTopLevel() throws Exception {
    new ProcessorRunner(new NoPathRoute())
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              router.get(
                  "/",
                  rsp -> {
                    assertEquals("root", rsp.value());
                  });
              router.get(
                  "/subpath",
                  rsp -> {
                    assertEquals("subpath", rsp.value());
                  });
            });
  }

  @Test
  public void routeAttributes() throws Exception {
    new ProcessorRunner(new RouteAttributes())
        .withRouter(
            app -> {
              Route route = app.getRoutes().get(0);
              assertEquals(12, route.getAttributes().size(), route.getAttributes().toString());
              assertEquals("string", route.getAttribute("someAnnotation"));
              assertEquals(Integer.valueOf(5), route.getAttribute("someAnnotation.i"));
              assertEquals(Long.valueOf(200), route.getAttribute("someAnnotation.l"));
              assertEquals(Float.valueOf(8), route.getAttribute("someAnnotation.f"));
              assertEquals(Double.valueOf(99), route.getAttribute("someAnnotation.d"));
              assertEquals(Integer.class, route.getAttribute("someAnnotation.type"));
              assertEquals(true, route.getAttribute("someAnnotation.bool"));
              assertEquals(Character.valueOf('X'), route.getAttribute("someAnnotation.c"));
              assertEquals(Short.MIN_VALUE, (short) route.getAttribute("someAnnotation.s"));
              assertEquals(Arrays.asList("a", "b"), route.getAttribute("someAnnotation.values"));
              assertEquals("User", route.getAttribute("roleAnnotation"));
              Map<String, Object> link = route.getAttribute("someAnnotation.annotation");
              assertNotNull(link);
              assertEquals("link", link.get("LinkAnnotation"));
              List<Map> array = (List) link.get("LinkAnnotation.array");
              assertEquals("1", array.get(0).get("ArrayAnnotation"));
              assertEquals("2", array.get(1).get("ArrayAnnotation"));
              assertFalse(route.getAttributes().containsKey("sourceAnnotation"));
              assertFalse(route.getAttributes().containsKey("classAnnotation"));
            });
  }

  @Test
  public void routeDispatch() throws Exception {
    new ProcessorRunner(new RouteDispatch())
        .withRouter(
            app -> {
              assertEquals("worker", app.getRoutes().get(0).getExecutorKey());
              assertEquals("single", app.getRoutes().get(1).getExecutorKey());
            });
  }
}
