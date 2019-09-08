package io.jooby.spi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Consumer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.jooby.MissingValueException;
import io.jooby.QueryString;
import io.jooby.internal.UrlParser;

class ValueConvertersTest {

  @AfterAll
  static void restoreFromServiceLoader() {
    // Restore ValueConvert list for other unit tests.
    ValueConverters.builder().fromServiceLoader().set();
  }

  @Test
  void testConvert() {
    ValueConverters.builder().add((value, type) -> {
      if (type == MyValue.class) {
        MyValue mv = new MyValue();
        // we have chosen simple parameters names to make sure we don't get a
        // false positve
        // from the reflection converter.
        mv.name = value.get("n").value();
        // TODO: ValueContainer probably should have primitive convert methods.
        mv.order = Integer.parseInt(value.get("o").value());
        return mv;
      }
      return null;
    }).set();
    queryString("n=stuff&o=1", queryString -> {
      MyValue mv = queryString.to(MyValue.class);
      assertEquals("stuff", mv.name);
      assertEquals(1, mv.order);
    });
    queryString("n=stuff&missingOrder=1", queryString -> {
      try {
        queryString.to(MyValue.class);
        fail();
      } catch (MissingValueException mve) {
        assertEquals("Missing value: 'o'", mve.getMessage());
      }
    });
  }

  static class MyValue {

    public String name;
    public int order;
  }

  private void queryString(String queryString, Consumer<QueryString> consumer) {
    consumer.accept(UrlParser.queryString(queryString));
  }

}
