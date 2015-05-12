package org.jooby.internal;

import static org.easymock.EasyMock.expect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jooby.MediaType;
import org.jooby.MockUnit;
import org.jooby.MockUnit.Block;
import org.jooby.Renderer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.io.ByteStreams;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BuiltinRenderer.class, ByteStreams.class})
public class InputStreamRendererTest {

  private Block defaultType = unit -> {
    Renderer.Context ctx = unit.get(Renderer.Context.class);
    expect(ctx.type(MediaType.octetstream)).andReturn(ctx);
  };

  private Block closeStream = unit -> {
    InputStream in = unit.get(InputStream.class);
    in.close();;
  };

  @Test
  public void render() throws Exception {
    new MockUnit(Renderer.Context.class, InputStream.class, OutputStream.class)
        .expect(defaultType)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.bytes(unit.capture(Renderer.Bytes.class));

          unit.mockStatic(ByteStreams.class);
          expect(ByteStreams.copy(unit.get(InputStream.class), unit.get(OutputStream.class)))
              .andReturn(0L);
        })
        .expect(closeStream)
        .run(unit -> {
          BuiltinRenderer.InputStream
              .render(unit.get(InputStream.class), unit.get(Renderer.Context.class));
        }, unit -> {
          unit.captured(Renderer.Bytes.class).iterator().next()
              .write(unit.get(OutputStream.class));
        });
  }

  @Test
  public void renderIgnored() throws Exception {
    new MockUnit(Renderer.Context.class)
        .run(unit -> {
          BuiltinRenderer.InputStream
              .render(new Object(), unit.get(Renderer.Context.class));
        });
  }

  @Test(expected = IOException.class)
  public void renderWithFailure() throws Exception {
    new MockUnit(Renderer.Context.class, InputStream.class, OutputStream.class)
        .expect(defaultType)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.bytes(unit.capture(Renderer.Bytes.class));

          unit.mockStatic(ByteStreams.class);
          expect(ByteStreams.copy(unit.get(InputStream.class), unit.get(OutputStream.class)))
              .andThrow(new IOException("intentational err"));
        })
        .expect(closeStream)
        .run(unit -> {
          BuiltinRenderer.InputStream
              .render(unit.get(InputStream.class), unit.get(Renderer.Context.class));
        }, unit -> {
          unit.captured(Renderer.Bytes.class).iterator().next()
              .write(unit.get(OutputStream.class));
        });
  }

}
