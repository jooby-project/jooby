package org.jooby.internal;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;

import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Parser;
import org.jooby.internal.reqparam.DateParser;
import org.jooby.internal.reqparam.LocalDateParser;
import org.jooby.internal.reqparam.LocaleParser;
import org.jooby.internal.reqparam.ParserExecutor;
import org.jooby.internal.reqparam.StaticMethodParser;
import org.jooby.internal.reqparam.StringConstructorParser;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;

public class ParamConverterTest {

  public enum Letter {

    A,

    B;
  }

  public static class StringBean {

    private String value;

    public StringBean(final String value) {
      this.value = value;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof StringBean) {
        return value.equals(((StringBean) obj).value);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public String toString() {
      return value.toString();
    }

  }

  public static class ValueOf {

    private String value;

    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof ValueOf) {
        return value.equals(((ValueOf) obj).value);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public String toString() {
      return value.toString();
    }

    public static ValueOf valueOf(final String value) {
      ValueOf v = new ValueOf();
      v.value = value;
      return v;
    }

  }

  @Test
  public void nullShouldResolveAsEmptyList() throws Exception {
    ParserExecutor resolver = newParser();
    List<String> value = resolver.convert(TypeLiteral.get(Types.listOf(String.class)), data());
    assertNotNull(value);
    assertTrue(value.isEmpty());
  }

  @Test
  public void shouldConvertToDateFromString() throws Exception {
    ParserExecutor resolver = newParser();
    Date date = resolver.convert(TypeLiteral.get(Date.class), data("22/02/2014"));
    assertNotNull(date);

    // local vs travis (date time zone)
    assertTrue(1393038000000L == date.getTime() || 1393027200000L == date.getTime());
  }

  private Object data(final String... value) {
    return ImmutableList.copyOf(value);
  }

  @Test
  public void shouldConvertToDateFromLong() throws Exception {
    ParserExecutor resolver = newParser();
    Date date = resolver.convert(TypeLiteral.get(Date.class), data("1393038000000"));
    assertNotNull(date);
    assertEquals("22/02/2014", new SimpleDateFormat("dd/MM/yyyy").format(date));
  }

  @Test
  public void shouldConvertToLocalDateFromString() throws Exception {
    ParserExecutor resolver = newParser();
    LocalDate date = resolver.convert(TypeLiteral.get(LocalDate.class), data("22/02/2014"));
    assertNotNull(date);
    assertEquals(22, date.getDayOfMonth());
    assertEquals(2, date.getMonthValue());
    assertEquals(2014, date.getYear());
  }

  @Test
  public void shouldConvertToLocalDateFromLong() throws Exception {
    ParserExecutor resolver = newParser();
    LocalDate date = resolver.convert(TypeLiteral.get(LocalDate.class), data("1393038000000"));
    assertNotNull(date);
    assertEquals(22, date.getDayOfMonth());
    assertEquals(2, date.getMonthValue());
    assertEquals(2014, date.getYear());
  }

  @Test
  public void shouldConvertBeanWithStringConstructor() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(new StringBean("231"),
        resolver.convert(TypeLiteral.get(StringBean.class), data("231")));
  }

  @Test
  public void shouldConvertListOfBeanWithStringConstructor() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(Lists.newArrayList(new StringBean("231")),
        resolver.convert(TypeLiteral.get(Types.listOf(StringBean.class)), data("231")));
  }

  @Test
  public void shouldConvertWithValueOfStaticMethod() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(ValueOf.valueOf("231"),
        resolver.convert(TypeLiteral.get(ValueOf.class), data("231")));
  }

  @Test
  public void shouldConvertWithFromStringStaticMethod() throws Exception {
    String uuid = UUID.randomUUID().toString();
    ParserExecutor resolver = newParser();
    assertEquals(UUID.fromString(uuid), resolver.convert(TypeLiteral.get(UUID.class), data(uuid)));
  }

  @Test
  public void shouldConvertWithForNameStaticMethod() throws Exception {
    String cs = "UTF-8";
    ParserExecutor resolver = newParser();
    assertEquals(Charset.forName(cs), resolver.convert(TypeLiteral.get(Charset.class), data(cs)));
  }

  @Test
  public void shouldConvertFromLocale() throws Exception {
    String locale = "es-ar";
    ParserExecutor resolver = newParser();
    assertEquals(LocaleUtils.toLocale(locale),
        resolver.convert(TypeLiteral.get(Locale.class), data(locale)));
  }

  @Test
  public void shouldConvertToInt() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(231, (int) resolver.convert(TypeLiteral.get(int.class), data("231")));

    assertEquals(421, (int) resolver.convert(TypeLiteral.get(Integer.class), data("421")));
  }

  @Test
  public void shouldConvertToBigDecimal() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(new BigDecimal(231.5),
        resolver.convert(TypeLiteral.get(BigDecimal.class), data("231.5")));
  }

  @Test
  public void shouldConvertOptionalListOfString() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals("Optional[[a, b, c]]", resolver.convert(
        TypeLiteral.get(Types.newParameterizedType(Optional.class, Types.listOf(String.class))),
        data("a", "b", "c"))
        .toString());
  }

  @Test
  public void shouldConvertToBigInteger() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(new BigInteger("231411"),
        resolver.convert(TypeLiteral.get(BigInteger.class), data("231411")));
  }

  @Test
  public void shouldConvertToString() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals("231", resolver.convert(TypeLiteral.get(String.class), data("231")));
  }

  @Test
  public void shouldConvertToChar() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals('c', (char) resolver.convert(TypeLiteral.get(char.class), data("c")));
    assertEquals('c', (char) resolver.convert(TypeLiteral.get(Character.class), data("c")));
  }

  @Test
  public void shouldConvertToLong() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(231L, (long) resolver.convert(TypeLiteral.get(long.class), data("231")));

    assertEquals(421L, (long) resolver.convert(TypeLiteral.get(Long.class), data("421")));
  }

  @Test
  public void shouldConvertToFloat() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(231.5f, (float) resolver.convert(TypeLiteral.get(float.class), data("231.5")), 0f);

    assertEquals(421.3f, (float) resolver.convert(TypeLiteral.get(Float.class), data("421.3")), 0f);
  }

  @Test
  public void shouldConvertToDouble() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(231.5d, (double) resolver.convert(TypeLiteral.get(double.class), data("231.5")),
        0f);

    assertEquals(421.3d, (double) resolver.convert(TypeLiteral.get(Double.class), data("421.3")),
        0f);
  }

  @Test
  public void shouldConvertToShort() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals((short) 231, (short) resolver.convert(TypeLiteral.get(short.class), data("231")));

    assertEquals((short) 421, (short) resolver.convert(TypeLiteral.get(Short.class), data("421")));
  }

  @Test
  public void shouldConvertToByte() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals((byte) 23, (byte) resolver.convert(TypeLiteral.get(byte.class), data("23")));

    assertEquals((byte) 42, (byte) resolver.convert(TypeLiteral.get(Byte.class), data("42")));
  }

  @Test
  public void shouldConvertToListOfBytes() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(Lists.newArrayList((byte) 23, (byte) 45),
        resolver.convert(TypeLiteral.get(Types.listOf(Byte.class)), data("23", "45")));
  }

  @Test
  public void shouldConvertToSetOfBytes() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(Sets.newHashSet((byte) 23, (byte) 45),
        resolver.convert(TypeLiteral.get(Types.setOf(Byte.class)), data("23", "45", "23")));
  }

  @Test
  public void shouldConvertToOptionalByte() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(Optional.of((byte) 23),
        resolver.convert(TypeLiteral.get(Types.newParameterizedType(Optional.class, Byte.class)),
            data("23")));

    assertEquals(Optional.empty(),
        resolver.convert(TypeLiteral.get(Types.newParameterizedType(Optional.class, Byte.class)),
            data()));
  }

  @Test
  public void shouldConvertToEnum() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(Letter.A, resolver.convert(TypeLiteral.get(Letter.class), data("A")));
  }

  @Test
  public void shouldConvertToBoolean() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(true, resolver.convert(TypeLiteral.get(boolean.class), data("true")));
    assertEquals(false, resolver.convert(TypeLiteral.get(boolean.class), data("false")));

    assertEquals(true, resolver.convert(TypeLiteral.get(Boolean.class), data("true")));
    assertEquals(false, resolver.convert(TypeLiteral.get(Boolean.class), data("false")));
  }

  @Test
  public void shouldConvertToSortedSet() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals("[a, b, c]", resolver.convert(
        TypeLiteral.get(Types.newParameterizedType(SortedSet.class, String.class)),
        data("c", "a", "b")).toString());
  }

  @Test
  public void shouldConvertToListOfBoolean() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(Lists.newArrayList(true, false),
        resolver.convert(TypeLiteral.get(Types.listOf(Boolean.class)),
            data("true", "false")));

    assertEquals(Lists.newArrayList(false, false),
        resolver.convert(TypeLiteral.get(Types.listOf(Boolean.class)),
            data("false", "false")));
  }

  @Test
  public void shouldConvertToSetOfBoolean() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(Sets.newHashSet(true, false),
        resolver.convert(TypeLiteral.get(Types.setOf(Boolean.class)),
            data("true", "false")));

    assertEquals(Sets.newHashSet(false),
        resolver.convert(TypeLiteral.get(Types.setOf(Boolean.class)),
            data("false", "false")));
  }

  @Test
  public void shouldConvertToOptionalBoolean() throws Exception {
    ParserExecutor resolver = newParser();

    assertEquals(Optional.of(true),
        resolver.convert(
            TypeLiteral.get(Types.newParameterizedType(Optional.class, Boolean.class)),
            data("true" )));

    assertEquals(Optional.of(false),
        resolver.convert(
            TypeLiteral.get(Types.newParameterizedType(Optional.class, Boolean.class)),
            data("false" )));

    assertEquals(Optional.empty(),
        resolver.convert(
            TypeLiteral.get(Types.newParameterizedType(Optional.class, Boolean.class)),
            data()));

  }

  @Test(expected = Err.class)
  public void shouldFailOnNoMatch() throws Exception {
    ParserExecutor resolver = new ParserExecutor(createMock(Injector.class),
        Sets.newHashSet((Parser) (toType, ctx) -> ctx.next()));

    resolver.convert(TypeLiteral.get(Types.newParameterizedType(Optional.class, Boolean.class)),
        "true");

  }

  @Test
  public void shouldConvertToMediaType() throws Exception {
    ParserExecutor resolver = newParser();
    assertEquals(Lists.newArrayList(MediaType.valueOf("text/html")),
        resolver.convert(TypeLiteral.get(Types.listOf(MediaType.class)),
            data("text/html")));
  }

  private ParserExecutor newParser() {
    return new ParserExecutor(createMock(Injector.class),
        Sets.newLinkedHashSet(
            Arrays.asList(
                BuiltinParser.Basic,
                BuiltinParser.Collection,
                BuiltinParser.Optional,
                BuiltinParser.Enum,
                new DateParser("dd/MM/yyyy"),
                new LocalDateParser(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                new LocaleParser(),
                new StaticMethodParser("valueOf"),
                new StringConstructorParser(),
                new StaticMethodParser("fromString"),
                new StaticMethodParser("forName")
                )));
  }
}
