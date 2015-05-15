package org.jooby.internal;

import static org.easymock.EasyMock.expect;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.jooby.MediaType;
import org.jooby.MockUnit;
import org.jooby.MockUnit.Block;
import org.jooby.Renderer;
import org.junit.Test;

public class ByteBufferRendererTest {

  private Block defaultType = unit -> {
    Renderer.Context ctx = unit.get(Renderer.Context.class);
    expect(ctx.type(MediaType.octetstream)).andReturn(ctx);
  };

  @Test
  public void renderArray() throws Exception {
    ByteBuffer bytes = ByteBuffer.wrap("bytes".getBytes());
    new MockUnit(Renderer.Context.class, OutputStream.class)
        .expect(defaultType)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.send(bytes);
        })
        .run(unit -> {
          BuiltinRenderer.ByteBuffer
              .render(bytes, unit.get(Renderer.Context.class));
        });
  }

  @Test
  public void renderDirect() throws Exception {
    ByteBuffer bytes = ByteBuffer.allocateDirect(0);
    new MockUnit(Renderer.Context.class, OutputStream.class)
        .expect(defaultType)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.send(bytes);
        })
        .run(unit -> {
          BuiltinRenderer.ByteBuffer
              .render(bytes, unit.get(Renderer.Context.class));
        });
  }

  @Test
  public void renderIgnore() throws Exception {
    new MockUnit(Renderer.Context.class)
        .run(unit -> {
          BuiltinRenderer.ByteBuffer
              .render(new Object(), unit.get(Renderer.Context.class));
        });
  }

}
