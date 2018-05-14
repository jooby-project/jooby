package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class RendererOrderFeature extends ServerFeature {

  Key<Set<Renderer>> KEY = Key.get(new TypeLiteral<Set<Renderer>>() {
  });

  {

    renderer(new Renderer() {

      @Override
      public void render(final Object object, final Context ctx) throws Exception {
        assertEquals("asset, bytes, byteBuffer, file, charBuffer, stream, reader, fileChannel, r1, r2, r3, defaultErr, text",
            ctx.toString());
      }

      @Override
      public String name() {
        return "r1";
      }
    });

    use((env, conf, binder) -> {
      Multibinder.newSetBinder(binder, Renderer.class).addBinding().toInstance(new Renderer() {

        @Override
        public void render(final Object object, final Context ctx) throws Exception {
        }

        @Override
        public String name() {
          return "r2";
        }

      });
    });

    renderer(new Renderer() {

      @Override
      public void render(final Object object, final Context ctx) throws Exception {
      }

      @Override
      public String name() {
        return "r3";
      }
    });

    get("/renderer/order", req -> req.require(KEY).stream().map(Renderer::name)
        .collect(Collectors.joining(", ")));

  }

  @Test
  public void order() throws Exception {
    request()
        .get("/renderer/order")
        .expect("asset, bytes, byteBuffer, file, charBuffer, stream, reader, fileChannel, r1, r2, r3, defaultErr, text");
  }
}
