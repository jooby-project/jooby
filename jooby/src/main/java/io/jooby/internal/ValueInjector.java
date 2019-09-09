/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.BadRequestException;
import io.jooby.FileUpload;
import io.jooby.MissingValueException;
import io.jooby.ProvisioningException;
import io.jooby.TypeMismatchException;
import io.jooby.Value;
import io.jooby.spi.ValueConverter;
import io.jooby.internal.converter.ValueOfConverter;
import io.jooby.internal.reflect.$Types;
import io.jooby.spi.BeanValueConverters;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;

import static io.jooby.SneakyThrows.propagate;

public final class ValueInjector {

  private static final String AMBIGUOUS_CONSTRUCTOR =
      "Ambiguous constructor found. Expecting a single constructor or only one annotated with "
          + Inject.class.getName();

  private static final Object[] NO_ARGS = new Object[0];
  private static List<ValueConverter> CONVERTERS;

  public static <T> T inject(Value scope, Class type) {
    return inject(scope, type, type);
  }

  public static <T> T inject(Value scope, Type type, Class rawType) {
    try {
      Object result = value(scope, rawType, type, converters());
      return (T) result;
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException x) {
      throw propagate(x);
    } catch (InvocationTargetException x) {
      throw propagate(x.getCause());
    }
  }

  private static <T> T newInstance(Class<T> type, Value scope, List<ValueConverter> converters)
      throws IllegalAccessException, InstantiationException, InvocationTargetException,
      NoSuchMethodException {
    Constructor[] constructors = type.getConstructors();
    if (constructors.length == 0) {
      return setters(type.getDeclaredConstructor().newInstance(), scope,
          Collections.emptySet(), converters);
    }
    Constructor constructor = selectConstructor(constructors);
    Set<Value> state = new HashSet<>();
    Object[] args = constructor.getParameterCount() == 0
        ? new Object[0]
        : inject(scope, constructor, state::add, converters);
    return (T) setters(constructor.newInstance(args), scope, state, converters);
  }

  private static Constructor selectConstructor(Constructor[] constructors) {
    Constructor result = null;
    if (constructors.length == 1) {
      result = constructors[0];
    } else {
      for (Constructor constructor : constructors) {
        if (Modifier.isPublic(constructor.getModifiers())) {
          Annotation inject = constructor.getAnnotation(Inject.class);
          if (inject != null) {
            if (result == null) {
              result = constructor;
            } else {
              throw new IllegalStateException(AMBIGUOUS_CONSTRUCTOR);
            }
          }
        }
      }
    }
    if (result == null) {
      throw new IllegalStateException(AMBIGUOUS_CONSTRUCTOR);
    }
    return result;
  }

  private static <T> T setters(T newInstance, Value object, Set<Value> skip,
      List<ValueConverter> converters) {
    Method[] methods = newInstance.getClass().getMethods();
    for (Value value : object) {
      if (!skip.contains(value)) {
        String name = value.name();
        String setter1 = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        Method method = findMethod(methods, setter1);
        if (method == null) {
          method = findMethod(methods, name);
        }
        if (method != null) {
          Parameter parameter = method.getParameters()[0];
          try {
            Object arg = value(value, parameter.getType(),
                parameter.getParameterizedType(), converters);
            method.invoke(newInstance, arg);
          } catch (InvocationTargetException x) {
            throw new ProvisioningException(parameter, x.getCause());
          } catch (Exception x) {
            throw new ProvisioningException(parameter, x);
          }
        }
      }
    }
    return newInstance;
  }

  private static Method findMethod(Method[] methods, String name) {
    for (Method method : methods) {
      if (method.getName().equals(name) && method.getParameterCount() == 1) {
        return method;
      }
    }
    return null;
  }

  private static Object resolve(Value scope, Class type, List<ValueConverter> converters)
      throws IllegalAccessException, InvocationTargetException, InstantiationException,
      NoSuchMethodException {
    if (scope.isObject() || scope.isSingle()) {
      Object o = BeanValueConverters.getInstance().convert(scope, type);
      if (o != null) {
        return o;
      }
      return newInstance(type, scope, converters);
    } else if (scope.isMissing()) {
      if (type.isPrimitive()) {
        // throws Err.Missing
        return scope.value();
      }
      return null;
    } else {
      throw new TypeMismatchException(scope.name(), type);
    }
  }

  public static Object[] inject(Value scope, Executable method, Consumer<Value> state,
      List<ValueConverter> converters)
      throws IllegalAccessException, InstantiationException, InvocationTargetException,
      NoSuchMethodException {
    Parameter[] parameters = method.getParameters();
    if (parameters.length == 0) {
      return NO_ARGS;
    }
    Object[] args = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      String name = paramName(parameter);
      Value value = scope.get(name);
      state.accept(value);
      try {
        args[i] = value(value, parameter.getType(), parameter.getParameterizedType(), converters);
      } catch (MissingValueException x) {
        throw new ProvisioningException(parameter, x);
      } catch (BadRequestException x) {
        throw new ProvisioningException(parameter, x);
      }
    }
    return args;
  }

  private static String paramName(Parameter parameter) {
    String name = parameter.getName();
    Named named = parameter.getAnnotation(Named.class);
    if (named != null && named.value().length() > 0) {
      name = named.value();
    }
    return name;
  }

  private static Object value(Value value, Class rawType, Type type,
      List<ValueConverter> converters)
      throws InvocationTargetException, IllegalAccessException, InstantiationException,
      NoSuchMethodException {
    if (value.isMissing() && rawType != Optional.class) {
      return resolve(value, rawType, converters);
    }
    if (rawType == String.class) {
      return value.get(0).value();
    }
    if (rawType == int.class || rawType == Integer.class) {
      return value.get(0).intValue();
    }
    if (rawType == long.class || rawType == Long.class) {
      return value.get(0).longValue();
    }
    if (rawType == float.class || rawType == Float.class) {
      return value.get(0).floatValue();
    }
    if (rawType == double.class || rawType == Double.class) {
      return value.get(0).doubleValue();
    }
    if (rawType == boolean.class || rawType == Boolean.class) {
      return value.get(0).booleanValue();
    }
    if (rawType == byte.class || rawType == Byte.class) {
      return value.get(0).byteValue();
    }
    if (List.class.isAssignableFrom(rawType)) {
      return collection(value, (ParameterizedType) type, new ArrayList(value.size()), converters);
    }
    if (Set.class.isAssignableFrom(rawType)) {
      return collection(value, (ParameterizedType) type, new LinkedHashSet(value.size()),
          converters);
    }
    if (Optional.class == rawType) {
      try {
        Class itemType = $Types.parameterizedType0(type);
        return Optional.ofNullable(
            value(value.isObject() && value.size() > 0 ? value : value.get(0), itemType, itemType,
                converters));
      } catch (MissingValueException x) {
        return Optional.empty();
      }
    }
    if (Path.class == rawType) {
      if (value.get(0).isUpload()) {
        FileUpload upload = (FileUpload) value.get(0);
        return upload.path();
      }
      throw new TypeMismatchException(value.name(), Path.class);
    }
    if (FileUpload.class == rawType) {
      if (value.get(0).isUpload()) {
        return value.get(0);
      }
      throw new TypeMismatchException(value.name(), FileUpload.class);
    }
    Value first = value.isObject() && value.size() > 0 ? value.iterator().next() : value.get(0);
    for (ValueConverter converter : converters) {
      if (converter.supports(rawType)) {
        return converter.convert(rawType, first.value());
      }
    }
    return resolve(value, rawType, converters);
  }

  private static Collection collection(Value scope, ParameterizedType type, Collection result,
      List<ValueConverter> converters)
      throws InvocationTargetException, IllegalAccessException, InstantiationException,
      NoSuchMethodException {
    Class itemType = $Types.parameterizedType0(type);
    if (scope.isArray()) {
      for (Value value : scope) {
        result.add(value(value, itemType, itemType, converters));
      }
    } else if (scope.isObject()) {
      Iterable<Value> values = scope;
      if (scope.size() == 1) {
        // Edge cases when we want to use a list on single value objects:
        Value next = scope.iterator().next();
        if (next.isSingle() || next.isArray()) {
          values = Collections.singletonList(scope);
        }
      }
      for (Value value : values) {
        result.add(value(value, itemType, itemType, converters));
      }
    } else if (!scope.isMissing()) {
      result.add(value(scope, itemType, itemType, converters));
    }
    return result;
  }

  private static synchronized List<ValueConverter> converters() {
    if (CONVERTERS == null) {
      CONVERTERS = new ArrayList<>();
      Iterator<ValueConverter> iterator = ServiceLoader.load(ValueConverter.class).iterator();
      while (iterator.hasNext()) {
        CONVERTERS.add(iterator.next());
      }
      CONVERTERS.add(new ValueOfConverter());
    }
    return CONVERTERS;
  }
}
