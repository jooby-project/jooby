package org.jooby.json;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.Renderer.Context;
import org.jooby.test.MockUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GsonRenderer.class, Gson.class })
public class GsonRendererTest {

  @Test
  //@Ignore
  public void render() throws Exception {
    Object value = new GsonRendererTest();
    new MockUnit(Gson.class, Renderer.Context.class)
        .expect(unit -> {
          Context ctx = unit.get(Renderer.Context.class);
          expect(ctx.accepts(MediaType.json)).andReturn(true);
          expect(ctx.type(MediaType.json)).andReturn(ctx);
          ctx.send("{}");
        })
        .expect(unit -> {
          Gson gson = unit.get(Gson.class);
          JsonElement el = gson.toJsonTree(value);
          expect(gson.toJsonTree(value).getAsString()).andReturn("{}");
        });
        /*.run(unit -> {
          new GsonRenderer(MediaType.json, unit.get(Gson.class))
              .render(value, unit.get(Renderer.Context.class));
        }, unit -> {
        });*/
  }

  @Test
  @Ignore
  public void renderSkip() throws Exception {
    Object value = new GsonRendererTest();
    new MockUnit(Gson.class, Renderer.Context.class)
        .expect(unit -> {
          Context ctx = unit.get(Renderer.Context.class);
          expect(ctx.accepts(MediaType.json)).andReturn(false);
        })
        .run(unit -> {
          new GsonRenderer(MediaType.json, unit.get(Gson.class))
              .render(value, unit.get(Renderer.Context.class));
        }, unit -> {
        });
  }

  @Test
  @Ignore
  public void toStr() throws Exception {
    new MockUnit(Gson.class)
        .run(unit -> {
          assertEquals("json", new GsonRenderer(MediaType.json, unit.get(Gson.class)).toString());
        });
  }
}
