package org.jooby.internal;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jooby.Err;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.internal.reqparam.CollectionParamConverter;
import org.jooby.internal.reqparam.CommonTypesParamConverter;
import org.jooby.internal.reqparam.DateParamConverter;
import org.jooby.internal.reqparam.EnumParamConverter;
import org.jooby.internal.reqparam.LocalDateParamConverter;
import org.jooby.internal.reqparam.LocaleParamConverter;
import org.jooby.internal.reqparam.OptionalParamConverter;
import org.jooby.internal.reqparam.ParamResolver;
import org.jooby.internal.reqparam.StaticMethodParamConverter;
import org.jooby.internal.reqparam.StringConstructorParamConverter;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.inject.TypeLiteral;

public class MutantImplTest {

  public enum LETTER {
    A, B;
  }

  @Test
  public void asEmptyList() throws Exception {
    assertEquals(Collections.emptyList(),
        newMutant((String) null).toList(String.class));
  }

  @Test
  public void asBoolean() throws Exception {
    assertEquals(true, newMutant("true").booleanValue());
    assertEquals(false, newMutant("false").booleanValue());

    assertEquals(false, newMutant("false").to(boolean.class));
  }

  @Test
  public void asBooleanList() throws Exception {
    assertEquals(ImmutableList.of(Boolean.TRUE, Boolean.FALSE),
        newMutant("true", "false").toList(boolean.class));

    assertEquals(ImmutableList.of(Boolean.TRUE, Boolean.FALSE),
        newMutant("true", "false").toList(Boolean.class));

    assertEquals(ImmutableList.of(Boolean.TRUE, Boolean.FALSE),
        newMutant("true", "false").to(new TypeLiteral<List<Boolean>>() {
        }));
  }

  @Test
  public void asBooleanSet() throws Exception {
    assertEquals(ImmutableSet.of(Boolean.TRUE, Boolean.FALSE),
        newMutant("true", "false").toSet(boolean.class));

    assertEquals(ImmutableSet.of(Boolean.TRUE, Boolean.FALSE),
        newMutant("true", "false").toSet(Boolean.class));
  }

  @Test
  public void asBooleanSortedSet() throws Exception {
    assertEquals(ImmutableSet.of(Boolean.TRUE, Boolean.FALSE),
        newMutant("false", "true").toSortedSet(boolean.class));

    assertEquals(ImmutableSet.of(Boolean.TRUE, Boolean.FALSE),
        newMutant("false", "true").toSortedSet(Boolean.class));
  }

  @Test
  public void asOptionalBoolean() throws Exception {
    assertEquals(Optional.empty(), newMutant((String) null).toOptional(Boolean.class));

    assertEquals(Optional.of(true), newMutant("true").toOptional(Boolean.class));
  }

  @Test(expected = Err.class)
  public void notABoolean() throws Exception {
    assertEquals(true, newMutant("True").booleanValue());
  }

  @Test
  public void asByte() throws Exception {
    assertEquals(23, newMutant("23").byteValue());

    assertEquals((byte) 23, (byte) newMutant("23").to(Byte.class));
  }

  @Test(expected = Err.class)
  public void notAByte() throws Exception {
    assertEquals(23, newMutant("23x").byteValue());
  }

  @Test(expected = Err.class)
  public void byteOverflow() throws Exception {
    assertEquals(255, newMutant("255").byteValue());
  }

  @Test
  public void asByteList() throws Exception {
    assertEquals(ImmutableList.of((byte) 1, (byte) 2, (byte) 3),
        newMutant("1", "2", "3").toList(byte.class));

    assertEquals(ImmutableList.of((byte) 1, (byte) 2, (byte) 3),
        newMutant("1", "2", "3").toList(Byte.class));
  }

  @Test
  public void asByteSet() throws Exception {
    assertEquals(ImmutableSet.of((byte) 1, (byte) 2, (byte) 3),
        newMutant("1", "2", "3").toSet(byte.class));

    assertEquals(ImmutableSet.of((byte) 1, (byte) 2, (byte) 3),
        newMutant("1", "2", "3").toSet(Byte.class));
  }

  @Test
  public void asByteSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of((byte) 1, (byte) 2, (byte) 3),
        newMutant("1", "2", "3").toSortedSet(byte.class));

    assertEquals(ImmutableSortedSet.of((byte) 1, (byte) 2, (byte) 3),
        newMutant("1", "2", "3").toSortedSet(Byte.class));
  }

  @Test
  public void asOptionalByte() throws Exception {
    assertEquals(Optional.empty(), newMutant((String) null).toOptional(Byte.class));

    assertEquals(Optional.of((byte) 1), newMutant("1").toOptional(Byte.class));
  }

  @Test
  public void asShort() throws Exception {
    assertEquals(23, newMutant("23").shortValue());

    assertEquals((short) 23, (short) newMutant("23").to(short.class));
  }

  @Test(expected = Err.class)
  public void notAShort() throws Exception {
    assertEquals(23, newMutant("23x").shortValue());
  }

  @Test(expected = Err.class)
  public void shortOverflow() throws Exception {
    assertEquals(45071, newMutant("45071").shortValue());
  }

  @Test
  public void asShortList() throws Exception {
    assertEquals(ImmutableList.of((short) 1, (short) 2, (short) 3),
        newMutant("1", "2", "3").toList(short.class));

    assertEquals(ImmutableList.of((short) 1, (short) 2, (short) 3),
        newMutant("1", "2", "3").toList(Short.class));
  }

  @Test
  public void asShortSet() throws Exception {
    assertEquals(ImmutableSet.of((short) 1, (short) 2, (short) 3),
        newMutant("1", "2", "3").toSet(short.class));

    assertEquals(ImmutableSet.of((short) 1, (short) 2, (short) 3),
        newMutant("1", "2", "3").toSet(Short.class));
  }

  @Test
  public void asShortSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of((short) 1, (short) 2, (short) 3),
        newMutant("1", "2", "3").toSortedSet(short.class));

    assertEquals(ImmutableSortedSet.of((short) 1, (short) 2, (short) 3),
        newMutant("1", "2", "3").toSortedSet(Short.class));
  }

  @Test
  public void asOptionalShort() throws Exception {
    assertEquals(Optional.empty(), newMutant((String) null).toOptional(Short.class));

    assertEquals(Optional.of((short) 1), newMutant("1").toOptional(short.class));
  }

  @Test
  public void asInt() throws Exception {
    assertEquals(678, newMutant("678").intValue());

    assertEquals(678, (int) newMutant("678").to(int.class));

    assertEquals(678, (int) newMutant("678").to(Integer.class));
  }

  @Test
  public void asIntList() throws Exception {
    assertEquals(ImmutableList.of(1, 2, 3),
        newMutant("1", "2", "3").toList(int.class));

    assertEquals(ImmutableList.of(1, 2, 3),
        newMutant("1", "2", "3").toList(Integer.class));
  }

  @Test
  public void asIntSet() throws Exception {
    assertEquals(ImmutableSet.of(1, 2, 3),
        newMutant("1", "2", "3").toSet(int.class));

    assertEquals(ImmutableSet.of(1, 2, 3),
        newMutant("1", "2", "3").toSet(Integer.class));

    assertEquals(ImmutableSet.of(1, 2, 3),
        newMutant("1", "2", "3").to(new TypeLiteral<Set<Integer>>() {
        }));
  }

  @Test
  public void asIntSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(1, 2, 3),
        newMutant("1", "2", "3").toSortedSet(int.class));

    assertEquals(ImmutableSet.of(1, 2, 3),
        newMutant("1", "2", "3").toSortedSet(Integer.class));
  }

  @Test
  public void asOptionalInt() throws Exception {
    assertEquals(Optional.empty(), newMutant((String) null).toOptional(int.class));

    assertEquals(Optional.of(1), newMutant("1").toOptional(int.class));
  }

  @Test(expected = Err.class)
  public void notAnInt() throws Exception {
    assertEquals(23, newMutant("23x").intValue());
  }

  @Test
  public void asLong() throws Exception {
    assertEquals(6781919191l, newMutant("6781919191").longValue());

    assertEquals(6781919191l, (long) newMutant("6781919191").to(long.class));

    assertEquals(6781919191l, (long) newMutant("6781919191").to(Long.class));
  }

  @Test(expected = Err.class)
  public void notALong() throws Exception {
    assertEquals(2323113, newMutant("23113x").longValue());
  }

  @Test
  public void asLongList() throws Exception {
    assertEquals(ImmutableList.of(1l, 2l, 3l),
        newMutant("1", "2", "3").toList(long.class));

    assertEquals(ImmutableList.of(1l, 2l, 3l),
        newMutant("1", "2", "3").toList(Long.class));
  }

  @Test
  public void asLongSet() throws Exception {
    assertEquals(ImmutableSet.of(1l, 2l, 3l),
        newMutant("1", "2", "3").toSet(long.class));

    assertEquals(ImmutableSet.of(1l, 2l, 3l),
        newMutant("1", "2", "3").toSet(Long.class));
  }

  @Test
  public void asLongSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(1l, 2l, 3l),
        newMutant("1", "2", "3").toSortedSet(long.class));

    assertEquals(ImmutableSortedSet.of(1l, 2l, 3l),
        newMutant("1", "2", "3").toSortedSet(Long.class));
  }

  @Test
  public void asOptionalLong() throws Exception {
    assertEquals(Optional.empty(), newMutant((String) null).toOptional(long.class));

    assertEquals(Optional.of(1l), newMutant("1").toOptional(long.class));
  }

  @Test
  public void asFloat() throws Exception {
    assertEquals(4.3f, newMutant("4.3").floatValue(), 0);

    assertEquals(4.3f, newMutant("4.3").to(float.class), 0);
  }

  @Test(expected = Err.class)
  public void notAFloat() throws Exception {
    assertEquals(23.113, newMutant("23.113x").floatValue(), 0);
  }

  @Test
  public void asFloatList() throws Exception {
    assertEquals(ImmutableList.of(1f, 2f, 3f),
        newMutant("1", "2", "3").toList(float.class));

    assertEquals(ImmutableList.of(1f, 2f, 3f),
        newMutant("1", "2", "3").toList(Float.class));
  }

  @Test
  public void asFloatSet() throws Exception {
    assertEquals(ImmutableSet.of(1f, 2f, 3f),
        newMutant("1", "2", "3").toSet(float.class));

    Set<Float> asSet = newMutant("1", "2", "3").toSet(Float.class);
    assertEquals(ImmutableSet.of(1f, 2f, 3f),
        asSet);
  }

  @Test
  public void asFloatSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(1f, 2f, 3f),
        newMutant("1", "2", "3").toSortedSet(float.class));

    assertEquals(ImmutableSortedSet.of(1f, 2f, 3f),
        newMutant("1", "2", "3").toSortedSet(Float.class));
  }

  @Test
  public void asOptionalFloat() throws Exception {
    assertEquals(Optional.empty(), newMutant((String) null).toOptional(float.class));

    assertEquals(Optional.of(1f), newMutant("1").toOptional(float.class));
  }

  @Test
  public void asDouble() throws Exception {
    assertEquals(4.23d, newMutant("4.23").doubleValue(), 0);

    assertEquals(4.3d, newMutant("4.3").to(double.class), 0);
  }

  @Test(expected = Err.class)
  public void notADouble() throws Exception {
    assertEquals(23.113, newMutant("23.113x").doubleValue(), 0);
  }

  @Test
  public void asDoubleList() throws Exception {
    assertEquals(ImmutableList.of(1d, 2d, 3d),
        newMutant("1", "2", "3").toList(double.class));

    assertEquals(ImmutableList.of(1d, 2d, 3d),
        newMutant("1", "2", "3").toList(Double.class));
  }

  @Test
  public void asDoubleSet() throws Exception {
    assertEquals(ImmutableSet.of(1d, 2d, 3d),
        newMutant("1", "2", "3").toSet(double.class));

    assertEquals(ImmutableSet.of(1d, 2d, 3d),
        newMutant("1", "2", "3").toSet(Double.class));
  }

  @Test
  public void asDoubleSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(1d, 2d, 3d),
        newMutant("1", "2", "3").toSortedSet(double.class));

    assertEquals(ImmutableSortedSet.of(1d, 2d, 3d),
        newMutant("1", "2", "3").toSortedSet(Double.class));
  }

  @Test
  public void asOptionalDouble() throws Exception {
    assertEquals(Optional.empty(), newMutant((String) null).toOptional(double.class));

    assertEquals(Optional.of(1d), newMutant("1").toOptional(double.class));
  }

  @Test
  public void asEnum() throws Exception {
    assertEquals(LETTER.A, newMutant("A").toEnum(LETTER.class));
    assertEquals(LETTER.A, newMutant("A").toEnum(LETTER.class));
    assertEquals(LETTER.B, newMutant("B").toEnum(LETTER.class));

    assertEquals(LETTER.B, newMutant("B").to(LETTER.class));
  }

  @Test
  public void asEnumList() throws Exception {
    assertEquals(ImmutableList.of(LETTER.A, LETTER.B),
        newMutant("A", "B").toList(LETTER.class));
  }

  @Test
  public void asEnumSet() throws Exception {
    assertEquals(ImmutableSet.of(LETTER.A, LETTER.B),
        newMutant("A", "B").toSet(LETTER.class));
  }

  @Test
  public void asEnumSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of(LETTER.A, LETTER.B),
        newMutant("A", "B").toSortedSet(LETTER.class));
  }

  @Test
  public void asOptionalEnum() throws Exception {
    assertEquals(Optional.empty(), newMutant((String) null).toOptional(LETTER.class));

    assertEquals(Optional.of(LETTER.A), newMutant("A").toOptional(LETTER.class));
  }

  @Test(expected = Err.class)
  public void notAnEnum() throws Exception {
    assertEquals(LETTER.A, newMutant("c").toEnum(LETTER.class));
  }

  @Test
  public void asString() throws Exception {
    assertEquals("xx", newMutant("xx").value());

    assertEquals("xx", newMutant("xx").to(String.class));

    assertEquals("xx", newMutant("xx").toString());
  }

  @Test
  public void asStringList() throws Exception {
    assertEquals(ImmutableList.of("aa", "bb"),
        newMutant("aa", "bb").toList(String.class));

    assertEquals("[aa, bb]", newMutant("aa", "bb").toString());
  }

  @Test
  public void asStringSet() throws Exception {
    assertEquals(ImmutableSet.of("aa", "bb"),
        newMutant("aa", "bb", "bb").toSet(String.class));
  }

  @Test
  public void asStringSortedSet() throws Exception {
    assertEquals(ImmutableSortedSet.of("aa", "bb"),
        newMutant("aa", "bb", "bb").toSortedSet(String.class));
  }

  @Test
  public void asOptionalString() throws Exception {
    assertEquals(Optional.empty(), newMutant((String) null).toOptional(String.class));

    assertEquals(Optional.empty(), newMutant((String) null).toOptional());

    assertEquals(Optional.of("A"), newMutant("A").toOptional(String.class));

    assertEquals(Optional.of("A"), newMutant("A").toOptional());
  }

  @Test
  public void emptyList() throws Exception {
    assertEquals(Collections.emptyList(), newMutant(new String[0]).toList(String.class));
    assertEquals(false, newMutant(new String[0]).isPresent());
    assertEquals("", newMutant(new String[0]).toString());
  }

  @Test
  public void nullList() throws Exception {
    assertEquals(Collections.emptyList(), newMutant((String) null).toList(String.class));
    assertEquals(false, newMutant((String) null).isPresent());
    assertEquals("", newMutant((String) null).toString());
  }

  private Mutant newMutant(final String... values) {
    return new MutantImpl(newConverter(), values);
  }

  private Mutant newMutant(final String value) {
    return new MutantImpl(newConverter(), value == null ? null
        : new String[]{value });
  }

  private ParamResolver newConverter() {
    return new ParamResolver(() -> createMock(Request.class),
        Sets.newLinkedHashSet(
            Arrays.asList(
                new CommonTypesParamConverter(),
                new CollectionParamConverter(),
                new OptionalParamConverter(),
                new EnumParamConverter(),
                new DateParamConverter("dd/MM/yyyy"),
                new LocalDateParamConverter(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                new LocaleParamConverter(),
                new StaticMethodParamConverter("valueOf"),
                new StringConstructorParamConverter(),
                new StaticMethodParamConverter("fromString"),
                new StaticMethodParamConverter("forName")
                )));
  }
}
