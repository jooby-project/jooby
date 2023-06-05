/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import io.jooby.internal.UrlParser;
import io.jooby.internal.ValueConverterHelper;
import jakarta.inject.Inject;

public class Issue2525 {

  public class VC2525 implements ValueConverter {
    @Override
    public boolean supports(@NotNull Class type) {
      return type == MyID2525.class;
    }

    @Override
    public Object convert(@NotNull Value value, @NotNull Class type) {
      return new MyID2525(value.value());
    }
  }

  public class MyID2525 {

    private String value;

    @Inject
    public MyID2525(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "MyID:" + value;
    }
  }

  public static class Foo2525 {
    public Integer a;
    public Integer b;

    public Foo2525(Integer a, Integer b) {
      Objects.requireNonNull(a);
      Objects.requireNonNull(b);

      this.a = a;
      this.b = b;
    }

    @Override
    public String toString() {
      return "{" + "a=" + a + ", b=" + b + '}';
    }
  }

  public static class JavaBeanParam {
    private String foo;

    public String getFoo() {
      return foo;
    }

    public void setFoo(String foo) {
      this.foo = foo;
    }

    @Override
    public String toString() {
      return foo;
    }
  }

  @Test
  public void shouldBeMoreClever() {
    queryString(
        "foo=1234",
        queryString -> {
          assertEquals("[1234]", queryString.toList(JavaBeanParam.class).toString());
        });
    queryString(
        "something=else&foo[0][a]=10&foo[0][b]=20&[0][a]=30&[0][b]=40",
        queryString -> {
          assertEquals("[{a=30, b=40}]", queryString.toList(Foo2525.class).toString());
          assertEquals("[{a=10, b=20}]", queryString.get("foo").toList(Foo2525.class).toString());
        });
    queryString(
        "id=1234",
        queryString -> {
          assertEquals("MyID:1234", queryString.get("id").to(MyID2525.class).toString());
        },
        new VC2525());
    queryString(
        "a=1&b=2&foo.a=3&foo.b=4",
        queryString -> {
          assertEquals("{a=1, b=2}", queryString.to(Foo2525.class).toString());
          assertEquals("{a=3, b=4}", queryString.get("foo").to(Foo2525.class).toString());
        });
    queryString(
        "something=else&foo[0][a]=10&foo[0][b]=20",
        queryString -> {
          assertEquals("[]", queryString.toList(Foo2525.class).toString());
          assertEquals("[{a=10, b=20}]", queryString.get("foo").toList(Foo2525.class).toString());
        });
    queryString(
        "something=else",
        queryString -> {
          assertEquals("[]", queryString.toList(Foo2525.class).toString());
          assertEquals("[]", queryString.get("foo").toList(Foo2525.class).toString());
        });
    queryString(
        "",
        queryString -> {
          assertEquals("[]", queryString.toList(Foo2525.class).toString());
          assertEquals("[]", queryString.get("foo").toList(Foo2525.class).toString());
        });
  }

  private void queryString(
      String queryString, Consumer<QueryString> consumer, ValueConverter... converter) {
    consumer.accept(
        UrlParser.queryString(ValueConverterHelper.testContext(converter), queryString));
  }
}
