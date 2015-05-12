package org.jooby.internal;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.jooby.MediaType;
import org.jooby.MockUnit;
import org.jooby.MockUnit.Block;
import org.jooby.Renderer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BuiltinRenderer.class, ByteStreams.class, Closeables.class })
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
          ctx.bytes(unit.capture(Renderer.Bytes.class));

          unit.mockStatic(ByteStreams.class);
          expect(ByteStreams.copy(isA(ByteArrayInputStream.class),
              eq(unit.get(OutputStream.class)))).andReturn(0L);
        })
        .run(unit -> {
          BuiltinRenderer.ByteBuffer
              .render(bytes, unit.get(Renderer.Context.class));
        }, unit -> {
          unit.captured(Renderer.Bytes.class).iterator().next()
              .write(unit.get(OutputStream.class));
        });
  }

  @Test
  public void renderDirect() throws Exception {
    ByteBuffer bytes = ByteBuffer.allocateDirect(0);
    new MockUnit(Renderer.Context.class, OutputStream.class)
        .expect(defaultType)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.bytes(unit.capture(Renderer.Bytes.class));

          unit.mockStatic(ByteStreams.class);
          expect(ByteStreams.copy(isA(ByteByfferInputStream.class),
              eq(unit.get(OutputStream.class)))).andReturn(0L);
        })
        .run(unit -> {
          BuiltinRenderer.ByteBuffer
              .render(bytes, unit.get(Renderer.Context.class));
        }, unit -> {
          unit.captured(Renderer.Bytes.class).iterator().next()
              .write(unit.get(OutputStream.class));
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
