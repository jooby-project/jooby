package org.jooby.internal.reqparam;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.inject.TypeLiteral;

public class StringConstructorParserTest {

  public static class Value {

    private String val;

    public Value(final String val) {
      this.val = val;
    }

    @Override
    public String toString() {
      return val;
    }

  }

  public static class ValueOfNoPublic {

    private String val;

    private ValueOfNoPublic(final String val) {
      this.val = val;
    }

    @Override
    public String toString() {
      return val;
    }

  }

  @Test
  public void matches() throws Exception {
    assertEquals(true,
        new StringConstructorParser().matches(TypeLiteral.get(Value.class)));

    assertEquals(false,
        new StringConstructorParser().matches(TypeLiteral.get(ValueOfNoPublic.class)));
  }

}
