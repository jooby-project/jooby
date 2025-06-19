/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import static io.jooby.SneakyThrows.propagate;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.FileUpload;
import io.jooby.Formdata;
import io.jooby.Usage;
import io.jooby.Value;
import io.jooby.annotation.EmptyBean;
import io.jooby.exception.BadRequestException;
import io.jooby.exception.ProvisioningException;
import io.jooby.internal.reflect.$Types;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class ReflectiveBeanConverter {

  private record Setter(Method method, Object arg) {
    public void invoke(Object instance) throws InvocationTargetException, IllegalAccessException {
      method.invoke(instance, arg);
    }
  }

  private static final String AMBIGUOUS_CONSTRUCTOR =
      "Ambiguous constructor found. Expecting a single constructor or only one annotated with "
          + Inject.class.getName();

  private static final Object[] NO_ARGS = new Object[0];

  public Object convert(@NonNull Value node, @NonNull Class type, boolean allowEmptyBean) {
    try {
      return newInstance(type, node, allowEmptyBean);
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException x) {
      throw propagate(x);
    } catch (InvocationTargetException x) {
      throw propagate(x.getCause());
    }
  }

  private static Object newInstance(Class type, Value node, boolean allowEmptyBean)
      throws IllegalAccessException,
          InstantiationException,
          InvocationTargetException,
          NoSuchMethodException {
    Constructor[] constructors = type.getConstructors();
    Set<Value> state = new HashSet<>();
    Constructor constructor;
    if (constructors.length == 0) {
      constructor = type.getDeclaredConstructor();
    } else {
      constructor = selectConstructor(constructors);
    }
    Object[] args =
        constructor.getParameterCount() == 0 ? NO_ARGS : inject(node, constructor, state::add);
    List<Setter> setters = setters(type, node, state);
    if (!allowEmptyBean(type, allowEmptyBean) && state.stream().allMatch(Value::isMissing)) {
      return null;
    }
    var instance = constructor.newInstance(args);
    for (Setter setter : setters) {
      setter.invoke(instance);
    }
    return instance;
  }

  private static boolean allowEmptyBean(Class type, boolean defaults) {
    return type.getAnnotation(EmptyBean.class) != null || defaults;
  }

  private static Constructor selectConstructor(Constructor[] constructors) {
    if (constructors.length == 1) {
      return constructors[0];
    } else {
      Constructor injectConstructor = null;
      Constructor defaultConstructor = null;
      for (Constructor constructor : constructors) {
        if (Modifier.isPublic(constructor.getModifiers())) {
          Annotation inject = constructor.getAnnotation(Inject.class);
          if (inject == null) {
            if (constructor.getParameterCount() == 0) {
              defaultConstructor = constructor;
            }
          } else {
            if (injectConstructor == null) {
              injectConstructor = constructor;
            } else {
              throw new IllegalStateException(AMBIGUOUS_CONSTRUCTOR);
            }
          }
        }
      }
      Constructor result = injectConstructor == null ? defaultConstructor : injectConstructor;
      if (result == null) {
        throw new IllegalStateException(AMBIGUOUS_CONSTRUCTOR);
      }
      return result;
    }
  }

  public static Object[] inject(Value scope, Executable method, Consumer<Value> state) {
    Parameter[] parameters = method.getParameters();
    if (parameters.length == 0) {
      return NO_ARGS;
    }
    Object[] args = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      String name = paramName(parameter);
      Value param = scope.get(name);
      var arg = value(parameter, scope, param);
      if (arg == null) {
        state.accept(Value.missing(name));
      } else {
        state.accept(param);
      }
      args[i] = arg;
    }
    return args;
  }

  private static String paramName(Parameter parameter) {
    Named named = parameter.getAnnotation(Named.class);
    if (named != null && named.value().length() > 0) {
      return named.value();
    }
    if (parameter.isNamePresent()) {
      return parameter.getName();
    }
    throw Usage.parameterNameNotPresent(parameter);
  }

  /**
   * Collect all possible values.
   *
   * @param node Root node.
   * @return Names, including file names.
   */
  private static Set<String> names(Value node) {
    Set<String> names = new LinkedHashSet<>();
    for (var item : node) {
      names.add(item.name());
    }
    if (node instanceof Formdata) {
      for (FileUpload file : ((Formdata) node).files()) {
        names.add(file.getName());
      }
    }
    return names;
  }

  private static List<Setter> setters(Class type, Value node, Set<Value> nodes) {
    var methods = type.getMethods();
    var result = new ArrayList<Setter>();
    for (String name : names(node)) {
      Value value = node.get(name);
      if (nodes.add(value)) {
        Method method = findSetter(methods, name);
        if (method != null) {
          Parameter parameter = method.getParameters()[0];
          try {
            Object arg = value(parameter, node, value);
            result.add(new Setter(method, arg));
          } catch (ProvisioningException x) {
            throw x;
          } catch (Exception x) {
            throw new ProvisioningException(parameter, x);
          }
        } else {
          nodes.remove(value);
        }
      }
    }
    return result;
  }

  private static Object value(Parameter parameter, Value node, Value value) {
    try {
      if (isFileUpload(node, parameter)) {
        Formdata formdata = (Formdata) node;
        if (Set.class.isAssignableFrom(parameter.getType())) {
          return new HashSet<>(formdata.files(value.name()));
        } else if (Collection.class.isAssignableFrom(parameter.getType())) {
          return formdata.files(value.name());
        } else if (Optional.class.isAssignableFrom(parameter.getType())) {
          List<FileUpload> files = formdata.files(value.name());
          return files.isEmpty() ? Optional.empty() : Optional.of(files.get(0));
        } else {
          return formdata.file(value.name());
        }
      } else {
        if (Set.class.isAssignableFrom(parameter.getType())) {
          return value.toSet($Types.parameterizedType0(parameter.getParameterizedType()));
        } else if (Collection.class.isAssignableFrom(parameter.getType())) {
          return value.toList($Types.parameterizedType0(parameter.getParameterizedType()));
        } else if (Optional.class.isAssignableFrom(parameter.getType())) {
          return value.toOptional($Types.parameterizedType0(parameter.getParameterizedType()));
        } else {
          if (isNullable(parameter)) {
            if (value.isSingle()) {
              var str = value.valueOrNull();
              if (str == null || str.length() == 0) {
                // treat empty values as null
                return null;
              }
            }
            return value.toNullable(parameter.getType());
          } else {
            return value.to(parameter.getType());
          }
        }
      }
    } catch (BadRequestException x) {
      throw new ProvisioningException(parameter, x);
    }
  }

  private static boolean isNullable(Parameter parameter) {
    var type = parameter.getType();
    if (hasAnnotation(parameter, ".Nullable")) {
      return true;
    }
    boolean nonnull = hasAnnotation(parameter, ".NonNull");
    if (nonnull) {
      return false;
    }
    return !type.isPrimitive();
  }

  private static boolean hasAnnotation(AnnotatedElement element, String... names) {
    var nameList = List.of(names);
    for (Annotation annotation : element.getAnnotations()) {
      if (nameList.stream()
          .anyMatch(name -> annotation.annotationType().getSimpleName().endsWith(name))) {
        return true;
      }
    }
    return false;
  }

  private static boolean isFileUpload(Value node, Parameter parameter) {
    return (node instanceof Formdata) && isFileUpload(parameter.getType())
        || isFileUpload($Types.parameterizedType0(parameter.getParameterizedType()));
  }

  private static boolean isFileUpload(Class type) {
    return FileUpload.class == type;
  }

  private static Method findSetter(Method[] methods, String name) {
    var setter = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    var candidates = new LinkedList<Method>();
    for (Method method : methods) {
      if ((method.getName().equals(name) || method.getName().equals(setter))
          && method.getParameterCount() == 1) {
        if (method.getName().startsWith("set")) {
          candidates.addFirst(method);
        } else {
          candidates.addLast(method);
        }
      }
    }
    return candidates.isEmpty() ? null : candidates.getFirst();
  }
}
