package tests;

import io.jooby.MockContext;
import io.jooby.MockRouter;
import io.jooby.Session;
import io.jooby.apt.MvcModuleCompilerRunner;
import io.jooby.internal.MockContextHelper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class Issue1387 {

  @Test
  public void shouldInjectContextParam() throws Exception {
    new MvcModuleCompilerRunner(new source.Issue1387()).module(app -> {
      MockRouter router = new MockRouter(app);

      MockContext ctx = MockContextHelper.mockContext();
      ctx.getAttributes().put("userId", "123");
      assertEquals("123", router.get("/1387", ctx).value());

      ctx = MockContextHelper.mockContext();
      source.Issue1387.Data1387 data = new source.Issue1387.Data1387();
      ctx.getAttributes().put("data", data);
      assertEquals(data, router.get("/1387/complex", ctx).value());

      ctx = MockContextHelper.mockContext();
      ctx.getAttributes().put("userId", 123);
      assertEquals(123, router.get("/1387/primitive", ctx).value());
    }).module(app -> {
      MockRouter router = new MockRouter(app);

      // Global attributes
      MockContext ctx = MockContextHelper.mockContext();
      Map<String, Object> attributes = ctx.getAttributes();
      attributes.put("k", "v");
      assertEquals(attributes, router.get("/1387/attributes", ctx).value());
      // Attribute set has more precedence
      Map<String, Object> map = new HashMap<>();
      map.put("k", "foo");
      map.put("b", "dd");
      attributes.put("attributes", map);
      assertEquals(map, router.get("/1387/attributes", ctx).value());
    });
  }

  @Test
  public void shouldInjectSessionParam() throws Exception {
    new MvcModuleCompilerRunner(new source.Issue1387()).module(app -> {
      MockRouter router = new MockRouter(app);

      MockContext ctx = MockContextHelper.mockContext();
      assertEquals(null, router.get("/1387/session", ctx).value());

      Session session = ctx.session();
      session.put("userId", "abc");
      assertEquals("abc", router.get("/1387/session", ctx).value());

      ctx = MockContextHelper.mockContext();
      session = ctx.session();
      session.put("userId", 123);

      assertEquals(123, router.get("/1387/session/int", ctx).value());
    });
  }

}
