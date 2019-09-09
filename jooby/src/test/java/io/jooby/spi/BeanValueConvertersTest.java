package io.jooby.spi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Consumer;

import io.jooby.TypeMismatchException;
import io.jooby.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.jooby.MissingValueException;
import io.jooby.QueryString;
import io.jooby.internal.UrlParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BeanValueConvertersTest {

  @AfterAll
  static void restoreFromServiceLoader() {
    // Restore ValueConvert list for other unit tests.
    BeanValueConverters.builder().fromServiceLoader().set();
  }

  @Test
  void testConvert() {
    BeanValueConverters.builder().add(new BeanValueConverter() {
      @Override public boolean supportsType(@Nonnull Class<?> type) {
        return type == MyValue.class;
      }

      @Nullable @Override public Object convert(@Nonnull Value value,
          @Nonnull Class<?> type) throws TypeMismatchException {
        MyValue mv = new MyValue();
        // we have chosen simple parameters names to make sure we don't get a
        // false positve
        // from the reflection converter.
        mv.name = value.get("n").value();
        // TODO: ValueContainer probably should have primitive convert methods.
        mv.order = value.get("o").intValue();
        return mv;
      }
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
