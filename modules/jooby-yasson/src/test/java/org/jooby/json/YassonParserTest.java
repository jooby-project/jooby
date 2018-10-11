package org.jooby.json;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import javax.json.bind.Jsonb;

import org.jooby.MediaType;
import org.jooby.Parser;
import org.jooby.Parser.Context;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.google.inject.TypeLiteral;

@RunWith(PowerMockRunner.class)
@PrepareForTest({YassonParser.class, Jsonb.class })
public class YassonParserTest {

  @SuppressWarnings("unchecked")
  @Test
  public void parseBody() throws Exception {
    TypeLiteral<YassonParserTest> type = TypeLiteral.get(YassonParserTest.class);
    Object value = new Object();
    new MockUnit(Jsonb.class, Parser.Context.class, Parser.BodyReference.class)
        .expect(unit -> {
          Context ctx = unit.get(Parser.Context.class);
          expect(ctx.type()).andReturn(MediaType.json);

          Parser.Builder builder = unit.mock(Parser.Builder.class);

          expect(ctx.ifbody(unit.capture(Parser.Callback.class))).andReturn(builder);
          expect(builder.ifparam(unit.capture(Parser.Callback.class))).andReturn(builder);
        })
        .expect(unit -> {
          Parser.BodyReference ref = unit.get(Parser.BodyReference.class);
          expect(ref.text()).andReturn("{}");
        })
        .expect(unit -> {
          Jsonb jsonb = unit.get(Jsonb.class);
          expect(jsonb.fromJson("{}", type.getType())).andReturn(value);
        })
        .run(unit -> {
          new YassonParser(MediaType.json, unit.get(Jsonb.class))
              .parse(type, unit.get(Parser.Context.class));
        }, unit -> {
          unit.captured(Parser.Callback.class).iterator().next()
              .invoke(unit.get(Parser.BodyReference.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void parseParam() throws Exception {
    TypeLiteral<YassonParserTest> type = TypeLiteral.get(YassonParserTest.class);
    Object value = new Object();
    new MockUnit(Jsonb.class, Parser.Context.class, Parser.ParamReference.class)
        .expect(unit -> {
          Context ctx = unit.get(Parser.Context.class);
          expect(ctx.type()).andReturn(MediaType.json);

          Parser.Builder builder = unit.mock(Parser.Builder.class);

          expect(ctx.ifbody(unit.capture(Parser.Callback.class))).andReturn(builder);
          expect(builder.ifparam(unit.capture(Parser.Callback.class))).andReturn(builder);
        })
        .expect(unit -> {
          Parser.ParamReference<String> ref = unit.get(Parser.ParamReference.class);
          expect(ref.first()).andReturn("{}");
        })
        .expect(unit -> {
            Jsonb jsonb = unit.get(Jsonb.class);
          expect(jsonb.fromJson("{}", type.getType())).andReturn(value);
        })
        .run(unit -> {
          new YassonParser(MediaType.json, unit.get(Jsonb.class))
              .parse(type, unit.get(Parser.Context.class));
        }, unit -> {
          unit.captured(Parser.Callback.class).get(1)
              .invoke(unit.get(Parser.ParamReference.class));
        });
  }

  @Test
  public void next() throws Exception {
    TypeLiteral<YassonParserTest> type = TypeLiteral.get(YassonParserTest.class);
    new MockUnit(Jsonb.class, Parser.Context.class, Parser.BodyReference.class)
        .expect(unit -> {
          Context ctx = unit.get(Parser.Context.class);
          expect(ctx.type()).andReturn(MediaType.html);
          expect(ctx.next()).andReturn(null);
        })
        .run(unit -> {
          new YassonParser(MediaType.json, unit.get(Jsonb.class))
              .parse(type, unit.get(Parser.Context.class));
        });
  }

  @Test
  public void nextAny() throws Exception {
    TypeLiteral<YassonParserTest> type = TypeLiteral.get(YassonParserTest.class);
    new MockUnit(Jsonb.class, Parser.Context.class, Parser.BodyReference.class)
        .expect(unit -> {
          Context ctx = unit.get(Parser.Context.class);
          expect(ctx.type()).andReturn(MediaType.all);
          expect(ctx.next()).andReturn(null);
        })
        .run(unit -> {
          new YassonParser(MediaType.json, unit.get(Jsonb.class))
              .parse(type, unit.get(Parser.Context.class));
        });
  }

  @Test
  public void toStr() throws Exception {
    new MockUnit(Jsonb.class)
        .run(unit -> {
          assertEquals("yasson", new YassonParser(MediaType.json, unit.get(Jsonb.class)).toString());
        });
  }

}
