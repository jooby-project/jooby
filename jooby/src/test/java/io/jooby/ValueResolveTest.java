package io.jooby;

import io.jooby.internal.HashValue;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValueResolveTest {

  @Test
  public void resolveOne() {
    ValueNode value = Value.value(null, "foo", "bar");
    assertEquals("bar", value.resolve("${foo}"));
    assertEquals("- bar", value.resolve("- ${foo}"));
    assertEquals("bar-", value.resolve("${foo}-"));
    assertEquals("-", value.resolve("-"));
    assertEquals("", value.resolve(""));
  }

  @Test
  public void resolveComplexWithoutRoot() {
    HashValue value = new HashValue(null, null);
    value.put("foo.bar", "baz");
    assertEquals("baz", value.resolve("${foo.bar}"));
    assertEquals("-baz-", value.resolve("-${foo.bar}-"));
  }

  @Test
  public void resolveComplex() {
    HashValue value = new HashValue(null, null);
    value.put("firstname", "Pedro");
    value.put("lastname", "Picapiedra");
    assertEquals("Hi Pedro Picapiedra!", value.resolve("Hi ${firstname} ${lastname}!"));
  }

  @Test
  public void resolveComplexWithRoot() {
    HashValue value = new HashValue(null, "foo");
    value.put("bar", "baz");
    assertEquals("baz", value.resolve("${foo.bar}"));
    assertEquals("-baz-", value.resolve("-${foo.bar}-"));
  }

  @Test
  public void resolveMissing() {
    try {
      ValueNode value = Value.value(null, "x", "y");
      assertEquals("bar", value.resolve("${foo}"));
    } catch (NoSuchElementException x) {
      assertEquals("Missing ${foo} at 1:1", x.getMessage());
    }

    ValueNode value = Value.value(null, "x", "y");
    assertEquals("${foo}", value.resolve("${foo}", true));
  }
}
