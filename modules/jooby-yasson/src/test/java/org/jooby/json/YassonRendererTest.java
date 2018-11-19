package org.jooby.json;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import javax.json.bind.Jsonb;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.Renderer.Context;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({YassonRenderer.class, Jsonb.class })
public class YassonRendererTest {

  @Test
  public void render() throws Exception {
    Object value = new YassonRendererTest();
    new MockUnit(Jsonb.class, Renderer.Context.class)
        .expect(unit -> {
          Context ctx = unit.get(Renderer.Context.class);
          expect(ctx.accepts(MediaType.json)).andReturn(true);
          expect(ctx.type(MediaType.json)).andReturn(ctx);
          ctx.send("{}");
        })
        .expect(unit -> {
            Jsonb jsonB = unit.get(Jsonb.class);
          expect(jsonB.toJson(value)).andReturn("{}");
        })
        .run(unit -> {
          new YassonRenderer(MediaType.json, unit.get(Jsonb.class))
              .render(value, unit.get(Renderer.Context.class));
        }, unit -> {
        });
  }

  @Test
  public void renderSkip() throws Exception {
    Object value = new YassonRendererTest();
    new MockUnit(Jsonb.class, Renderer.Context.class)
        .expect(unit -> {
          Context ctx = unit.get(Renderer.Context.class);
          expect(ctx.accepts(MediaType.json)).andReturn(false);
        })
        .run(unit -> {
          new YassonRenderer(MediaType.json, unit.get(Jsonb.class))
              .render(value, unit.get(Renderer.Context.class));
        }, unit -> {
        });
  }

  @Test
  public void toStr() throws Exception {
    new MockUnit(Jsonb.class)
        .run(unit -> {
          assertEquals("json", new YassonRenderer(MediaType.json, unit.get(Jsonb.class)).toString());
        });
  }
}
