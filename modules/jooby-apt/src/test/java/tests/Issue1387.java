package tests;

import io.jooby.Context;
import io.jooby.MockContext;
import io.jooby.Session;
import io.jooby.apt.MvcHandlerCompilerRunner;
import io.jooby.internal.MockContextHelper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class Issue1387 {

  @Test
  public void shouldInjectContextParam() throws Exception {
    new MvcHandlerCompilerRunner(new source.Issue1387())
        .compile("/1387", handler -> {
          MockContext ctx = MockContextHelper.mockContext();
          ctx.getAttributes().put("userId", "123");
          assertEquals("123", handler.apply(ctx));
        })
        .compile("/1387/primitive", handler -> {
          MockContext ctx = MockContextHelper.mockContext();
          ctx.getAttributes().put("userId", 123);
          assertEquals(123, handler.apply(ctx));
        })
        .compile("/1387/complex", handler -> {
          MockContext ctx = MockContextHelper.mockContext();
          source.Issue1387.Data1387 data = new source.Issue1387.Data1387();
          ctx.getAttributes().put("data", data);
          assertEquals(data, handler.apply(ctx));
        })
        .compile("/1387/attributes", handler -> {
          // Global attributes
          MockContext ctx = MockContextHelper.mockContext();
          Map<String, Object> attributes = ctx.getAttributes();
          attributes.put("k", "v");
          assertEquals(attributes, handler.apply(ctx));
          // Attribute set has more precedence
          Map<String, Object> map = new HashMap<>();
          map.put("k", "foo");
          map.put("b", "dd");
          attributes.put("attributes", map);
          assertEquals(map, handler.apply(ctx));
        })
    ;
  }

  @Test
  public void shouldInjectSessionParam() throws Exception {
    new MvcHandlerCompilerRunner(new source.Issue1387())
        .compile("/1387/session", handler -> {
          MockContext ctx = MockContextHelper.mockContext();
          assertEquals(null, handler.apply(ctx));

          Session session = ctx.session();
          session.put("userId", "abc");
          assertEquals("abc", handler.apply(ctx));
        })
        .compile("/1387/session/int", handler -> {
          MockContext ctx = MockContextHelper.mockContext();
          Session session = ctx.session();
          session.put("userId", 123);

          assertEquals(123, handler.apply(ctx));
        })
    ;
  }

}
