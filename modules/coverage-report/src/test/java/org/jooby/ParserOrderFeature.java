package org.jooby;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.jooby.test.ServerFeature;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.Set;

public class ParserOrderFeature extends ServerFeature {

  Key<Set<Parser>> KEY = Key.get(new TypeLiteral<Set<Parser>>() {
  });

  {

    parser(new Parser() {

      @Override
      public Object parse(final TypeLiteral<?> type, final Context ctx) throws Throwable {
        assertEquals(
            "[Basic, Collection, Optional, Enum, byte[], p1, p2, p3, Date, LocalDate, ZonedDateTime, Locale, valueOf(String), fromString(String), forName(String), init(String), bean]",
            ctx.toString());
        return ctx.next();
      }

      @Override
      public String toString() {
        return "p1";
      }

    });

    use((env, conf, binder) -> {
      Multibinder.newSetBinder(binder, Parser.class).addBinding().toInstance(new Parser() {

        @Override
        public Object parse(final TypeLiteral<?> type, final Context ctx) throws Throwable {
          return ctx.next();
        }

        @Override
        public String toString() {
          return "p2";
        }

      });
    });

    parser(new Parser() {

      @Override
      public Object parse(final TypeLiteral<?> type, final Context ctx) throws Throwable {
        return ctx.next();
      }

      @Override
      public String toString() {
        return "p3";
      }

    });

    get("/parser/order", req -> req.require(KEY));

  }

  @Test
  public void order() throws Exception {
    request()
        .get("/parser/order")
        .expect(
            "[Basic, Collection, Optional, Enum, byte[], p1, p2, p3, Date, LocalDate, ZonedDateTime, Locale, valueOf(String), fromString(String), forName(String), init(String), bean]");
  }
}
