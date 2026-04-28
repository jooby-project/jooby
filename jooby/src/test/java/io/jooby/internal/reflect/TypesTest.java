/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.junit.jupiter.api.Test;

import io.jooby.Reified;

public class TypesTest {

  @Test
  public void testNewParameterizedType() {
    ParameterizedType listStr =
        $Types.newParameterizedTypeWithOwner(null, List.class, String.class);
    assertEquals(List.class, listStr.getRawType());
    assertEquals(String.class, listStr.getActualTypeArguments()[0]);
    assertNull(listStr.getOwnerType());
    assertEquals("java.util.List<java.lang.String>", listStr.toString());

    // Test with owner
    ParameterizedType entry =
        $Types.newParameterizedTypeWithOwner(
            Map.class, Map.Entry.class, String.class, Integer.class);
    assertEquals(Map.class, entry.getOwnerType());

    // Error case: Null type arg
    assertThrows(
        NullPointerException.class,
        () -> $Types.newParameterizedTypeWithOwner(null, List.class, (Type) null));

    // Error case: Missing owner type for non-static inner class
    class Inner {}
    assertThrows(
        IllegalArgumentException.class,
        () -> $Types.newParameterizedTypeWithOwner(null, Inner.class));
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

    // Identity case for existing wildcards
    assertEquals(extendsStr, $Types.subtypeOf(extendsStr));
    assertEquals(superStr, $Types.supertypeOf(superStr));
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

    class TypeVar<T> {
      T t;
    }
    Type tv = TypeVar.class.getTypeParameters()[0];
    assertEquals(Object.class, $Types.getRawType(tv));

    // Unsupported type branch
    assertThrows(IllegalArgumentException.class, () -> $Types.getRawType(null));
  }

  @Test
  public void testEqualsMissingBranches() {
    // 1. Coverage for WildcardType (a is Wildcard, b is not)
    WildcardType wildcard = $Types.subtypeOf(String.class);
    assertFalse($Types.equals(wildcard, String.class));

    // 2. Coverage for TypeVariable (a is TypeVariable, b is not)
    class Var<T> {}
    TypeVariable<?> tv = Var.class.getTypeParameters()[0];
    assertFalse($Types.equals(tv, String.class));

    // 3. Coverage for Unsupported Type (the final 'else { return false; }' branch)
    // We create a custom Type implementation that $Types doesn't recognize
    Type customType =
        new Type() {
          @Override
          public String getTypeName() {
            return "CustomType";
          }
        };
    assertFalse($Types.equals(customType, String.class));
  }

  @Test
  public void testGetGenericSupertypeDeepHierarchy() {
    // 1. Recursive Interface Search: covers 'else if (toResolve.isAssignableFrom(interfaces[i]))'
    // This finds Iterable<String> starting from ArrayList
    Type iterable = $Types.getGenericSupertype(ArrayList.class, ArrayList.class, Iterable.class);
    assertTrue(iterable instanceof ParameterizedType);
    assertEquals(Iterable.class, ((ParameterizedType) iterable).getRawType());

    // 2. Deep Class Hierarchy: covers the 'while' loop and 'rawType = rawSupertype'
    // This finds AbstractCollection from ArrayList (skipping AbstractList)
    Type collection =
        $Types.getGenericSupertype(ArrayList.class, ArrayList.class, AbstractCollection.class);
    assertTrue(collection instanceof ParameterizedType);
    assertEquals(AbstractCollection.class, ((ParameterizedType) collection).getRawType());

    // 3. Fallback: covers 'return toResolve' at the end of the method
    // This happens when the toResolve type is not in the hierarchy at all
    Type unrelated = $Types.getGenericSupertype(String.class, String.class, List.class);
    assertEquals(List.class, unrelated);

    // 4. Exact Superclass Match: covers 'if (rawSupertype == toResolve)'
    // This finds AbstractList directly from ArrayList
    Type abstractList =
        $Types.getGenericSupertype(ArrayList.class, ArrayList.class, AbstractList.class);
    assertEquals(ArrayList.class.getGenericSuperclass(), abstractList);
  }

  @Test
  public void testResolveAdditionalBranches() {
    // 1. Infinite Recursion Guard: covers 'return toResolve' when visitedTypeVariables contains the
    // variable
    // This happens if the variable is already being resolved in the current stack
    class Node<T extends Node<T>> {}
    TypeVariable<?> tv = Node.class.getTypeParameters()[0];
    Set<TypeVariable> visited = new HashSet<>();
    visited.add(tv);
    // We call the private resolve directly via the public one by setting up a loop
    Type resolvedRecursion = $Types.resolve(Node.class, Node.class, tv);
    assertEquals(tv, resolvedRecursion);

    // 2. Standard Java Array resolution: covers 'toResolve instanceof Class && isArray()'
    // and 'return componentType == newComponentType ? original : arrayOf(newComponentType)'
    Type stringArray = String[].class;
    Type resolvedArray = $Types.resolve(String.class, String.class, stringArray);
    assertSame(stringArray, resolvedArray); // componentType == newComponentType branch

    // 3. Parameterized Type - No Change: covers 'return changed ? ... : original'
    Type listString = new Reified<List<String>>() {}.getType();
    Type resolvedList = $Types.resolve(String.class, String.class, listString);
    assertSame(listString, resolvedList); // changed == false branch

    // 4. Wildcard with Lower Bound: covers 'originalLowerBound.length == 1'
    Type wildcardLower = $Types.supertypeOf(String.class);
    Type resolvedWildLower = $Types.resolve(String.class, String.class, wildcardLower);
    assertEquals(wildcardLower, resolvedWildLower);
  }

  @Test
  public void testEquals() {
    Type t1 = new Reified<List<String>>() {}.getType();
    Type t2 = $Types.newParameterizedTypeWithOwner(null, List.class, String.class);
    Type t3 = new Reified<List<Integer>>() {}.getType();

    assertTrue($Types.equals(t1, t2));
    assertFalse($Types.equals(t1, t3));
    assertFalse($Types.equals(t1, List.class));
    assertFalse($Types.equals(t1, null));

    // Arrays
    assertTrue($Types.equals($Types.arrayOf(String.class), $Types.arrayOf(String.class)));
    assertFalse($Types.equals($Types.arrayOf(String.class), $Types.arrayOf(Integer.class)));
    assertFalse($Types.equals($Types.arrayOf(String.class), String.class));

    // Wildcards
    assertTrue($Types.equals($Types.subtypeOf(String.class), $Types.subtypeOf(String.class)));
    assertFalse($Types.equals($Types.subtypeOf(String.class), $Types.supertypeOf(String.class)));

    // TypeVariables
    class Var<T, U> {}
    TypeVariable<?> v1 = Var.class.getTypeParameters()[0];
    TypeVariable<?> v1b = Var.class.getTypeParameters()[0];
    TypeVariable<?> v2 = Var.class.getTypeParameters()[1];
    assertTrue($Types.equals(v1, v1b));
    assertFalse($Types.equals(v1, v2));
  }

  @Test
  public void testCanonicalize() {
    Type t = new Reified<List<String>>() {}.getType();
    Type canon = $Types.canonicalize(t);

    // Use $Types.equals to check structural equality
    assertTrue($Types.equals(t, canon));
    assertNotSame(t, canon);

    // Test Array canonicalization
    Type arrayType = String[].class;
    Type canonArray = $Types.canonicalize(arrayType);

    assertTrue($Types.equals($Types.canonicalize(arrayType), canonArray));
  }

  @Test
  public void testResolve() {
    // Resolve List<T> against ArrayList<String>
    Class<?> arrayList = ArrayList.class;
    Type superType = $Types.getGenericSupertype(arrayList, arrayList, List.class);

    // Resolve T in List<T> context of ArrayList<String>
    Type resolved =
        $Types.resolve(new Reified<ArrayList<String>>() {}.getType(), ArrayList.class, superType);
    assertEquals(new Reified<List<String>>() {}.getType(), resolved);

    Type arrayTv = ArrayVar.class.getDeclaredFields()[0].getGenericType();
    Type resolvedArray =
        $Types.resolve(new Reified<ArrayVar<String>>() {}.getType(), ArrayVar.class, arrayTv);
    assertTrue(resolvedArray instanceof GenericArrayType);
  }

  // Define this as a static nested class
  static class ArrayVar<T> {
    T[] array;
  }

  @Test
  public void testResolveWildcards() {
    Type wildType = Wild.class.getDeclaredFields()[0].getGenericType();
    Type resolved = $Types.resolve(new Reified<Wild<String>>() {}.getType(), Wild.class, wildType);
    assertEquals(new Reified<List<? extends String>>() {}.getType(), resolved);
  }

  // Define this as a static nested class
  static class Wild<T> {
    List<? extends T> list;
  }

  @Test
  public void testResolveTypeVariableRecursive() {
    class Node<T extends Node<T>> {}
    TypeVariable<?> tv = Node.class.getTypeParameters()[0];
    Type resolved = $Types.resolve(Node.class, Node.class, tv);
    assertEquals(tv, resolved);
  }

  @Test
  public void testParameterizedType0() {
    assertEquals(String.class, $Types.parameterizedType0(new Reified<List<String>>() {}.getType()));
    assertEquals(Integer.class, $Types.parameterizedType0($Types.subtypeOf(Integer.class)));
    assertEquals(String.class, $Types.parameterizedType0(String.class));
    assertEquals(String.class, $Types.parameterizedType0(null)); // Fallback branch
  }

  @Test
  public void testHashCode() {
    Type t1 = $Types.newParameterizedTypeWithOwner(null, List.class, String.class);
    Type t2 = $Types.newParameterizedTypeWithOwner(null, List.class, String.class);
    assertEquals(t1.hashCode(), t2.hashCode());

    Type g1 = $Types.arrayOf(String.class);
    assertEquals(g1.hashCode(), $Types.arrayOf(String.class).hashCode());
  }

  @Test
  public void testCheckNotPrimitive() {
    $Types.checkNotPrimitive(String.class);
    assertThrows(IllegalArgumentException.class, () -> $Types.checkNotPrimitive(int.class));
  }

  @Test
  public void testGetGenericSupertypeWithInterfaces() {
    Type t = $Types.getGenericSupertype(Properties.class, Properties.class, Map.class);
    assertTrue(t instanceof ParameterizedType);
    assertEquals(Map.class, ((ParameterizedType) t).getRawType());
  }
}
