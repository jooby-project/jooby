package io.jooby;

import io.jooby.internal.HashValue;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValueResolveTest {

  @Test
  public void resolveOne() {
    Value value = Value.value(null, "foo", "bar");
    assertEquals("bar", value.resolve("${foo}"));
    assertEquals("- bar", value.resolve("- ${foo}"));
    assertEquals("bar-", value.resolve("${foo}-"));
    assertEquals("-", value.resolve("-"));
    assertEquals("", value.resolve(""));
  }

  @Test
  public void resolveComplexWithoutRoot() {
    Value value = new HashValue(null, null)
        .put("foo.bar", "baz");
    assertEquals("baz", value.resolve("${foo.bar}"));
    assertEquals("-baz-", value.resolve("-${foo.bar}-"));
  }

  @Test
  public void resolveComplex() {
    Value value = new HashValue(null, null)
        .put("firstname", "Pedro")
        .put("lastname", "Picapiedra");
    assertEquals("Hi Pedro Picapiedra!", value.resolve("Hi ${firstname} ${lastname}!"));
  }

  @Test
  public void resolveComplexWithRoot() {
    Value value = new HashValue(null, "foo")
        .put("bar", "baz");
    assertEquals("baz", value.resolve("${foo.bar}"));
    assertEquals("-baz-", value.resolve("-${foo.bar}-"));
  }

  @Test
  public void resolveMissing() {
    try {
      Value value = Value.value(null, "x", "y");
      assertEquals("bar", value.resolve("${foo}"));
    } catch (NoSuchElementException x) {
      assertEquals("Missing ${foo} at 1:1", x.getMessage());
    }

    Value value = Value.value(null, "x", "y");
    assertEquals("${foo}", value.resolve("${foo}", true));
  }
}
