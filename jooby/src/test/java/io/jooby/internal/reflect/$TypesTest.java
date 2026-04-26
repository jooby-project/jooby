package io.jooby.internal.reflect;

import io.jooby.Reified;
import org.junit.jupiter.api.Test;

import java.lang.reflect.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class $TypesTest {

  @Test
  public void testNewParameterizedType() {
    ParameterizedType listStr = $Types.newParameterizedTypeWithOwner(null, List.class, String.class);
    assertEquals(List.class, listStr.getRawType());
    assertEquals(String.class, listStr.getActualTypeArguments()[0]);
    assertNull(listStr.getOwnerType());
    assertEquals("java.util.List<java.lang.String>", listStr.toString());

    // Test with owner
    ParameterizedType entry = $Types.newParameterizedTypeWithOwner(Map.class, Map.Entry.class, String.class, Integer.class);
    assertEquals(Map.class, entry.getOwnerType());

    // Error case: Null type arg
    assertThrows(NullPointerException.class, () ->
        $Types.newParameterizedTypeWithOwner(null, List.class, (Type) null));
  }

  @Test
  public void testArrayOf() {
    GenericArrayType arrayType = $Types.arrayOf(String.class);
    assertEquals(String.class, arrayType.getGenericComponentType());
    assertEquals("java.lang.String[]", arrayType.toString());
  }

  @Test
  public void testWildcards() {
    WildcardType extendsStr = $Types.subtypeOf(String.class);
    assertEquals(String.class, extendsStr.getUpperBounds()[0]);
    assertEquals("? extends java.lang.String", extendsStr.toString());

    WildcardType superStr = $Types.supertypeOf(String.class);
    assertEquals(String.class, superStr.getLowerBounds()[0]);
    assertEquals("? super java.lang.String", superStr.toString());

    // Subtype of Object is just "?"
    assertEquals("?", $Types.subtypeOf(Object.class).toString());
  }

  @Test
  public void testGetRawType() {
    assertEquals(String.class, $Types.getRawType(String.class));

    Type listType = new Reified<List<String>>() {}.getType();
    assertEquals(List.class, $Types.getRawType(listType));

    Type arrayType = $Types.arrayOf(String.class);
    assertEquals(String[].class, $Types.getRawType(arrayType));

    WildcardType wildcard = $Types.subtypeOf(Number.class);
    assertEquals(Number.class, $Types.getRawType(wildcard));

    // Unsupported type
    assertThrows(IllegalArgumentException.class, () -> $Types.getRawType(null));
  }

  @Test
  public void testEquals() {
    Type t1 = new Reified<List<String>>() {}.getType();
    Type t2 = $Types.newParameterizedTypeWithOwner(null, List.class, String.class);
    Type t3 = new Reified<List<Integer>>() {}.getType();

    assertTrue($Types.equals(t1, t2));
    assertFalse($Types.equals(t1, t3));
    assertFalse($Types.equals(t1, List.class));

    // Arrays
    assertTrue($Types.equals($Types.arrayOf(String.class), $Types.arrayOf(String.class)));
    assertFalse($Types.equals($Types.arrayOf(String.class), $Types.arrayOf(Integer.class)));

    // Wildcards
    assertTrue($Types.equals($Types.subtypeOf(String.class), $Types.subtypeOf(String.class)));
  }

  @Test
  public void testCanonicalize() {
    Type t = new Reified<List<String>>() {}.getType();
    Type canon = $Types.canonicalize(t);
    assertEquals(t, canon);
    assertNotSame(t, canon); // Implementation class vs anonymous internal
  }

  @Test
  public void testResolve() {
    // Resolve List<T> against ArrayList<String>
    Class<?> arrayList = ArrayList.class;
    Type superType = $Types.getGenericSupertype(arrayList, arrayList, List.class);

    // Resolve T in List<T> context of ArrayList<String>
    Type resolved = $Types.resolve(new Reified<ArrayList<String>>(){}.getType(), ArrayList.class, superType);
    assertEquals(new Reified<List<String>>(){}.getType(), resolved);
  }

  @Test
  public void testResolveTypeVariableRecursive() {
    // Test for infinite recursion guard
    class Node<T extends Node<T>> {}
    TypeVariable<?> tv = Node.class.getTypeParameters()[0];
    Type resolved = $Types.resolve(Node.class, Node.class, tv);
    assertEquals(tv, resolved);
  }

  @Test
  public void testParameterizedType0() {
    assertEquals(String.class, $Types.parameterizedType0(new Reified<List<String>>(){}.getType()));
    assertEquals(Integer.class, $Types.parameterizedType0($Types.subtypeOf(Integer.class)));
    assertEquals(String.class, $Types.parameterizedType0(String.class)); // Fallback
  }

  @Test
  public void testHashCode() {
    Type t1 = $Types.newParameterizedTypeWithOwner(null, List.class, String.class);
    Type t2 = $Types.newParameterizedTypeWithOwner(null, List.class, String.class);
    assertEquals(t1.hashCode(), t2.hashCode());

    Type g1 = $Types.arrayOf(String.class);
    Type g2 = $Types.arrayOf(String.class);
    assertEquals(g1.hashCode(), g2.hashCode());
  }

  @Test
  public void testCheckNotPrimitive() {
    $Types.checkNotPrimitive(String.class);
    assertThrows(IllegalArgumentException.class, () -> $Types.checkNotPrimitive(int.class));
  }

  @Test
  public void testGetGenericSupertypeWithInterfaces() {
    // Test finding interface in hierarchy
    Type t = $Types.getGenericSupertype(Properties.class, Properties.class, Map.class);
    assertTrue(t instanceof ParameterizedType);
    assertEquals(Map.class, ((ParameterizedType)t).getRawType());
  }
}
