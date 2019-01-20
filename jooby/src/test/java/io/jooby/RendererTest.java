package io.jooby;

import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RendererTest {

  static class AtomicRefRenderer implements Renderer {

    AtomicReference<Object> result = new AtomicReference<>();

    @Override public boolean render(@Nonnull Context ctx, @Nonnull Object result) throws Exception {
      this.result.set(result);
      return true;
    }
  }

  @Test
  public void shouldRenderWhenAcceptHeaderMatches() throws Exception {
    AtomicRefRenderer renderer = new AtomicRefRenderer();
    MockContext ctx = new MockContext()
        .setHeaders(Value.headers().put("Accept", "application/json"));
    Object result = new Object();
    renderer.accept(MediaType.json).render(ctx, result);
    assertEquals(result, renderer.result.get());
  }

  @Test
  public void shouldNotRenderWhenAcceptHeaderMatches() throws Exception {
    AtomicRefRenderer renderer = new AtomicRefRenderer();
    MockContext ctx = new MockContext()
        .setHeaders(Value.headers().put("Accept", "text/html"));
    Object result = new Object();
    renderer.accept(MediaType.json).render(ctx, result);
    assertEquals(null, renderer.result.get());
  }
}
