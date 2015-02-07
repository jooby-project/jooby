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
import org.jooby.ParamConverter;
import org.jooby.Request;
import org.jooby.internal.reqparam.CollectionParamConverter;
import org.jooby.internal.reqparam.CommonTypesParamConverter;
import org.jooby.internal.reqparam.DateParamConverter;
import org.jooby.internal.reqparam.EnumParamConverter;
import org.jooby.internal.reqparam.LocalDateParamConverter;
import org.jooby.internal.reqparam.LocaleParamConverter;
import org.jooby.internal.reqparam.OptionalParamConverter;
import org.jooby.internal.reqparam.RootParamConverter;
import org.jooby.internal.reqparam.StaticMethodParamConverter;
import org.jooby.internal.reqparam.StringConstructorParamConverter;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
    RootParamConverter resolver = newConverter();
    List<String> value = resolver.convert(TypeLiteral.get(Types.listOf(String.class)), null);
    assertNotNull(value);
    assertTrue(value.isEmpty());
  }

  @Test
  public void shouldConvertToDateFromString() throws Exception {
    RootParamConverter resolver = newConverter();
    Date date = resolver.convert(TypeLiteral.get(Date.class), new Object[]{"22/02/2014" });
    assertNotNull(date);
    assertEquals(1393038000000L, date.getTime());
  }

  @Test
  public void shouldConvertToDateFromLong() throws Exception {
    RootParamConverter resolver = newConverter();
    Date date = resolver.convert(TypeLiteral.get(Date.class), "1393038000000");
    assertNotNull(date);
    assertEquals("22/02/2014", new SimpleDateFormat("dd/MM/yyyy").format(date));
  }

  @Test
  public void shouldConvertToLocalDateFromString() throws Exception {
    RootParamConverter resolver = newConverter();
    LocalDate date = resolver.convert(TypeLiteral.get(LocalDate.class), "22/02/2014");
    assertNotNull(date);
    assertEquals(22, date.getDayOfMonth());
    assertEquals(2, date.getMonthValue());
    assertEquals(2014, date.getYear());
  }

  @Test
  public void shouldConvertToLocalDateFromLong() throws Exception {
    RootParamConverter resolver = newConverter();
    LocalDate date = resolver.convert(TypeLiteral.get(LocalDate.class), "1393038000000");
    assertNotNull(date);
    assertEquals(22, date.getDayOfMonth());
    assertEquals(2, date.getMonthValue());
    assertEquals(2014, date.getYear());
  }

  @Test
  public void shouldConvertBeanWithStringConstructor() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(new StringBean("231"), resolver.convert(TypeLiteral.get(StringBean.class), "231"));
  }

  @Test
  public void shouldConvertListOfBeanWithStringConstructor() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(Lists.newArrayList(new StringBean("231")),
        resolver.convert(TypeLiteral.get(Types.listOf(StringBean.class)), "231"));
  }

  @Test
  public void shouldConvertWithValueOfStaticMethod() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(ValueOf.valueOf("231"), resolver.convert(TypeLiteral.get(ValueOf.class), "231"));
  }

  @Test
  public void shouldConvertWithFromStringStaticMethod() throws Exception {
    String uuid = UUID.randomUUID().toString();
    RootParamConverter resolver = newConverter();
    assertEquals(UUID.fromString(uuid), resolver.convert(TypeLiteral.get(UUID.class), uuid));
  }

  @Test
  public void shouldConvertWithForNameStaticMethod() throws Exception {
    String cs = "UTF-8";
    RootParamConverter resolver = newConverter();
    assertEquals(Charset.forName(cs), resolver.convert(TypeLiteral.get(Charset.class), cs));
  }

  @Test
  public void shouldConvertFromLocale() throws Exception {
    String locale = "es-ar";
    RootParamConverter resolver = newConverter();
    assertEquals(LocaleUtils.toLocale(locale),
        resolver.convert(TypeLiteral.get(Locale.class), locale));
  }

  @Test
  public void shouldConvertToInt() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(231, (int) resolver.convert(TypeLiteral.get(int.class), "231"));

    assertEquals(421, (int) resolver.convert(TypeLiteral.get(Integer.class), "421"));
  }

  @Test
  public void shouldConvertToBigDecimal() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(new BigDecimal(231.5),
        resolver.convert(TypeLiteral.get(BigDecimal.class), "231.5"));
  }

  @Test
  public void shouldConvertOptionalListOfString() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals("Optional[[a, b, c]]", resolver.convert(
        TypeLiteral.get(Types.newParameterizedType(Optional.class, Types.listOf(String.class))),
        new Object[]{"a", "b", "c" })
        .toString());
  }

  @Test
  public void shouldConvertToBigInteger() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(new BigInteger("231411"),
        resolver.convert(TypeLiteral.get(BigInteger.class), "231411"));
  }

  @Test
  public void shouldConvertToString() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals("231", resolver.convert(TypeLiteral.get(String.class), "231"));
  }

  @Test
  public void shouldConvertToChar() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals('c', (char) resolver.convert(TypeLiteral.get(char.class), "c"));
    assertEquals('c', (char) resolver.convert(TypeLiteral.get(Character.class), "c"));
  }

  @Test
  public void shouldConvertToLong() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(231L, (long) resolver.convert(TypeLiteral.get(long.class), "231"));

    assertEquals(421L, (long) resolver.convert(TypeLiteral.get(Long.class), "421"));
  }

  @Test
  public void shouldConvertToFloat() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(231.5f, (float) resolver.convert(TypeLiteral.get(float.class), "231.5"), 0f);

    assertEquals(421.3f, (float) resolver.convert(TypeLiteral.get(Float.class), "421.3"), 0f);
  }

  @Test
  public void shouldConvertToDouble() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(231.5d, (double) resolver.convert(TypeLiteral.get(double.class), "231.5"), 0f);

    assertEquals(421.3d, (double) resolver.convert(TypeLiteral.get(Double.class), "421.3"), 0f);
  }

  @Test
  public void shouldConvertToShort() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals((short) 231, (short) resolver.convert(TypeLiteral.get(short.class), "231"));

    assertEquals((short) 421, (short) resolver.convert(TypeLiteral.get(Short.class), "421"));
  }

  @Test
  public void shouldConvertToByte() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals((byte) 23, (byte) resolver.convert(TypeLiteral.get(byte.class), "23"));

    assertEquals((byte) 42, (byte) resolver.convert(TypeLiteral.get(Byte.class), "42"));
  }

  @Test
  public void shouldConvertToListOfBytes() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(Lists.newArrayList((byte) 23, (byte) 45),
        resolver.convert(TypeLiteral.get(Types.listOf(Byte.class)), new Object[]{"23", "45" }));
  }

  @Test
  public void shouldConvertToSetOfBytes() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(Sets.newHashSet((byte) 23, (byte) 45),
        resolver.convert(TypeLiteral.get(Types.setOf(Byte.class)), new Object[]{"23", "45", "23" }));
  }

  @Test
  public void shouldConvertToOptionalByte() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(Optional.of((byte) 23),
        resolver.convert(TypeLiteral.get(Types.newParameterizedType(Optional.class, Byte.class)),
            new Object[]{"23" }));

    assertEquals(Optional.empty(),
        resolver.convert(TypeLiteral.get(Types.newParameterizedType(Optional.class, Byte.class)),
            null));
  }

  @Test
  public void shouldConvertToEnum() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(Letter.A, resolver.convert(TypeLiteral.get(Letter.class), new Object[]{"A" }));
  }

  @Test
  public void shouldConvertToBoolean() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(true, resolver.convert(TypeLiteral.get(boolean.class), new Object[]{"true" }));
    assertEquals(false, resolver.convert(TypeLiteral.get(boolean.class), new Object[]{"false" }));

    assertEquals(true, resolver.convert(TypeLiteral.get(Boolean.class), new Object[]{"true" }));
    assertEquals(false, resolver.convert(TypeLiteral.get(Boolean.class), new Object[]{"false" }));
  }

  @Test
  public void shouldConvertToSortedSet() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals("[a, b, c]", resolver.convert(
        TypeLiteral.get(Types.newParameterizedType(SortedSet.class, String.class)),
        new Object[]{"c", "a", "b" }).toString());
  }

  @Test
  public void shouldConvertToListOfBoolean() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(Lists.newArrayList(true, false),
        resolver.convert(TypeLiteral.get(Types.listOf(Boolean.class)),
            new Object[]{"true", "false" }));

    assertEquals(Lists.newArrayList(false, false),
        resolver.convert(TypeLiteral.get(Types.listOf(Boolean.class)),
            new Object[]{"false", "false" }));
  }

  @Test
  public void shouldConvertToSetOfBoolean() throws Exception {
    RootParamConverter resolver = newConverter();
    assertEquals(Sets.newHashSet(true, false),
        resolver.convert(TypeLiteral.get(Types.setOf(Boolean.class)),
            new Object[]{"true", "false" }));

    assertEquals(Sets.newHashSet(false),
        resolver.convert(TypeLiteral.get(Types.setOf(Boolean.class)),
            new Object[]{"false", "false" }));
  }

  @Test
  public void shouldConvertToOptionalBoolean() throws Exception {
    RootParamConverter resolver = newConverter();

    assertEquals(Optional.of(true),
        resolver.convert(
            TypeLiteral.get(Types.newParameterizedType(Optional.class, Boolean.class)),
            new Object[]{"true" }));

    assertEquals(Optional.of(false),
        resolver.convert(
            TypeLiteral.get(Types.newParameterizedType(Optional.class, Boolean.class)),
            new Object[]{"false" }));

    assertEquals(Optional.empty(),
        resolver.convert(TypeLiteral.get(Types.newParameterizedType(Optional.class, Boolean.class)),
            null));

  }

  @Test(expected = Err.class)
  public void shouldFailOnNoMatch() throws Exception {
    RootParamConverter resolver = new RootParamConverter(() -> createMock(Request.class),
        Sets.newHashSet((ParamConverter) (toType, values, ctx) -> ctx.convert(toType, values)));

    resolver.convert(TypeLiteral.get(Types.newParameterizedType(Optional.class, Boolean.class)),
        new Object[]{"true" });

  }

  private RootParamConverter newConverter() {
    return new RootParamConverter(() -> createMock(Request.class),
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
