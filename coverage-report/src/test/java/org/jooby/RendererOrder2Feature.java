package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class RendererOrder2Feature extends ServerFeature {

  Key<Set<Renderer>> KEY = Key.get(new TypeLiteral<Set<Renderer>>() {
  });

  {

    use((env, conf, binder) -> {
      Multibinder.newSetBinder(binder, Renderer.class).addBinding().toInstance(new Renderer() {

        @Override
        public void render(final Object object, final Context ctx) throws Exception {
        }

        @Override
        public String toString() {
          return "r2";
        }

      });
    });

    renderer(new Renderer() {

      @Override
      public void render(final Object object, final Context ctx) throws Exception {
        assertEquals("[r2, r1, r3, byte[], ByteBuffer, File, CharBuffer, InputStream, Reader, FileChannel, toString()]",
            ctx.toString());
      }

      @Override
      public String toString() {
        return "r1";
      }
    });

    renderer(new Renderer() {

      @Override
      public void render(final Object object, final Context ctx) throws Exception {
      }

      @Override
      public String toString() {
        return "r3";
      }
    });

    get("/renderer/order", req -> req.require(KEY));

  }

  @Test
  public void order() throws Exception {
    request()
        .get("/renderer/order")
        .expect("[r2, r1, r3, byte[], ByteBuffer, File, CharBuffer, InputStream, Reader, FileChannel, toString()]");
  }
}
