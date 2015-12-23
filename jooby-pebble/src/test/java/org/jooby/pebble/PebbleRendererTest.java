package org.jooby.pebble;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.View;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PebbleRenderer.class, HashMap.class })
public class PebbleRendererTest {

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void render() throws Exception {
    new MockUnit(PebbleEngine.class, View.class, Renderer.Context.class)
        .expect(unit -> {
          Map vmodel = unit.mock(Map.class);
          Map<String, Object> locals = unit.mock(Map.class);

          Map model = unit.constructor(HashMap.class).build();
          model.putAll(locals);
          expect(model.putIfAbsent("_vname", "vname")).andReturn(null);
          model.putAll(vmodel);

          View view = unit.get(View.class);
          expect(view.name()).andReturn("vname");
          expect(view.model()).andReturn(vmodel);

          StringWriter writer = unit.constructor(StringWriter.class).build();

          Renderer.Context ctx = unit.get(Renderer.Context.class);
          expect(ctx.locals()).andReturn(locals);
          expect(ctx.type(MediaType.html)).andReturn(ctx);
          ctx.send(writer.toString());

          PebbleTemplate template = unit.mock(PebbleTemplate.class);
          template.evaluate(writer, model);

          PebbleEngine pebble = unit.get(PebbleEngine.class);
          expect(pebble.getTemplate("vname")).andReturn(template);
        })
        .run(unit -> {
          PebbleRenderer engine = new PebbleRenderer(unit.get(PebbleEngine.class));
          engine.render(unit.get(View.class), unit.get(Renderer.Context.class));
          assertEquals("pebble", engine.toString());
        });
  }
}
