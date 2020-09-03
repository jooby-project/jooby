package tests;

import io.jooby.Context;
import io.jooby.MockContext;
import io.jooby.MockRouter;
import io.jooby.ParamSource;
import io.jooby.Route;
import io.jooby.StatusCode;
import io.jooby.apt.MvcModuleCompilerRunner;
import org.junit.jupiter.api.Test;
import source.ArrayRoute;
import source.Bean;
import source.GetPostRoute;
import source.JavaBeanParam;
import source.MinRoute;
import source.NoPathRoute;
import source.ParamSourceCheckerContext;
import source.PrimitiveReturnType;
import source.RouteAttributes;
import source.RouteDispatch;
import source.RouteWithMimeTypes;
import source.RouteWithParamLookup;
import source.Routes;
import source.VoidRoute;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.jooby.ParamSource.COOKIE;
import static io.jooby.ParamSource.FLASH;
import static io.jooby.ParamSource.FORM;
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

public class ModuleCompilerTest {
  @Test
  public void minRoute() throws Exception {
    new MvcModuleCompilerRunner(new MinRoute())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          assertEquals("/mypath", router.get("/mypath").value());
        });
  }

  @Test
  public void arrayRoute() throws Exception {
    new MvcModuleCompilerRunner(new ArrayRoute())
        .module(app -> {
          MockRouter router = new MockRouter(app);

          Object value = router.get("/mypath").value();
          assertTrue(value instanceof String[]);
          assertArrayEquals(new String[] { "/mypath1", "/mypath2" }, (String[]) value);

          value = router.get("/mybeans").value();
          assertTrue(value instanceof Bean[]);
          assertArrayEquals(new Bean[] { new Bean(1), new Bean(2) }, (Bean[]) value);

          value = router.get("/mymultibeans").value();
          assertTrue(value instanceof Bean[][]);
          assertArrayEquals(new Bean[][] { { new Bean(1) }, { new Bean(2) } }, (Bean[][]) value);
        });
  }

  @Test
  public void routes() throws Exception {
    new MvcModuleCompilerRunner(new Routes())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          assertEquals("/path", router.get("/path").value());
          assertEquals(Arrays.asList("/path/subpath"), router.get("/path/subpath").value());
          assertTrue(router.get("/path/object").value() instanceof Context);
          assertTrue(router.post("/path/post").value() instanceof JavaBeanParam);

          assertEquals("/path/pathAttributeWork", router.get("/path/pathAttributeWork").value());
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
    new MvcModuleCompilerRunner(new RouteWithMimeTypes())
        .module(app -> {
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
    new MvcModuleCompilerRunner(new RouteWithParamLookup())
        .module(app -> {
          MockRouter router = new MockRouter(app);

          Throwable t = assertThrows(IllegalArgumentException.class, () -> router.get("/lookup/no-sources").value());

          assertEquals(t.getMessage(), "No parameter sources were specified.");

          {
            MockContext context = new ParamSourceCheckerContext(sources -> assertArrayEquals(sources, new ParamSource[] {
                PATH
            }));
            router.get("/lookup/source-num-1", context).value();
          }

          {
            MockContext context = new ParamSourceCheckerContext(sources -> assertArrayEquals(sources, new ParamSource[] {
                PATH, HEADER
            }));
            router.get("/lookup/source-num-2", context).value();
          }

          {
            MockContext context = new ParamSourceCheckerContext(sources -> assertArrayEquals(sources, new ParamSource[] {
                PATH, HEADER, COOKIE
            }));
            router.get("/lookup/source-num-3", context).value();
          }

          {
            MockContext context = new ParamSourceCheckerContext(sources -> assertArrayEquals(sources, new ParamSource[] {
                PATH, HEADER, COOKIE, FLASH
            }));
            router.get("/lookup/source-num-4", context).value();
          }

          {
            MockContext context = new ParamSourceCheckerContext(sources -> assertArrayEquals(sources, new ParamSource[] {
                PATH, HEADER, COOKIE, FLASH, SESSION
            }));
            router.get("/lookup/source-num-5", context).value();
          }

          {
            MockContext context = new ParamSourceCheckerContext(sources -> assertArrayEquals(sources, new ParamSource[] {
                PATH, HEADER, COOKIE, FLASH, SESSION, QUERY
            }));
            router.get("/lookup/source-num-6", context).value();
          }

          {
            MockContext context = new ParamSourceCheckerContext(sources -> assertArrayEquals(sources, new ParamSource[] {
                PATH, HEADER, COOKIE, FLASH, SESSION, QUERY, FORM
            }));
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
    new MvcModuleCompilerRunner(new VoidRoute())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          router.delete("/void", rsp -> {
            assertEquals(StatusCode.NO_CONTENT, rsp.getStatusCode());
          });
        });
  }

  @Test
  public void getPostRoutes() throws Exception {
    new MvcModuleCompilerRunner(new GetPostRoute())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          router.get("/", rsp -> {
            assertEquals("Got it!", rsp.value());
          });
          router.post("/", rsp -> {
            assertEquals("Got it!", rsp.value());
          });
        });
  }

  @Test
  public void noTopLevel() throws Exception {
    new MvcModuleCompilerRunner(new NoPathRoute())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          router.get("/", rsp -> {
            assertEquals("root", rsp.value());
          });
          router.get("/subpath", rsp -> {
            assertEquals("subpath", rsp.value());
          });
        })
    ;
  }

  @Test
  public void setPrimitiveReturnType() throws Exception {
    new MvcModuleCompilerRunner(new PrimitiveReturnType())
        .module(app -> {
          Route route = app.getRoutes().get(0);
          assertEquals(int.class, route.getReturnType());
        })
    ;
  }

  @Test
  public void routeAttributes() throws Exception {
    new MvcModuleCompilerRunner(new RouteAttributes())
        .module(app -> {
          Route route = app.getRoutes().get(0);
          assertEquals(13, route.getAttributes().size(), route.getAttributes().toString());
          assertEquals("string", route.attribute("someAnnotation"));
          assertEquals(Integer.valueOf(5), route.attribute("someAnnotation.i"));
          assertEquals(Long.valueOf(200), route.attribute("someAnnotation.l"));
          assertEquals(Float.valueOf(8), route.attribute("someAnnotation.f"));
          assertEquals(Double.valueOf(99), route.attribute("someAnnotation.d"));
          assertEquals(Integer.class, route.attribute("someAnnotation.type"));
          assertEquals(true, route.attribute("someAnnotation.bool"));
          assertEquals(Character.valueOf('X'), route.attribute("someAnnotation.c"));
          assertEquals(Short.MIN_VALUE, (short) route.attribute("someAnnotation.s"));
          assertEquals(Arrays.asList("a", "b"), route.attribute("someAnnotation.values"));
          assertEquals("User", route.attribute("roleAnnotation"));
          assertEquals("one", route.attribute("roleAnnotation.level"));
          Map<String, Object> link = route.attribute("someAnnotation.annotation");
          assertNotNull(link);
          assertEquals("link", link.get("LinkAnnotation"));
          List<Map> array = (List) link.get("LinkAnnotation.array");
          assertEquals("1", array.get(0).get("ArrayAnnotation"));
          assertEquals("2", array.get(1).get("ArrayAnnotation"));
          assertFalse(route.getAttributes().containsKey("sourceAnnotation"));
          assertFalse(route.getAttributes().containsKey("classAnnotation"));
        })
    ;
  }

  @Test
  public void routeDispatch() throws Exception {
    new MvcModuleCompilerRunner(new RouteDispatch())
        .module(app -> {
          assertEquals("worker", app.getRoutes().get(0).getExecutorKey());
          assertEquals("single", app.getRoutes().get(1).getExecutorKey());
        })
    ;
  }
}
