package org.jooby.json;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.MediaType;
import org.jooby.MockUnit;
import org.jooby.Parser;
import org.jooby.Parser.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.Gson;
import com.google.inject.TypeLiteral;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GsonParser.class, Gson.class })
public class GsonParserTest {

  @SuppressWarnings("unchecked")
  @Test
  public void parse() throws Exception {
    TypeLiteral<GsonParserTest> type = TypeLiteral.get(GsonParserTest.class);
    Object value = new Object();
    new MockUnit(Gson.class, Parser.Context.class, Parser.BodyReference.class)
        .expect(unit -> {
          Context ctx = unit.get(Parser.Context.class);
          expect(ctx.type()).andReturn(MediaType.json);

          Parser.Builder builder = unit.mock(Parser.Builder.class);

          expect(ctx.body(unit.capture(Parser.Callback.class))).andReturn(builder);
        })
        .expect(unit -> {
          Parser.BodyReference ref = unit.get(Parser.BodyReference.class);
          expect(ref.text()).andReturn("{}");
        })
        .expect(unit -> {
          Gson gson = unit.get(Gson.class);
          expect(gson.fromJson("{}", type.getType())).andReturn(value);
        })
        .run(unit -> {
          new GsonParser(MediaType.json, unit.get(Gson.class))
              .parse(type, unit.get(Parser.Context.class));
        }, unit -> {
          unit.captured(Parser.Callback.class).iterator().next()
              .invoke(unit.get(Parser.BodyReference.class));
        });
  }

  @Test
  public void next() throws Exception {
    TypeLiteral<GsonParserTest> type = TypeLiteral.get(GsonParserTest.class);
    new MockUnit(Gson.class, Parser.Context.class, Parser.BodyReference.class)
        .expect(unit -> {
          Context ctx = unit.get(Parser.Context.class);
          expect(ctx.type()).andReturn(MediaType.html);
          expect(ctx.next()).andReturn(null);
        })
        .run(unit -> {
          new GsonParser(MediaType.json, unit.get(Gson.class))
              .parse(type, unit.get(Parser.Context.class));
        });
  }

  @Test
  public void toStr() throws Exception {
    new MockUnit(Gson.class)
        .run(unit -> {
          assertEquals("gson", new GsonParser(MediaType.json, unit.get(Gson.class)).toString());
        });
  }

}
