package io.jooby.internal.asm;

import io.jooby.Reified;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TypeParserTest {

  class X {
  }

  class Y {
  }

  class User {
  }

  class BaseUser extends User {
  }

  class BasicUser extends User {
  }

  class SuperUser extends BasicUser {
  }

  @Test
  public void primitives() {
    assertEquals(int.class, parse(int.class));
    assertEquals(void.class, parse(void.class));
    assertEquals(long.class, parse(long.class));
    assertEquals(byte.class, parse(byte.class));
    assertEquals(short.class, parse(short.class));
    assertEquals(boolean.class, parse(boolean.class));
    assertEquals(float.class, parse(float.class));
    assertEquals(double.class, parse(double.class));
  }

  @Test
  public void wrapper() {
    assertEquals(Integer.class, parse(Integer.class));
  }

  @Test
  public void array() {
    assertEquals(String[].class, parse(String[].class));
    assertEquals(long[].class, parse(long[].class));
    assertEquals(Double[].class, parse(Double[].class));
    assertEquals(TypeParserTest[][][].class, parse(TypeParserTest[][][].class));
  }

  @Test
  public void string() {
    assertEquals(String.class, parse(String.class));
  }

  @Test
  public void single() {
    assertEquals(TypeParserTest.class, parse(TypeParserTest.class));
  }

  @Test
  public void commonAncestor() {
    assertEquals(List.class, commonAncestor(ArrayList.class, LinkedList.class));

    assertEquals(List.class, commonAncestor(ArrayList.class, List.class));

    assertEquals(List.class, commonAncestor(List.class, LinkedList.class));

    assertEquals(BasicUser.class, commonAncestor(SuperUser.class, BasicUser.class));

    assertEquals(User.class, commonAncestor(BasicUser.class, BaseUser.class));

    assertEquals(List.class, commonAncestor(List.class, List.class));

    assertEquals(BasicUser.class, commonAncestor(BasicUser.class, BasicUser.class));

    assertEquals(User.class, commonAncestor(SuperUser.class, BaseUser.class));

    assertEquals(Object.class, commonAncestor(X.class, Y.class));
  }

  @Test
  public void parameterized() {
    assertEquals(Reified.list(String.class).getType(),
        parse("Ljava/util/List<Ljava/lang/String;>;"));

    assertEquals(
        Reified.getParameterized(List.class, Reified.list(String.class).getType()).getType(),
        parse("Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;"));

    assertEquals(Reified.map(String.class, Reified.list(String.class).getType()).getType(),
        parse("Ljava/util/Map<Ljava/lang/String;Ljava/util/List<Ljava/lang/String;>;>;"));
  }

  private java.lang.reflect.Type parse(Class type) {
    return parse(Type.getType(type).getDescriptor());
  }

  private java.lang.reflect.Type parse(String descriptor) {
    TypeParser parser = new TypeParser(getClass().getClassLoader());
    java.lang.reflect.Type result = parser.parseTypeDescriptor(descriptor);
    return result;
  }

  private java.lang.reflect.Type commonAncestor(Class... types) {
    TypeParser parser = new TypeParser(getClass().getClassLoader());
    return parser.commonAncestor(Stream.of(types).collect(Collectors.toSet()));
  }
}
