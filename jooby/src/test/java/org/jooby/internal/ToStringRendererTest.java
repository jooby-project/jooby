package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.jooby.MediaType;
import org.jooby.MockUnit;
import org.jooby.MockUnit.Block;
import org.jooby.Renderer;
import org.jooby.Results;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BuiltinRenderer.class, CharStreams.class, Closeables.class })
public class ToStringRendererTest {

  private Block defaultType = unit -> {
    Renderer.Context ctx = unit.get(Renderer.Context.class);
    expect(ctx.type(MediaType.html)).andReturn(ctx);
  };

  @Test
  public void render() throws Exception {
    StringWriter writer = new StringWriter();
    Object value = new Object() {
      @Override
      public String toString() {
        return "toString";
      }
    };
    new MockUnit(Renderer.Context.class, Object.class)
        .expect(defaultType)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.text(unit.capture(Renderer.Text.class));
        })
        .run(unit -> {
          BuiltinRenderer.ToString
              .render(value, unit.get(Renderer.Context.class));
        }, unit -> {
          unit.captured(Renderer.Text.class).iterator().next()
              .write(writer);
        });

    assertEquals(value.toString(), writer.toString());
  }

  @Test
  public void renderIgnored() throws Exception {
    new MockUnit(Renderer.Context.class)
        .run(unit -> {
          BuiltinRenderer.ToString
              .render(Results.html("v"), unit.get(Renderer.Context.class));
        });
  }

}
