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
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.TypeMismatchException;
import io.jooby.Value;
import io.jooby.internal.reflect.$Types;

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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static io.jooby.SneakyThrows.propagate;

public class ValueInjector {

  private static final String AMBIGUOUS_CONSTRUCTOR =
      "Ambiguous constructor found. Expecting a single constructor or only one annotated with "
          + Inject.class.getName();

  private static final Object[] NO_ARGS = new Object[0];

  public <T> T inject(Value scope, Class type) {
    return inject(scope, type, type);
  }

  public <T> T inject(Value scope, Type type, Class rawType) {
    try {
      Object result = value(scope, rawType, type);
      return (T) result;
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException x) {
      throw propagate(x);
    } catch (InvocationTargetException x) {
      throw propagate(x.getCause());
    }
  }

  private <T> T newInstance(Class<T> type, Value scope)
      throws IllegalAccessException, InstantiationException, InvocationTargetException,
      NoSuchMethodException {
    Constructor[] constructors = type.getConstructors();
    if (constructors.length == 0) {
      return setters(type.getDeclaredConstructor().newInstance(), scope,
          Collections.emptySet());
    }
    Constructor constructor = selectConstructor(constructors);
    Set<Value> state = new HashSet<>();
    Object[] args = constructor.getParameterCount() == 0
        ? new Object[0]
        : inject(scope, constructor, state::add);
    return (T) setters(constructor.newInstance(args), scope, state);
  }

  private Constructor selectConstructor(Constructor[] constructors) {
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

  private <T> T setters(T newInstance, Value object, Set<Value> skip) {
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
                parameter.getParameterizedType());
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

  private Method findMethod(Method[] methods, String name) {
    for (Method method : methods) {
      if (method.getName().equals(name) && method.getParameterCount() == 1) {
        return method;
      }
    }
    return null;
  }

  private Object resolve(Value scope, Class type)
      throws IllegalAccessException, InvocationTargetException, InstantiationException,
      NoSuchMethodException {
    if (scope.isObject() || scope.isSingle()) {
      return newInstance(type, scope);
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

  public Object[] inject(Value scope, Executable method, Consumer<Value> state)
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
        args[i] = value(value, parameter.getType(), parameter.getParameterizedType());
      } catch (MissingValueException x) {
        throw new ProvisioningException(parameter, x);
      } catch (BadRequestException x) {
        throw new ProvisioningException(parameter, x);
      }
    }
    return args;
  }

  private String paramName(Parameter parameter) {
    String name = parameter.getName();
    Named named = parameter.getAnnotation(Named.class);
    if (named != null && named.value().length() > 0) {
      name = named.value();
    }
    return name;
  }

  public static boolean isSimple(Class rawType, Type type) {
    if (rawType == String.class) {
      return true;
    }
    if (rawType == int.class || rawType == Integer.class) {
      return true;
    }
    if (rawType == long.class || rawType == Long.class) {
      return true;
    }
    if (rawType == float.class || rawType == Float.class) {
      return true;
    }
    if (rawType == double.class || rawType == Double.class) {
      return true;
    }
    if (rawType == boolean.class || rawType == Boolean.class) {
      return true;
    }
    if (rawType == byte.class || rawType == Byte.class) {
      return true;
    }
    if (List.class.isAssignableFrom(rawType)) {
      Class type0 = $Types.parameterizedType0(type);
      return isSimple(type0, type0);
    }
    if (Set.class.isAssignableFrom(rawType)) {
      Class type0 = $Types.parameterizedType0(type);
      return isSimple(type0, type0);
    }
    if (Optional.class.isAssignableFrom(rawType)) {
      Class type0 = $Types.parameterizedType0(type);
      return isSimple(type0, type0);
    }
    if (StatusCode.class == rawType) {
      return true;
    }
    if (UUID.class == rawType) {
      return true;
    }
    if (Instant.class == rawType) {
      return true;
    }
    if (BigDecimal.class == rawType) {
      return true;
    }
    if (BigInteger.class == rawType) {
      return true;
    }
    if (Charset.class == rawType) {
      return true;
    }
    if (Path.class == rawType) {
      return true;
    }
    if (FileUpload.class == rawType) {
      return true;
    }
    /**********************************************************************************************
     * Static method: valueOf
     * ********************************************************************************************
     */
    try {
      Method valueOf = rawType.getMethod("valueOf", String.class);
      if (Modifier.isStatic(valueOf.getModifiers())) {
        return true;
      }
    } catch (NoSuchMethodException x) {
      // Ignored
    }
    return false;
  }

  private Object value(Value value, Class rawType, Type type)
      throws InvocationTargetException, IllegalAccessException, InstantiationException,
      NoSuchMethodException {
    if (value.isMissing() && rawType != Optional.class) {
      return resolve(value, rawType);
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
      return collection(value, (ParameterizedType) type, new ArrayList(value.size()));
    }
    if (Set.class.isAssignableFrom(rawType)) {
      return collection(value, (ParameterizedType) type, new LinkedHashSet(value.size()));
    }
    if (Optional.class.isAssignableFrom(rawType)) {
      try {
        Class itemType = $Types.parameterizedType0(type);
        return Optional.ofNullable(value(value.isObject() && value.size() > 0 ? value : value.get(0), itemType, itemType));
      } catch (MissingValueException x) {
        return Optional.empty();
      }
    }
    if (StatusCode.class == rawType) {
      return StatusCode.valueOf(value.get(0).intValue());
    }
    if (UUID.class == rawType) {
      return UUID.fromString(value.get(0).value());
    }
    if (Instant.class == rawType) {
      return Instant.ofEpochMilli(value.get(0).longValue());
    }
    if (BigDecimal.class == rawType) {
      return new BigDecimal(value.get(0).value());
    }
    if (BigInteger.class == rawType) {
      return new BigInteger(value.get(0).value());
    }
    if (Charset.class == rawType) {
      return Charset.forName(value.get(0).value());
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
    /**********************************************************************************************
     * Static method: valueOf
     * ********************************************************************************************
     */
    try {
      Method valueOf = rawType.getMethod("valueOf", String.class);
      if (Modifier.isStatic(valueOf.getModifiers())) {
        String enumKey = value.iterator().next().value();
        try {
          return valueOf.invoke(null, enumKey);
        } catch (InvocationTargetException x) {
          Throwable cause = x.getCause();
          if (cause instanceof IllegalArgumentException) {
            // fallback to upper case
            return valueOf.invoke(null, enumKey.toUpperCase());
          } else {
            throw SneakyThrows.propagate(cause);
          }
        }
      }
    } catch (NoSuchMethodException x) {
      // Ignored
    }
    return resolve(value, rawType);
  }

  private Collection collection(Value scope, ParameterizedType type, Collection result)
      throws InvocationTargetException, IllegalAccessException, InstantiationException,
      NoSuchMethodException {
    Class itemType = $Types.parameterizedType0(type);
    if (scope.isArray()) {
      for (Value value : scope) {
        result.add(value(value, itemType, itemType));
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
        result.add(value(value, itemType, itemType));
      }
    } else if (!scope.isMissing()) {
      result.add(value(scope, itemType, itemType));
    }
    return result;
  }
}
