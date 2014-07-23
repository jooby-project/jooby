package jooby.internal;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jooby.HttpField;

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
        newGetter((String) null).getList(String.class));
  }

  @Test
  public void asBoolean() throws Exception {
    assertEquals(true, newGetter("true").getBoolean());
    assertEquals(false, newGetter("false").getBoolean());

    assertEquals(false, newGetter("false").get(boolean.class));
  }

  @Test
  public void asBooleanList() throws Exception {
    assertEquals(ImmutableList.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("true", "false").getList(boolean.class));

    assertEquals(ImmutableList.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("true", "false").getList(Boolean.class));

    assertEquals(ImmutableList.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("true", "false").get(new TypeLiteral<List<Boolean>>() {
        }));
  }

  @Test
  public void asBooleanSet() throws Exception {
    assertEquals(ImmutableSet.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("true", "false").getSet(boolean.class));

    assertEquals(ImmutableSet.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("true", "false").getSet(Boolean.class));
  }

  @Test
  public void asBooleanSortedSet() throws Exception {
    assertEquals(ImmutableSet.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("false", "true").getSortedSet(boolean.class));

    assertEquals(ImmutableSet.of(Boolean.TRUE, Boolean.FALSE),
        newGetter("false", "true").getSortedSet(Boolean.class));
  }

  @Test
  public void asOptionalBoolean() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).getOptional(Boolean.class));

    assertEquals(Optional.of(true), newGetter("true").getOptional(Boolean.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void notABoolean() throws Exception {
    assertEquals(true, newGetter("True").getBoolean());
  }

  @Test
  public void asByte() throws Exception {
    assertEquals(23, newGetter("23").getByte());

    assertEquals((byte) 23, (byte) newGetter("23").get(Byte.class));
  }

  @Test(expected = NumberFormatException.class)
  public void notAByte() throws Exception {
    assertEquals(23, newGetter("23x").getByte());
  }

  @Test(expected = NumberFormatException.class)
  public void byteOverflow() throws Exception {
    assertEquals(255, newGetter("255").getByte());
  }

  @Test
  public void asByteList() throws Exception {
    assertEquals(ImmutableList.of((byte) 1, (byte) 2, (byte) 3),
        newGetter("1", "2", "3").getList(byte.class));

    assertEquals(ImmutableList.of((byte) 1, (byte) 2, (byte) 3),
        newGetter("1", "2", "3").getList(Byte.class));
  }

  @Test
  public void asByteSet() throws Exception {
    assertEquals(ImmutableSet.of((byte) 1, (byte) 2, (byte) 3),
        newGetter("1", "2", "3").getSet(byte.class));

    assertEquals(ImmutableSet.of((byte) 1, (byte) 2, (byte) 3),
        newGetter("1", "2", "3").getSet(Byte.class));
  }

  @Test
  public void asByteSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of((byte) 1, (byte) 2, (byte) 3),
        newGetter("1", "2", "3").getSortedSet(byte.class));

    assertEquals(ImmutableSortedSet.of((byte) 1, (byte) 2, (byte) 3),
        newGetter("1", "2", "3").getSortedSet(Byte.class));
  }

  @Test
  public void asOptionalByte() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).getOptional(Byte.class));

    assertEquals(Optional.of((byte) 1), newGetter("1").getOptional(Byte.class));
  }

  @Test
  public void asShort() throws Exception {
    assertEquals(23, newGetter("23").getShort());

    assertEquals((short) 23, (short) newGetter("23").get(short.class));
  }

  @Test(expected = NumberFormatException.class)
  public void notAShort() throws Exception {
    assertEquals(23, newGetter("23x").getShort());
  }

  @Test(expected = NumberFormatException.class)
  public void shortOverflow() throws Exception {
    assertEquals(45071, newGetter("45071").getShort());
  }

  @Test
  public void asShortList() throws Exception {
    assertEquals(ImmutableList.of((short) 1, (short) 2, (short) 3),
        newGetter("1", "2", "3").getList(short.class));

    assertEquals(ImmutableList.of((short) 1, (short) 2, (short) 3),
        newGetter("1", "2", "3").getList(Short.class));
  }

  @Test
  public void asShortSet() throws Exception {
    assertEquals(ImmutableSet.of((short) 1, (short) 2, (short) 3),
        newGetter("1", "2", "3").getSet(short.class));

    assertEquals(ImmutableSet.of((short) 1, (short) 2, (short) 3),
        newGetter("1", "2", "3").getSet(Short.class));
  }

  @Test
  public void asShortSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of((short) 1, (short) 2, (short) 3),
        newGetter("1", "2", "3").getSortedSet(short.class));

    assertEquals(ImmutableSortedSet.of((short) 1, (short) 2, (short) 3),
        newGetter("1", "2", "3").getSortedSet(Short.class));
  }

  @Test
  public void asOptionalShort() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).getOptional(Short.class));

    assertEquals(Optional.of((short) 1), newGetter("1").getOptional(short.class));
  }

  @Test
  public void asInt() throws Exception {
    assertEquals(678, newGetter("678").getInt());

    assertEquals(678, (int) newGetter("678").get(int.class));

    assertEquals(678, (int) newGetter("678").get(Integer.class));
  }

  @Test
  public void asIntList() throws Exception {
    assertEquals(ImmutableList.of(1, 2, 3),
        newGetter("1", "2", "3").getList(int.class));

    assertEquals(ImmutableList.of(1, 2, 3),
        newGetter("1", "2", "3").getList(Integer.class));
  }

  @Test
  public void asIntSet() throws Exception {
    assertEquals(ImmutableSet.of(1, 2, 3),
        newGetter("1", "2", "3").getSet(int.class));

    assertEquals(ImmutableSet.of(1, 2, 3),
        newGetter("1", "2", "3").getSet(Integer.class));
  }

  @Test
  public void asIntSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(1, 2, 3),
        newGetter("1", "2", "3").getSortedSet(int.class));

    assertEquals(ImmutableSet.of(1, 2, 3),
        newGetter("1", "2", "3").getSortedSet(Integer.class));
  }

  @Test
  public void asOptionalInt() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).getOptional(int.class));

    assertEquals(Optional.of(1), newGetter("1").getOptional(int.class));
  }

  @Test(expected = NumberFormatException.class)
  public void notAnInt() throws Exception {
    assertEquals(23, newGetter("23x").getInt());
  }

  @Test
  public void asLong() throws Exception {
    assertEquals(6781919191l, newGetter("6781919191").getLong());

    assertEquals(6781919191l, (long) newGetter("6781919191").get(long.class));

    assertEquals(6781919191l, (long) newGetter("6781919191").get(Long.class));
  }

  @Test(expected = NumberFormatException.class)
  public void notALong() throws Exception {
    assertEquals(2323113, newGetter("23113x").getLong());
  }

  @Test
  public void asLongList() throws Exception {
    assertEquals(ImmutableList.of(1l, 2l, 3l),
        newGetter("1", "2", "3").getList(long.class));

    assertEquals(ImmutableList.of(1l, 2l, 3l),
        newGetter("1", "2", "3").getList(Long.class));
  }

  @Test
  public void asLongSet() throws Exception {
    assertEquals(ImmutableSet.of(1l, 2l, 3l),
        newGetter("1", "2", "3").getSet(long.class));

    assertEquals(ImmutableSet.of(1l, 2l, 3l),
        newGetter("1", "2", "3").getSet(Long.class));
  }

  @Test
  public void asLongSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(1l, 2l, 3l),
        newGetter("1", "2", "3").getSortedSet(long.class));

    assertEquals(ImmutableSortedSet.of(1l, 2l, 3l),
        newGetter("1", "2", "3").getSortedSet(Long.class));
  }

  @Test
  public void asOptionalLong() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).getOptional(long.class));

    assertEquals(Optional.of(1l), newGetter("1").getOptional(long.class));
  }

  @Test
  public void asFloat() throws Exception {
    assertEquals(4.3f, newGetter("4.3").getFloat(), 0);

    assertEquals(4.3f, newGetter("4.3").get(float.class), 0);
  }

  @Test(expected = NumberFormatException.class)
  public void notAFloat() throws Exception {
    assertEquals(23.113, newGetter("23.113x").getFloat(), 0);
  }

  @Test
  public void asFloatList() throws Exception {
    assertEquals(ImmutableList.of(1f, 2f, 3f),
        newGetter("1", "2", "3").getList(float.class));

    assertEquals(ImmutableList.of(1f, 2f, 3f),
        newGetter("1", "2", "3").getList(Float.class));
  }

  @Test
  public void asFloatSet() throws Exception {
    assertEquals(ImmutableSet.of(1f, 2f, 3f),
        newGetter("1", "2", "3").getSet(float.class));

    Set<Float> asSet = newGetter("1", "2", "3").getSet(Float.class);
    assertEquals(ImmutableSet.of(1f, 2f, 3f),
        asSet);
  }

  @Test
  public void asFloatSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(1f, 2f, 3f),
        newGetter("1", "2", "3").getSortedSet(float.class));

    assertEquals(ImmutableSortedSet.of(1f, 2f, 3f),
        newGetter("1", "2", "3").getSortedSet(Float.class));
  }

  @Test
  public void asOptionalFloat() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).getOptional(float.class));

    assertEquals(Optional.of(1f), newGetter("1").getOptional(float.class));
  }

  @Test
  public void asDouble() throws Exception {
    assertEquals(4.23d, newGetter("4.23").getDouble(), 0);

    assertEquals(4.3d, newGetter("4.3").get(double.class), 0);
  }

  @Test(expected = NumberFormatException.class)
  public void notADouble() throws Exception {
    assertEquals(23.113, newGetter("23.113x").getDouble(), 0);
  }

  @Test
  public void asDoubleList() throws Exception {
    assertEquals(ImmutableList.of(1d, 2d, 3d),
        newGetter("1", "2", "3").getList(double.class));

    assertEquals(ImmutableList.of(1d, 2d, 3d),
        newGetter("1", "2", "3").getList(Double.class));
  }

  @Test
  public void asDoubleSet() throws Exception {
    assertEquals(ImmutableSet.of(1d, 2d, 3d),
        newGetter("1", "2", "3").getSet(double.class));

    assertEquals(ImmutableSet.of(1d, 2d, 3d),
        newGetter("1", "2", "3").getSet(Double.class));
  }

  @Test
  public void asDoubleSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(1d, 2d, 3d),
        newGetter("1", "2", "3").getSortedSet(double.class));

    assertEquals(ImmutableSortedSet.of(1d, 2d, 3d),
        newGetter("1", "2", "3").getSortedSet(Double.class));
  }

  @Test
  public void asOptionalDouble() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).getOptional(double.class));

    assertEquals(Optional.of(1d), newGetter("1").getOptional(double.class));
  }

  @Test
  public void asEnum() throws Exception {
    assertEquals(LETTER.A, newGetter("a").getEnum(LETTER.class));
    assertEquals(LETTER.A, newGetter("A").getEnum(LETTER.class));
    assertEquals(LETTER.B, newGetter("B").getEnum(LETTER.class));

    assertEquals(LETTER.B, newGetter("B").get(LETTER.class));
  }

  @Test
  public void asEnumList() throws Exception {
    assertEquals(ImmutableList.of(LETTER.A, LETTER.B),
        newGetter("a", "b").getList(LETTER.class));
  }

  @Test
  public void asEnumSet() throws Exception {
    assertEquals(ImmutableSet.of(LETTER.A, LETTER.B),
        newGetter("a", "b").getSet(LETTER.class));
  }

  @Test
  public void asEnumSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(LETTER.A, LETTER.B),
        newGetter("a", "b").getSortedSet(LETTER.class));
  }

  @Test
  public void asOptionalEnum() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).getOptional(LETTER.class));

    assertEquals(Optional.of(LETTER.A), newGetter("A").getOptional(LETTER.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void notAnEnum() throws Exception {
    assertEquals(LETTER.A, newGetter("c").getEnum(LETTER.class));
  }

  @Test
  public void asString() throws Exception {
    assertEquals("xx", newGetter("xx").getString());

    assertEquals("xx", newGetter("xx").get(String.class));
  }

  @Test
  public void asStringList() throws Exception {
    assertEquals(ImmutableList.of("aa", "bb"),
        newGetter("aa", "bb").getList(String.class));
  }

  @Test
  public void asStringSet() throws Exception {
    assertEquals(ImmutableSet.of("aa", "bb"),
        newGetter("aa", "bb", "bb").getSet(String.class));
  }

  @Test
  public void asStringSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of("aa", "bb"),
        newGetter("aa", "bb", "bb").getSortedSet(String.class));
  }

  @Test
  public void asOptionalString() throws Exception {
    assertEquals(Optional.empty(), newGetter((String) null).getOptional(String.class));

    assertEquals(Optional.of("A"), newGetter("A").getOptional(String.class));
  }

  private HttpField newGetter(final String... values) {
    return new GetterImpl("param", ImmutableList.copyOf(values));
  }

  private HttpField newGetter(final String value) {
    return new GetterImpl("param", value == null ? null : ImmutableList.of(value));
  }
}
