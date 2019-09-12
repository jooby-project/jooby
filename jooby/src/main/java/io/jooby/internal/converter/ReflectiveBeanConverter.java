package io.jooby.internal.converter;

import io.jooby.BadRequestException;
import io.jooby.MissingValueException;
import io.jooby.ProvisioningException;
import io.jooby.Value;
import io.jooby.ValueConverter;
import io.jooby.internal.reflect.$Types;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static io.jooby.SneakyThrows.propagate;

public class ReflectiveBeanConverter implements ValueConverter {
  private static final String AMBIGUOUS_CONSTRUCTOR =
      "Ambiguous constructor found. Expecting a single constructor or only one annotated with "
          + Inject.class.getName();

  private static final Object[] NO_ARGS = new Object[0];

  @Override public boolean supports(@Nonnull Class type) {
    return true;
  }

  @Override public Object convert(@Nonnull Value value, @Nonnull Class type) {
    try {
      return newInstance(type, value);
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException x) {
      throw propagate(x);
    } catch (InvocationTargetException x) {
      throw propagate(x.getCause());
    }
  }

  private static <T> T newInstance(Class<T> type, Value scope)
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
        ? NO_ARGS
        : inject(scope, constructor, state::add);
    return (T) setters(constructor.newInstance(args), scope, state);
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
      state.accept(param);
      args[i] = value(parameter, param);
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

  private static <T> T setters(T newInstance, Value object, Set<Value> skip) {
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
            Object arg = value(parameter, value);
            method.invoke(newInstance, arg);
          } catch (ProvisioningException x) {
            throw x;
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

  private static Object value(Parameter parameter, Value param) {
    try {
      if (List.class.isAssignableFrom(parameter.getType())) {
        return param.toList($Types.parameterizedType0(parameter.getParameterizedType()));
      } else if (Set.class.isAssignableFrom(parameter.getType())) {
        return param.toSet($Types.parameterizedType0(parameter.getParameterizedType()));
      } else if (Optional.class.isAssignableFrom(parameter.getType())) {
        return param.toOptional($Types.parameterizedType0(parameter.getParameterizedType()));
      } else {
        if (param.isMissing() && parameter.getType().isPrimitive()) {
          // fail
          param.value();
        }
        return param.to(parameter.getType());
      }
    } catch (MissingValueException x) {
      throw new ProvisioningException(parameter, x);
    } catch (BadRequestException x) {
      throw new ProvisioningException(parameter, x);
    }
  }

  private static Method findMethod(Method[] methods, String name) {
    for (Method method : methods) {
      if (method.getName().equals(name) && method.getParameterCount() == 1) {
        return method;
      }
    }
    return null;
  }
}
