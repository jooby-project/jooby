package org.jooby.internal;

import static org.easymock.EasyMock.expect;

import org.jooby.MediaType;
import org.jooby.MockUnit;
import org.jooby.MockUnit.Block;
import org.jooby.Renderer;
import org.jooby.Results;
import org.junit.Test;

public class ToStringRendererTest {

  private Block defaultType = unit -> {
    Renderer.Context ctx = unit.get(Renderer.Context.class);
    expect(ctx.type(MediaType.html)).andReturn(ctx);
  };

  @Test
  public void render() throws Exception {
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
          ctx.send("toString");
        })
        .run(unit -> {
          BuiltinRenderer.ToString
              .render(value, unit.get(Renderer.Context.class));
        });

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
