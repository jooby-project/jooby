package org.jooby.thymeleaf;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jooby.Env;
import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.View;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.google.common.collect.ImmutableMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ThlEngine.class, TemplateEngine.class, Thlxss.class, Context.class })
public class ThlEngineTest {

  @Test
  public void newInstance() throws Exception {
    new MockUnit(TemplateEngine.class, Env.class)
        .run(unit -> {
          new ThlEngine(unit.get(TemplateEngine.class), unit.get(Env.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void renderer() throws Exception {
    new MockUnit(Env.class, View.class, Renderer.Context.class)
        .expect(unit -> {
          TemplateEngine engine = unit.powerMock(TemplateEngine.class);
          unit.registerMock(TemplateEngine.class, engine);
        })
        .expect(viewName("index"))
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          expect(ctx.locals()).andReturn(new HashMap<>(ImmutableMap.of("_vname", "index")));
        })
        .expect(unit -> {
          Thlxss xss = unit.constructor(Thlxss.class)
              .args(Env.class)
              .build(unit.get(Env.class));

          Map model = unit.mock(Map.class);
          expect(model.putIfAbsent("_vname", "index")).andReturn(null);
          expect(model.putIfAbsent("xss", xss)).andReturn(null);

          View view = unit.get(View.class);
          expect(view.model()).andReturn(model);

          Renderer.Context ctx = unit.get(Renderer.Context.class);
          expect(ctx.locale()).andReturn(Locale.CANADA);

          Context context = unit.constructor(Context.class)
              .args(Locale.class, Map.class)
              .build(Locale.CANADA, model);

          TemplateEngine engine = unit.get(TemplateEngine.class);
          expect(engine.process("index", context)).andReturn("...");
          expect(ctx.type(MediaType.html)).andReturn(ctx);
          ctx.send("...");
        })
        .run(unit -> {
          ThlEngine engine = new ThlEngine(unit.get(TemplateEngine.class), unit.get(Env.class));
          engine.render(unit.get(View.class), unit.get(Renderer.Context.class));
          assertEquals("thymeleaf", engine.name());
        });
  }

  private Block viewName(final String vname) {
    return unit -> {
      View view = unit.get(View.class);
      expect(view.name()).andReturn(vname);
    };
  }
}
