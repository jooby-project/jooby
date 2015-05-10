package org.jooby.internal.reqparam;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.inject.TypeLiteral;

public class StaticMethodParserTest {

  public static class Value {

    private String val;

    private Value(final String val) {
      this.val = val;
    }

    public static Value valueOf(final String val) {
      return new Value(val);
    }

    @Override
    public String toString() {
      return val;
    }

  }

  public static class ValueOfNoStatic {

    public ValueOfNoStatic valueOf() {
      return new ValueOfNoStatic();
    }

  }

  public static class ValueOfNoPublic {

    @SuppressWarnings("unused")
    private static ValueOfNoStatic valueOf() {
      return new ValueOfNoStatic();
    }

  }

  public static class ValueOfNoPublicNoStatic {

    ValueOfNoStatic valueOf() {
      return new ValueOfNoStatic();
    }

  }

  @Test
  public void defaults() throws Exception {
    new StaticMethodParser("valueOf");
  }

  @Test(expected = NullPointerException.class)
  public void nullArg() throws Exception {
    new StaticMethodParser(null);
  }

  @Test
  public void matches() throws Exception {
    assertEquals(true, new StaticMethodParser("valueOf").matches(TypeLiteral.get(Value.class)));

    assertEquals(false,
        new StaticMethodParser("valueOf").matches(TypeLiteral.get(ValueOfNoStatic.class)));

    assertEquals(false,
        new StaticMethodParser("valueOf").matches(TypeLiteral.get(ValueOfNoPublic.class)));

    assertEquals(false,
        new StaticMethodParser("valueOf").matches(TypeLiteral.get(ValueOfNoPublicNoStatic.class)));
  }

}
