package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jooby.MediaType;
import org.jooby.MockUnit;
import org.jooby.MockUnit.Block;
import org.jooby.Renderer;
import org.junit.Test;

public class BytesRendererTest {

  private Block defaultType = unit -> {
    Renderer.Context ctx = unit.get(Renderer.Context.class);
    expect(ctx.type(MediaType.octetstream)).andReturn(ctx);
  };

  @Test
  public void render() throws Exception {
    byte[] bytes = "bytes".getBytes();
    new MockUnit(Renderer.Context.class)
        .expect(defaultType)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.send(bytes);
        })
        .run(unit -> {
          BuiltinRenderer.Bytes
              .render(bytes, unit.get(Renderer.Context.class));
        });
  }

  @Test
  public void renderIgnoredAnyOtherArray() throws Exception {
    int[] bytes = new int[0];
    new MockUnit(Renderer.Context.class)
        .run(unit -> {
          BuiltinRenderer.Bytes
              .render(bytes, unit.get(Renderer.Context.class));
        });
  }

  @Test
  public void renderIgnore() throws Exception {
    new MockUnit(Renderer.Context.class)
        .run(unit -> {
          BuiltinRenderer.Bytes
              .render(new Object(), unit.get(Renderer.Context.class));
        });
  }

  @Test(expected = IOException.class)
  public void renderWithFailure() throws Exception {
    byte[] bytes = "bytes".getBytes();
    new MockUnit(Renderer.Context.class, InputStream.class, OutputStream.class)
        .expect(defaultType)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.send(bytes);
          expectLastCall().andThrow(new IOException("intentational err"));

        })
        .run(unit -> {
          BuiltinRenderer.Bytes
              .render(bytes, unit.get(Renderer.Context.class));
        });
  }

}
