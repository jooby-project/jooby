/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.value;

import static io.jooby.SneakyThrows.propagate;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.FileUpload;
import io.jooby.Formdata;
import io.jooby.Usage;
import io.jooby.exception.BadRequestException;
import io.jooby.exception.ProvisioningException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.internal.reflect.$Types;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Creates an object from {@link Value}. Value might come from HTTP Context (Query, Path, Form,
 * etc.) or from configuration value.
 *
 * <p>This is the fallback/default converter for a JavaBeans object.
 *
 * @author edgar
 * @since 1.0.0
 */
public class ReflectiveBeanConverter implements Converter {

  private record Setter(Method method, Object arg) {
    public void invoke(MethodHandles.Lookup lookup, Object instance) throws Throwable {
      var handle = lookup.unreflect(method);
      handle.invoke(instance, arg);
    }
  }

  private static final String AMBIGUOUS_CONSTRUCTOR =
      "Ambiguous constructor found. Expecting a single constructor or only one annotated with "
          + Inject.class.getName();

  private final ValueFactory factory;
  private final MethodHandles.Lookup lookup;

  /**
   * Creates a new instance using a lookup.
   *
   * @param factory Value factory.
   * @param lookup Method handle lookup.
   */
  public ReflectiveBeanConverter(ValueFactory factory, MethodHandles.Lookup lookup) {
    this.factory = factory;
    this.lookup = lookup;
  }

  /**
   * Convert a value into a JavaBean object.
   *
   * <p>Selected constructor follows one of these rules:
   *
   * <ul>
   *   <li>It is the default (no args) constructor.
   *   <li>There is only when constructor. If the constructor has non-null arguments a {@link
   *       ProvisioningException} will be thrown when {@link Value} fails to resolve the non-null
   *       argument
   *   <li>There are multiple constructor but only one is annotated with {@link Inject}. If the
   *       constructor has non-null arguments a {@link ProvisioningException} will be thrown when
   *       {@link Value} fails to resolve the non-null argument
   * </ul>
   *
   * <p>Any other value is matched against a setter like method. Method might or might not be
   * prefixed with <code>set</code>.
   *
   * <p>Argument might be annotated with nullable like annotations. Optionally with {@link Named}
   * annotation for non-standard Java Names.
   *
   * @param type Requested type.
   * @param value Value value.
   * @param hint Requested hint.
   * @return Object instance.
   * @throws TypeMismatchException when convert returns <code>null</code> and hint is set to {@link
   *     ConversionHint#Strict}.
   * @throws ProvisioningException when convert target type constructor requires a non-null value
   *     and value is missing or null.
   */
  @Override
  public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint)
      throws TypeMismatchException, ProvisioningException {
    var rawType = $Types.parameterizedType0(type);
    var allowEmptyBean = hint == ConversionHint.Empty;
    try {
      var constructors = rawType.getConstructors();
      Set<Value> state = new HashSet<>();
      Constructor<?> constructor;
      if (constructors.length == 0) {
        //noinspection unchecked
        constructor = rawType.getDeclaredConstructor();
      } else {
        constructor = selectConstructor(constructors);
      }
      var args = inject(factory, value, constructor, state::add);
      var setters = setters(rawType, value, state);
      Object instance;
      if (!allowEmptyBean && state.stream().allMatch(Value::isMissing)) {
        instance = null;
      } else {
        var handle = lookup.unreflectConstructor(constructor);
        instance = handle.invokeWithArguments(args);
        for (var setter : setters) {
          setter.invoke(lookup, instance);
        }
      }
      if (instance == null && hint == ConversionHint.Strict) {
        throw new TypeMismatchException(value.name(), type);
      }
      return instance;
    } catch (InvocationTargetException x) {
      throw propagate(x.getCause());
    } catch (Throwable x) {
      throw propagate(x);
    }
  }

  private static Constructor<?> selectConstructor(Constructor<?>[] constructors) {
    if (constructors.length == 1) {
      return constructors[0];
    } else {
      Constructor<?> injectConstructor = null;
      Constructor<?> defaultConstructor = null;
      for (var constructor : constructors) {
        if (Modifier.isPublic(constructor.getModifiers())) {
          var inject = constructor.getAnnotation(Inject.class);
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
      var result = injectConstructor == null ? defaultConstructor : injectConstructor;
      if (result == null) {
        throw new IllegalStateException(AMBIGUOUS_CONSTRUCTOR);
      }
      return result;
    }
  }

  private static List<Object> inject(
      ValueFactory factory, Value scope, Executable method, Consumer<Value> state) {
    var parameters = method.getParameters();
    if (parameters.length == 0) {
      return List.of();
    }
    var args = new ArrayList<>(parameters.length);
    for (var parameter : parameters) {
      var name = parameterName(parameter);
      var param = scope.get(name);
      var arg = value(parameter, scope, param);
      if (arg == null) {
        state.accept(Value.missing(factory, name));
      } else {
        state.accept(param);
      }
      args.add(arg);
    }
    return args;
  }

  private static String parameterName(Parameter parameter) {
    var named = parameter.getAnnotation(Named.class);
    if (named != null && !named.value().isEmpty()) {
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

  private static List<Setter> setters(Class<?> type, Value node, Set<Value> nodes) {
    var methods = type.getMethods();
    var result = new ArrayList<Setter>();
    for (String name : names(node)) {
      var value = node.get(name);
      if (nodes.add(value)) {
        var method = findSetter(methods, name);
        if (method != null) {
          var parameter = method.getParameters()[0];
          try {
            var arg = value(parameter, node, value);
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

  @SuppressWarnings("unchecked")
  private static Object value(Parameter parameter, Value node, Value value) {
    try {
      if (isFileUpload(node, parameter)) {
        var formdata = (Formdata) node;
        if (Set.class.isAssignableFrom(parameter.getType())) {
          return new LinkedHashSet<>(formdata.files(value.name()));
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
              if (str == null || str.isEmpty()) {
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
    for (var annotation : element.getAnnotations()) {
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

  private static boolean isFileUpload(Class<?> type) {
    return FileUpload.class == type;
  }

  private static Method findSetter(Method[] methods, String name) {
    var setter = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    var candidates = new LinkedList<Method>();
    for (var method : methods) {
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
