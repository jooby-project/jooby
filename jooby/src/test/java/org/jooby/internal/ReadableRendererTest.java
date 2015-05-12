package org.jooby.internal;

import static org.easymock.EasyMock.expect;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.concurrent.CountDownLatch;

import org.jooby.MediaType;
import org.jooby.MockUnit;
import org.jooby.MockUnit.Block;
import org.jooby.Renderer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BuiltinRenderer.class, CharStreams.class, Closeables.class })
public class ReadableRendererTest {

  private Block defaultType = unit -> {
    Renderer.Context ctx = unit.get(Renderer.Context.class);
    expect(ctx.type(MediaType.html)).andReturn(ctx);
  };

  @Test
  public void render() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Reader reader = new StringReader("text") {
      @Override
      public void close() {
        super.close();
        latch.countDown();
      }
    };
    Writer writer = new StringWriter();
    new MockUnit(Renderer.Context.class)
        .expect(defaultType)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.text(unit.capture(Renderer.Text.class));

          unit.mockStatic(CharStreams.class);
          expect(CharStreams.copy(reader, writer)).andReturn(0L);
        })
        .run(unit -> {
          BuiltinRenderer.Readable
              .render(reader, unit.get(Renderer.Context.class));
        }, unit -> {
          unit.captured(Renderer.Text.class).iterator().next()
              .write(writer);
        });
    latch.await();
  }

  @Test
  public void renderNoClose() throws Exception {
    CharBuffer buffer = CharBuffer.wrap(new char[0]);
    Writer writer = new StringWriter();
    new MockUnit(Renderer.Context.class)
        .expect(defaultType)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.text(unit.capture(Renderer.Text.class));

          unit.mockStatic(CharStreams.class);
          expect(CharStreams.copy(buffer, writer)).andReturn(0L);
        })
        .run(unit -> {
          BuiltinRenderer.Readable
              .render(buffer, unit.get(Renderer.Context.class));
        }, unit -> {
          unit.captured(Renderer.Text.class).iterator().next()
              .write(writer);
        });
  }

  @Test
  public void renderIgnored() throws Exception {
    new MockUnit(Renderer.Context.class)
        .run(unit -> {
          BuiltinRenderer.Readable
              .render(new Object(), unit.get(Renderer.Context.class));
        });
  }

  @Test(expected = IOException.class)
  public void renderWithFailure() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Reader reader = new StringReader("text") {
      @Override
      public void close() {
        super.close();
        latch.countDown();
      }
    };
    Writer writer = new StringWriter();
    new MockUnit(Renderer.Context.class)
        .expect(defaultType)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.text(unit.capture(Renderer.Text.class));

          unit.mockStatic(CharStreams.class);
          expect(CharStreams.copy(reader, writer)).andThrow(new IOException("intentional err"));
        })
        .run(unit -> {
          BuiltinRenderer.Readable
              .render(reader, unit.get(Renderer.Context.class));
        }, unit -> {
          unit.captured(Renderer.Text.class).iterator().next()
              .write(writer);
        });
    latch.await();
  }

}
