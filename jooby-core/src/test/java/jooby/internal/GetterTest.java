package jooby.internal;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jooby.Variant;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.TypeLiteral;

public class GetterTest {

  public enum LETTER {
    A, B;
  }

  @Test
  public void asEmptyList() throws Exception {
    assertEquals(Collections.emptyList(),
        newGetter((String) null).toList(String.class));
  }

  @Test
  public void asBoolean() throws Exception {
    assertEquals(true, newGetter("true").booleanValue());
    assertEquals(false, newGetter("false").booleanValue());

    assertEquals(false, newGetter("false").to(boolean.class));
  }

  @Test
  public void asBooleanList() throws Exception {
    assertEquals(ImmutableList.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("true", "false").toList(boolean.class));

    assertEquals(ImmutableList.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("true", "false").toList(Boolean.class));

    assertEquals(ImmutableList.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("true", "false").to(new TypeLiteral<List<Boolean>>() {
        }));
  }

  @Test
  public void asBooleanSet() throws Exception {
    assertEquals(ImmutableSet.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("true", "false").toSet(boolean.class));

    assertEquals(ImmutableSet.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("true", "false").toSet(Boolean.class));
  }

  @Test
  public void asBooleanSortedSet() throws Exception {
    assertEquals(ImmutableSet.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("false", "true").toSortedSet(boolean.class));

    assertEquals(ImmutableSet.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("false", "true").toSortedSet(Boolean.class));
  }

  @Test
  public void asOptionalBoolean() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).toOptional(Boolean.class));

    assertEquals(Optional.of(true), newGetter("true").toOptional(Boolean.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void notABoolean() throws Exception {
    assertEquals(true, newGetter("True").booleanValue());
  }

  @Test
  public void asByte() throws Exception {
    assertEquals(23, newGetter("23").byteValue());

    assertEquals((byte) 23, (byte) newGetter("23").to(Byte.class));
  }

  @Test(expected = NumberFormatException.class)
  public void notAByte() throws Exception {
    assertEquals(23, newGetter("23x").byteValue());
  }

  @Test(expected = NumberFormatException.class)
  public void byteOverflow() throws Exception {
    assertEquals(255, newGetter("255").byteValue());
  }

  @Test
  public void asByteList() throws Exception {
    assertEquals(ImmutableList.of((byte) 1, (byte) 2, (byte) 3),
        newGetter("1", "2", "3").toList(byte.class));

    assertEquals(ImmutableList.of((byte) 1, (byte) 2, (byte) 3),
        newGetter("1", "2", "3").toList(Byte.class));
  }

  @Test
  public void asByteSet() throws Exception {
    assertEquals(ImmutableSet.of((byte) 1, (byte) 2, (byte) 3),
        newGetter("1", "2", "3").toSet(byte.class));

    assertEquals(ImmutableSet.of((byte) 1, (byte) 2, (byte) 3),
        newGetter("1", "2", "3").toSet(Byte.class));
  }

  @Test
  public void asByteSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of((byte) 1, (byte) 2, (byte) 3),
        newGetter("1", "2", "3").toSortedSet(byte.class));

    assertEquals(ImmutableSortedSet.of((byte) 1, (byte) 2, (byte) 3),
        newGetter("1", "2", "3").toSortedSet(Byte.class));
  }

  @Test
  public void asOptionalByte() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).toOptional(Byte.class));

    assertEquals(Optional.of((byte) 1), newGetter("1").toOptional(Byte.class));
  }

  @Test
  public void asShort() throws Exception {
    assertEquals(23, newGetter("23").shortValue());

    assertEquals((short) 23, (short) newGetter("23").to(short.class));
  }

  @Test(expected = NumberFormatException.class)
  public void notAShort() throws Exception {
    assertEquals(23, newGetter("23x").shortValue());
  }

  @Test(expected = NumberFormatException.class)
  public void shortOverflow() throws Exception {
    assertEquals(45071, newGetter("45071").shortValue());
  }

  @Test
  public void asShortList() throws Exception {
    assertEquals(ImmutableList.of((short) 1, (short) 2, (short) 3),
        newGetter("1", "2", "3").toList(short.class));

    assertEquals(ImmutableList.of((short) 1, (short) 2, (short) 3),
        newGetter("1", "2", "3").toList(Short.class));
  }

  @Test
  public void asShortSet() throws Exception {
    assertEquals(ImmutableSet.of((short) 1, (short) 2, (short) 3),
        newGetter("1", "2", "3").toSet(short.class));

    assertEquals(ImmutableSet.of((short) 1, (short) 2, (short) 3),
        newGetter("1", "2", "3").toSet(Short.class));
  }

  @Test
  public void asShortSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of((short) 1, (short) 2, (short) 3),
        newGetter("1", "2", "3").toSortedSet(short.class));

    assertEquals(ImmutableSortedSet.of((short) 1, (short) 2, (short) 3),
        newGetter("1", "2", "3").toSortedSet(Short.class));
  }

  @Test
  public void asOptionalShort() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).toOptional(Short.class));

    assertEquals(Optional.of((short) 1), newGetter("1").toOptional(short.class));
  }

  @Test
  public void asInt() throws Exception {
    assertEquals(678, newGetter("678").intValue());

    assertEquals(678, (int) newGetter("678").to(int.class));

    assertEquals(678, (int) newGetter("678").to(Integer.class));
  }

  @Test
  public void asIntList() throws Exception {
    assertEquals(ImmutableList.of(1, 2, 3),
        newGetter("1", "2", "3").toList(int.class));

    assertEquals(ImmutableList.of(1, 2, 3),
        newGetter("1", "2", "3").toList(Integer.class));
  }

  @Test
  public void asIntSet() throws Exception {
    assertEquals(ImmutableSet.of(1, 2, 3),
        newGetter("1", "2", "3").toSet(int.class));

    assertEquals(ImmutableSet.of(1, 2, 3),
        newGetter("1", "2", "3").toSet(Integer.class));
  }

  @Test
  public void asIntSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(1, 2, 3),
        newGetter("1", "2", "3").toSortedSet(int.class));

    assertEquals(ImmutableSet.of(1, 2, 3),
        newGetter("1", "2", "3").toSortedSet(Integer.class));
  }

  @Test
  public void asOptionalInt() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).toOptional(int.class));

    assertEquals(Optional.of(1), newGetter("1").toOptional(int.class));
  }

  @Test(expected = NumberFormatException.class)
  public void notAnInt() throws Exception {
    assertEquals(23, newGetter("23x").intValue());
  }

  @Test
  public void asLong() throws Exception {
    assertEquals(6781919191l, newGetter("6781919191").longValue());

    assertEquals(6781919191l, (long) newGetter("6781919191").to(long.class));

    assertEquals(6781919191l, (long) newGetter("6781919191").to(Long.class));
  }

  @Test(expected = NumberFormatException.class)
  public void notALong() throws Exception {
    assertEquals(2323113, newGetter("23113x").longValue());
  }

  @Test
  public void asLongList() throws Exception {
    assertEquals(ImmutableList.of(1l, 2l, 3l),
        newGetter("1", "2", "3").toList(long.class));

    assertEquals(ImmutableList.of(1l, 2l, 3l),
        newGetter("1", "2", "3").toList(Long.class));
  }

  @Test
  public void asLongSet() throws Exception {
    assertEquals(ImmutableSet.of(1l, 2l, 3l),
        newGetter("1", "2", "3").toSet(long.class));

    assertEquals(ImmutableSet.of(1l, 2l, 3l),
        newGetter("1", "2", "3").toSet(Long.class));
  }

  @Test
  public void asLongSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(1l, 2l, 3l),
        newGetter("1", "2", "3").toSortedSet(long.class));

    assertEquals(ImmutableSortedSet.of(1l, 2l, 3l),
        newGetter("1", "2", "3").toSortedSet(Long.class));
  }

  @Test
  public void asOptionalLong() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).toOptional(long.class));

    assertEquals(Optional.of(1l), newGetter("1").toOptional(long.class));
  }

  @Test
  public void asFloat() throws Exception {
    assertEquals(4.3f, newGetter("4.3").floatValue(), 0);

    assertEquals(4.3f, newGetter("4.3").to(float.class), 0);
  }

  @Test(expected = NumberFormatException.class)
  public void notAFloat() throws Exception {
    assertEquals(23.113, newGetter("23.113x").floatValue(), 0);
  }

  @Test
  public void asFloatList() throws Exception {
    assertEquals(ImmutableList.of(1f, 2f, 3f),
        newGetter("1", "2", "3").toList(float.class));

    assertEquals(ImmutableList.of(1f, 2f, 3f),
        newGetter("1", "2", "3").toList(Float.class));
  }

  @Test
  public void asFloatSet() throws Exception {
    assertEquals(ImmutableSet.of(1f, 2f, 3f),
        newGetter("1", "2", "3").toSet(float.class));

    Set<Float> asSet = newGetter("1", "2", "3").toSet(Float.class);
    assertEquals(ImmutableSet.of(1f, 2f, 3f),
        asSet);
  }

  @Test
  public void asFloatSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(1f, 2f, 3f),
        newGetter("1", "2", "3").toSortedSet(float.class));

    assertEquals(ImmutableSortedSet.of(1f, 2f, 3f),
        newGetter("1", "2", "3").toSortedSet(Float.class));
  }

  @Test
  public void asOptionalFloat() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).toOptional(float.class));

    assertEquals(Optional.of(1f), newGetter("1").toOptional(float.class));
  }

  @Test
  public void asDouble() throws Exception {
    assertEquals(4.23d, newGetter("4.23").doubleValue(), 0);

    assertEquals(4.3d, newGetter("4.3").to(double.class), 0);
  }

  @Test(expected = NumberFormatException.class)
  public void notADouble() throws Exception {
    assertEquals(23.113, newGetter("23.113x").doubleValue(), 0);
  }

  @Test
  public void asDoubleList() throws Exception {
    assertEquals(ImmutableList.of(1d, 2d, 3d),
        newGetter("1", "2", "3").toList(double.class));

    assertEquals(ImmutableList.of(1d, 2d, 3d),
        newGetter("1", "2", "3").toList(Double.class));
  }

  @Test
  public void asDoubleSet() throws Exception {
    assertEquals(ImmutableSet.of(1d, 2d, 3d),
        newGetter("1", "2", "3").toSet(double.class));

    assertEquals(ImmutableSet.of(1d, 2d, 3d),
        newGetter("1", "2", "3").toSet(Double.class));
  }

  @Test
  public void asDoubleSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(1d, 2d, 3d),
        newGetter("1", "2", "3").toSortedSet(double.class));

    assertEquals(ImmutableSortedSet.of(1d, 2d, 3d),
        newGetter("1", "2", "3").toSortedSet(Double.class));
  }

  @Test
  public void asOptionalDouble() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).toOptional(double.class));

    assertEquals(Optional.of(1d), newGetter("1").toOptional(double.class));
  }

  @Test
  public void asEnum() throws Exception {
    assertEquals(LETTER.A, newGetter("a").enumValue(LETTER.class));
    assertEquals(LETTER.A, newGetter("A").enumValue(LETTER.class));
    assertEquals(LETTER.B, newGetter("B").enumValue(LETTER.class));

    assertEquals(LETTER.B, newGetter("B").to(LETTER.class));
  }

  @Test
  public void asEnumList() throws Exception {
    assertEquals(ImmutableList.of(LETTER.A, LETTER.B),
        newGetter("a", "b").toList(LETTER.class));
  }

  @Test
  public void asEnumSet() throws Exception {
    assertEquals(ImmutableSet.of(LETTER.A, LETTER.B),
        newGetter("a", "b").toSet(LETTER.class));
  }

  @Test
  public void asEnumSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(LETTER.A, LETTER.B),
        newGetter("a", "b").toSortedSet(LETTER.class));
  }

  @Test
  public void asOptionalEnum() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).toOptional(LETTER.class));

    assertEquals(Optional.of(LETTER.A), newGetter("A").toOptional(LETTER.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void notAnEnum() throws Exception {
    assertEquals(LETTER.A, newGetter("c").enumValue(LETTER.class));
  }

  @Test
  public void asString() throws Exception {
    assertEquals("xx", newGetter("xx").toString());

    assertEquals("xx", newGetter("xx").to(String.class));
  }

  @Test
  public void asStringList() throws Exception {
    assertEquals(ImmutableList.of("aa", "bb"),
        newGetter("aa", "bb").toList(String.class));
  }

  @Test
  public void asStringSet() throws Exception {
    assertEquals(ImmutableSet.of("aa", "bb"),
        newGetter("aa", "bb", "bb").toSet(String.class));
  }

  @Test
  public void asStringSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of("aa", "bb"),
        newGetter("aa", "bb", "bb").toSortedSet(String.class));
  }

  @Test
  public void asOptionalString() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).toOptional(String.class));

    assertEquals(Optional.of("A"), newGetter("A").toOptional(String.class));
  }

  private Variant newGetter(final String... values) {
    return new VariantImpl("param", ImmutableList.copyOf(values), Collections.emptySet());
  }

  private Variant newGetter(final String value) {
    return new VariantImpl("param", value == null ? null : ImmutableList.of(value),
        Collections.emptySet());
  }
}
