package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jooby.Body.Writer;
import org.junit.Test;

public class ViewEngineTest {

  public static abstract class TestEngine implements View.Engine {
  }

  @Test
  public void engine() throws Exception {
    new MockUnit(Body.Writer.class)
        .run(unit -> {
          List<View> views = new ArrayList<>();
          TestEngine engine = new TestEngine() {

            @Override
            public void render(final View view, final Writer writer) throws Exception {
              views.add(view);
            }
          };

          engine.format(View.of("index", "this", new Object()), unit.get(Body.Writer.class));
          assertEquals(true, engine.canFormat(View.class));
          assertTrue(engine.name().startsWith("viewenginetest"));
          assertEquals(Arrays.asList(MediaType.html), engine.types());
          assertEquals(1, views.size());
        });
  }

}
