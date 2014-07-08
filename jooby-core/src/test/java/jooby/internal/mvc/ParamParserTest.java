package jooby.internal.mvc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.util.Types;

public class ParamParserTest {

  public enum LETTER {
    A, B;
  }

  @Test
  public void string() throws Exception {
    eq("xx", String.class, "xx");
  }

  @Test
  public void stringArray() throws Exception {
    assertArrayEquals(new String[]{"a", "b" }, (Object[]) parse(String[].class, "a", "b"));
  }

  @Test
  public void stringList() throws Exception {
    assertEquals(Arrays.asList("a", "bc"), parse(Types.listOf(String.class), "a", "bc"));
  }

  @Test
  public void stringSet() throws Exception {
    assertEquals(Sets.newHashSet("a", "bc"), parse(Types.setOf(String.class), "a", "bc"));
  }

  @Test
  public void sortedSet() throws Exception {
    assertEquals(Sets.newTreeSet(Arrays.asList("1", "2", "3")),
        parse(Types.newParameterizedType(SortedSet.class, String.class), "1", "3", "2"));
  }

  @Test
  public void bool() throws Exception {
    eq("true", boolean.class, true);
    eq("false", boolean.class, false);

    eq("true", Boolean.class, true);
    eq("false", Boolean.class, false);
  }

  @Test
  public void boolArray() throws Exception {
    assertArrayEquals(new Boolean[]{Boolean.FALSE, Boolean.TRUE },
        (Boolean[]) parse(Boolean[].class, "false", "true"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void notBoolean() throws Exception {
    eq("True", boolean.class, true);
  }

  @Test
  public void integer() throws Exception {
    eq("2", int.class, 2);

    eq("12", Integer.class, 12);
  }

  @Test
  public void intArray() throws Exception {
    assertArrayEquals(new Integer[]{7, 14 },
        (Object[]) parse(Integer[].class, "7", "14"));
  }

  @Test
  public void doubles() throws Exception {
    eq("2.98", double.class, 2.98);

    eq("12", Double.class, 12d);
  }

  @Test
  public void enums() throws Exception {
    eq("a", LETTER.class, LETTER.A);

    eq("A", LETTER.class, LETTER.A);
  }

  @Test
  public void floats() throws Exception {
    eq("2.98", float.class, 2.98f);

    eq("12", Float.class, 12f);
  }

  @Test
  public void fromString() throws Exception {
    eq("b0b41825-062d-4003-ace5-b55c486fb639", UUID.class,
        UUID.fromString("b0b41825-062d-4003-ace5-b55c486fb639"));

  }

  @Test
  public void optionalArg() throws Exception {
    eq(null, Types.newParameterizedType(Optional.class, Integer.class), Optional.empty());
  }

  @Test
  public void optionalInt() throws Exception {
    eq("2", Types.newParameterizedType(Optional.class, Integer.class), Optional.of(2));
  }

  @Test
  public void optionalListOfInt() throws Exception {
    Object result = parse(Types.newParameterizedType(Optional.class, Types.listOf(Integer.class)),
        "1", "2");
    assertEquals(Optional.of(Arrays.asList(1, 2)), result);
  }

  private void eq(final String value, final Type type, final Object expected)
      throws Exception {
    Assert.assertEquals(expected,
        ParamParser.parser(type).get().parse(type, value == null ? null : ImmutableList.of(value)));
  }

  private Object parse(final Type type, final String... values)
      throws Exception {
    return ParamParser.parser(type).get().parse(type, ImmutableList.copyOf(values));
  }

}
