package tests;

import io.jooby.Context;
import io.jooby.MockContext;
import io.jooby.MockRouter;
import io.jooby.Route;
import io.jooby.StatusCode;
import io.jooby.apt.MvcHandlerCompilerRunner;
import io.jooby.apt.MvcModuleCompilerRunner;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import source.GetPostRoute;
import source.JavaBeanParam;
import source.NoPathRoute;
import source.PrimitiveReturnType;
import source.RouteAttributes;
import source.RouteWithMimeTypes;
import source.Routes;
import source.VoidRoute;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModuleCompilerTest {
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
          ;
          assertEquals("/path/path", router.get("/path/path").value());
          ;
          assertEquals("/path/value", router.get("/path/value").value());
          ;

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
  public void voidRoutes() throws Exception {
    new MvcModuleCompilerRunner(new VoidRoute())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          router.get("/void", rsp -> {
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
        })
    ;
  }
}
