/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.asm;

import io.jooby.Reified;
import io.jooby.SneakyThrows;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class TypeParser {

  private final ClassLoader loader;

  public TypeParser(ClassLoader loader) {
    this.loader = loader;
  }

  public ClassLoader getClassLoader() {
    return loader;
  }

  public Type commonAncestor(Set<Type> types) {
    if (types.size() == 0) {
      return Object.class;
    }
    if (types.size() == 1) {
      return types.iterator().next();
    }
    Set<Class<?>> classes = determineCommonAncestor(types);
    Iterator<Class<?>> iterator = classes.iterator();
    return iterator.hasNext() ? iterator.next() : Object.class;
  }

  private void superclasses(Class clazz, Set<Class<?>> result) {
    if (clazz != null && clazz != Object.class) {
      if (result.add(clazz)) {
        for (Class k : clazz.getInterfaces()) {
          superclasses(k, result);
        }
        superclasses(clazz.getSuperclass(), result);
      }
    }
  }

  private Set<Class<?>> determineCommonAncestor(Set<Type> classes) {
    Iterator<Type> it = classes.iterator();
    // begin with set from first hierarchy
    Set<Class<?>> result = new LinkedHashSet<>();
    superclasses(Reified.rawType(it.next()), result);
    // remove non-superclasses of remaining
    while (it.hasNext()) {
      Class<?> c = Reified.rawType(it.next());
      Iterator<Class<?>> resultIt = result.iterator();
      while (resultIt.hasNext()) {
        Class<?> sup = resultIt.next();
        if (!sup.isAssignableFrom(c)) {
          resultIt.remove();
        }
      }
    }
    return result;
  }

  public Class resolve(final String name) {
    try {
      String classname = name.replace('/', '.');
      switch (classname) {
        case "boolean":
          return boolean.class;
        case "char":
          return char.class;
        case "byte":
          return byte.class;
        case "short":
          return short.class;
        case "int":
          return int.class;
        case "long":
          return long.class;
        case "float":
          return float.class;
        case "double":
          return double.class;
        case "java.lang.String":
          return String.class;
        case "java.lang.Object":
          return Object.class;
        default:
          Class<?> result;
          if (classname.startsWith("[")) {
            result = Class.forName(classname, false, loader);
          } else {
            result = loader.loadClass(classname);
          }
          if (List.class.isAssignableFrom(result)) {
            return List.class;
          }
          if (Set.class.isAssignableFrom(result)) {
            return Set.class;
          }
          if (Map.class.isAssignableFrom(result)) {
            return Map.class;
          }
          return result;
      }
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public Type parseTypeDescriptor(String descriptor) {
    Type type = simpleType(descriptor, 0, false);
    if (type != null) {
      return type;
    }
    StringBuilder name = new StringBuilder();
    LinkedList<LinkedList<Type>> stack = new LinkedList<>();
    stack.addLast(new LinkedList<>());
    int array = 0;
    for (int i = 0; i < descriptor.length(); ) {
      char ch = descriptor.charAt(i);
      if (ch == '[') {
        array += 1;
      } else if (ch == 'L') {
        if (name.length() > 0) {
          name.append(ch);
        }
      } else if (ch == '<') {
        newType(stack, name, array);
        stack.add(new LinkedList<>());
        array = 0;
      } else if (ch == ';') {
        if (name.length() > 0) {
          newType(stack, name, array);
          array = 0;
        }
      } else if (ch == '/') {
        name.append('.');
      } else if (ch == '>') {
        newParameterized(stack);
      } else {
        name.append(ch);
      }
      i += 1;
    }
    if (name.length() > 0) {
      newType(stack, name, array);
    }
    while (stack.size() > 1) {
      newParameterized(stack);
    }
    return stack.getFirst().getFirst();
  }

  private void newType(LinkedList<LinkedList<Type>> stack, StringBuilder name, int array) {
    Type it;
    if (array == 0) {
      it = resolve(name.toString());
    } else {
      StringBuilder dimension = new StringBuilder();
      IntStream.range(0, array).forEach(x -> dimension.append('['));
      it = resolve(dimension + "L" + name + ";");
    }
    stack.getLast().add(it);
    name.setLength(0);
  }

  private Class simpleType(String descriptor, int at, boolean array) {
    switch (descriptor.charAt(at)) {
      case 'V':
        return void.class;
      case 'Z':
        return array ? boolean[].class : boolean.class;
      case 'C':
        return array ? char[].class : char.class;
      case 'B':
        return array ? byte[].class : byte.class;
      case 'S':
        return array ? short[].class : short.class;
      case 'I':
        return array ? int[].class : int.class;
      case 'F':
        return array ? float[].class : float.class;
      case 'J':
        return array ? long[].class : long.class;
      case 'D':
        return array ? double[].class : double.class;
      case '[':
        return simpleType(descriptor, at + 1, true);
    }
    if (descriptor.equals("Ljava/lang/String;")) {
      return String.class;
    }
    if (descriptor.equals("[Ljava/lang/String;")) {
      return String[].class;
    }
    return null;
  }

  private void newParameterized(LinkedList<LinkedList<Type>> stack) {
    Type[] types = stack.removeLast().toArray(new Type[0]);
    LinkedList<Type> parent = stack.peekLast();
    if (parent.size() > 0) {
      Type rawType = parent.removeLast();
      Type paramType = Reified.getParameterized(rawType, types).getType();
      parent.addLast(paramType);
    } else {
      parent.add(types[0]);
    }
  }
}
