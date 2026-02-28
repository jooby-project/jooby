/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import io.jooby.value.ValueFactory;

/**
 * Hierarchical schema for JSON field selection. A Projection defines exactly which fields of a Java
 * object should be serialized to JSON.
 *
 * <p>It supports multiple declaration styles, all of which are validated against the target class
 * hierarchy (including unwrapping Collections and Maps) at definition time.
 *
 * <h3>1. Dot Notation</h3>
 *
 * <p>Standard path-based selection for nested objects.
 *
 * <pre>{@code
 * Projection.of(User.class).include("name", "address.city");
 * }</pre>
 *
 * <h3>2. Avaje Notation</h3>
 *
 * <p>Parenthesis-based grouping for complex nested graphs, compatible with LinkedIn-style syntax.
 *
 * <pre>{@code
 * Projection.of(User.class).include("id, address(city, zip, geo(lat, lon))");
 * }</pre>
 *
 * <h3>3. Type-Safe Method References</h3>
 *
 * <p>Refactor-safe selection using Java method references. These are validated by the compiler.
 *
 * <pre>{@code
 * Projection.of(User.class).include(User::getName, User::getId);
 * }</pre>
 *
 * <h3>4. Functional Nested DSL</h3>
 *
 * <p>A type-safe way to define deep projections while maintaining IDE autocomplete for nested
 * types.
 *
 * <pre>{@code
 * Projection.of(User.class)
 * .include(User::getName)
 * .include(User::getAddress, addr -> addr
 * .include(Address::getCity)
 * );
 * }</pre>
 *
 * <h3>Polymorphism and Validation</h3>
 *
 * <p>By default, projections strictly validate requested fields against the declared return type
 * using reflection. If a field is not found, an {@link IllegalArgumentException} is thrown at
 * compilation time.
 *
 * <p>If your route returns polymorphic types (e.g., a {@code List<Animal>} containing {@code Dog}
 * and {@code Cat} instances), strict validation will fail if you request a subclass-specific field
 * like {@code barkVolume}. To support polymorphic shaping, you can disable strict validation using
 * {@link #validate()} prior to calling {@code include()}:
 *
 * <pre>{@code
 * Projection.of(Animal.class)
 * .validate(false)
 * .include("name, barkVolume")
 * }</pre>
 *
 * <h3>Performance</h3>
 *
 * <p>Projections are pre-compiled. All reflection and path validation happen during the <code>
 * include</code> calls. In a production environment, it is recommended to define Projections as
 * <code>static final</code> constants.
 *
 * @param <T> The root type being projected.
 * @author edgar
 * @since 4.0.0
 */
public class Projection<T> {

  private static final Map<Class<?>, String> PROP_CACHE = new ConcurrentHashMap<>();

  private final Class<T> type;
  private final Map<String, Projection<?>> children = new LinkedHashMap<>();
  private String view = "";
  private final boolean root;
  private boolean validate;

  private Projection(Class<T> type, boolean root, boolean validate) {
    this.type = Objects.requireNonNull(type);
    this.root = root;
    this.validate = validate;
  }

  /**
   * Creates a new Projection for the given type.
   *
   * @param <T> Root type.
   * @param type Root class.
   * @return A new Projection instance.
   */
  public static <T> Projection<T> of(Class<T> type) {
    return new Projection<>(type, true, false);
  }

  /**
   * Includes fields via string notation. Supports both Dot notation ({@code a.b}) and Avaje
   * notation ({@code a(b,c)}).
   *
   * @param paths Field paths to include.
   * @return This projection instance.
   * @throws IllegalArgumentException If a field name is not found on the class hierarchy.
   */
  public Projection<T> include(String... paths) {
    for (String path : paths) {
      if (path == null || path.isEmpty()) continue;
      validateParentheses(path);
      for (String segment : splitByComma(path)) {
        parseAndValidate(segment.trim());
      }
    }
    rebuild();
    return this;
  }

  public Map<String, Projection<?>> getChildren() {
    return Collections.unmodifiableMap(children);
  }

  /**
   * Configures whether the projection should fail when a requested property is not found on the
   * declared class type.
   *
   * @return This projection instance.
   */
  public Projection<T> validate() {
    this.validate = true;
    return this;
  }

  /** Determines if a type is a simple/scalar value that cannot be further projected. */
  private boolean isSimpleType(Type type) {
    var valueFactory = new ValueFactory();
    return valueFactory.get(type) != null;
  }

  /**
   * Returns the Avaje-compatible DSL string.
   *
   * @return The pre-compiled view string.
   */
  public String toView() {
    return view;
  }

  public Class<T> getType() {
    return type;
  }

  private void validateParentheses(String path) {
    int depth = 0;
    for (int i = 0; i < path.length(); i++) {
      char c = path.charAt(i);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
      }

      // If depth drops below 0, we have an extra closing parenthesis like "id)"
      if (depth < 0) {
        throw new IllegalArgumentException("Mismatched parentheses in projection: " + path);
      }
    }

    // If depth is not 0 at the end, we are missing a closing parenthesis
    if (depth > 0) {
      throw new IllegalArgumentException("Missing closing parenthesis in projection: " + path);
    }
  }

  private void parseAndValidate(String path) {
    if (path == null || path.trim().isEmpty()) return;
    path = path.trim();

    // 1. Root-level grouping: "(id, name, address)"
    if (path.startsWith("(") && path.endsWith(")")) {
      String content = path.substring(1, path.length() - 1).trim();
      for (String p : splitByComma(content)) {
        parseAndValidate(p);
      }
      return;
    }

    int parenIdx = path.indexOf('(');
    int dotIdx = path.indexOf('.');

    // 2. Nested grouping: "address(city, loc)" or "address(*)"
    if (parenIdx != -1 && (dotIdx == -1 || parenIdx < dotIdx)) {
      String parentName = path.substring(0, parenIdx).trim();
      if (parentName.isEmpty()) return;

      String content = path.substring(parenIdx + 1, path.lastIndexOf(')')).trim();

      Class<?> childType = resolveFieldType(this.type, parentName);
      Projection<?> child =
          children.computeIfAbsent(parentName, k -> new Projection<>(childType, false, validate));

      for (String p : splitByComma(content)) {
        p = p.trim();
        // Ignore explicit wildcard to leave children map empty (triggering allow-all later)
        if (!p.equals("*") && !p.isEmpty()) {
          child.parseAndValidate(p);
        }
      }
      child.rebuild();
    }
    // 3. Dot notation: "address.city"
    else if (dotIdx != -1) {
      String parentName = path.substring(0, dotIdx).trim();
      String content = path.substring(dotIdx + 1).trim();

      Class<?> childType = resolveFieldType(this.type, parentName);
      Projection<?> child =
          children.computeIfAbsent(parentName, k -> new Projection<>(childType, false, validate));

      if (!content.equals("*") && !content.isEmpty()) {
        child.parseAndValidate(content);
      }
      child.rebuild();
    }
    // 4. Flat field: "id"
    else {
      if (!path.equals("*")) {
        Class<?> childType = resolveFieldType(this.type, path);
        children.computeIfAbsent(path, k -> new Projection<>(childType, false, validate));
      }
    }
  }

  private List<String> splitByComma(String s) {
    List<String> result = new ArrayList<>();
    int depth = 0;
    StringBuilder sb = new StringBuilder();
    for (char c : s.toCharArray()) {
      if (c == '(') depth++;
      else if (c == ')') depth--;

      if (c == ',' && depth == 0) {
        result.add(sb.toString());
        sb.setLength(0);
      } else {
        sb.append(c);
      }
    }
    result.add(sb.toString());
    return result;
  }

  private void rebuild() {
    StringBuilder buffer = new StringBuilder();
    int i = 0;
    for (Map.Entry<String, Projection<?>> entry : children.entrySet()) {
      if (i > 0) {
        buffer.append(",");
      }

      buffer.append(entry.getKey());
      Projection<?> child = entry.getValue();

      if (!child.getChildren().isEmpty()) {
        // Node has explicit children, recurse normally
        buffer.append("(").append(child.toView()).append(")");
      } else {
        // Option 3: Deep Smart Wildcard injection
        Class<?> childType = child.type;
        if (!childType.isPrimitive() && !childType.getName().startsWith("java.")) {
          // It's a complex POJO with no explicit children.
          // We must build a full explicit wildcard string for Avaje.
          String deepWildcard = buildDeepWildcard(childType);
          if (!deepWildcard.isEmpty()) {
            buffer.append("(").append(deepWildcard).append(")");
          }
        }
      }
      i++;
    }

    String result = buffer.toString();

    // Ensure root-level multi-fields are strictly wrapped for Avaje
    if (root && !result.startsWith("(") && result.contains(",")) {
      this.view = "(" + result + ")";
    } else {
      this.view = result;
    }
  }

  private String buildDeepWildcard(Class<?> type) {
    return buildDeepWildcard(type, new HashSet<>());
  }

  private String buildDeepWildcard(Class<?> type, Set<Class<?>> seen) {
    if (type == null || type.isPrimitive() || type.getName().startsWith("java.")) {
      return "";
    }

    if (!seen.add(type)) {
      return "";
    }

    Map<String, Type> properties = new TreeMap<>();

    // 1. Getters FIRST (The ultimate source of truth for JSON serialization)
    for (Method method : type.getMethods()) {
      if (method.getDeclaringClass() == Object.class
          || method.getParameterCount() > 0
          || java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
        continue;
      }

      String methodName = method.getName();
      String propName = null;

      if (methodName.startsWith("get") && methodName.length() > 3) {
        propName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
      } else if (methodName.startsWith("is") && methodName.length() > 2) {
        Class<?> retType = method.getReturnType();
        if (retType == boolean.class || retType == Boolean.class) {
          propName = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }
      }

      if (propName != null) {
        properties.putIfAbsent(propName, method.getGenericReturnType());
      }
    }

    // 2. Fields SECOND (Fallback for properties without getters, like Java Records or plain fields)
    Class<?> currentClass = type;
    while (currentClass != null && currentClass != Object.class) {
      for (java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
        int modifiers = field.getModifiers();
        if (java.lang.reflect.Modifier.isStatic(modifiers)
            || java.lang.reflect.Modifier.isTransient(modifiers)) {
          continue;
        }
        // Only adds the field if a getter didn't already claim this property name
        properties.putIfAbsent(field.getName(), field.getGenericType());
      }
      currentClass = currentClass.getSuperclass();
    }

    // 3. Build the View String
    StringBuilder sb = new StringBuilder();
    int count = 0;

    for (Map.Entry<String, Type> entry : properties.entrySet()) {
      if (count > 0) sb.append(",");
      sb.append(entry.getKey());

      Type propType = entry.getValue();
      Class<?> rawType = null;

      if (propType instanceof Class) {
        rawType = (Class<?>) propType;
      } else if (propType instanceof ParameterizedType) {
        ParameterizedType paramType = (ParameterizedType) propType;
        Type raw = paramType.getRawType();

        if (raw instanceof Class) {
          Class<?> rawClass = (Class<?>) raw;
          if (Collection.class.isAssignableFrom(rawClass)) {
            Type typeArg = paramType.getActualTypeArguments()[0];
            if (typeArg instanceof Class) rawType = (Class<?>) typeArg;
          } else if (Map.class.isAssignableFrom(rawClass)) {
            Type typeArg = paramType.getActualTypeArguments()[1];
            if (typeArg instanceof Class) rawType = (Class<?>) typeArg;
          } else {
            rawType = rawClass;
          }
        }
      }

      if (rawType != null && !rawType.isPrimitive() && !rawType.getName().startsWith("java.")) {
        String nested = buildDeepWildcard(rawType, seen);
        if (!nested.isEmpty()) {
          sb.append("(").append(nested).append(")");
        }
      }
      count++;
    }

    seen.remove(type);
    return sb.toString();
  }

  private Class<?> resolveFieldType(Class<?> currentType, String fieldName) {
    // 1. If we are already in a dynamic tree, keep returning Object.class
    if (currentType == null || currentType == Object.class) {
      return Object.class;
    }

    Type genericType = null;
    Class<?> rawType = null;

    // 2. Try Getters FIRST (The ultimate source of truth for JSON serialization)
    String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

    try {
      Method method = currentType.getMethod("get" + capitalized);
      rawType = method.getReturnType();
      genericType = method.getGenericReturnType();
    } catch (NoSuchMethodException e1) {
      try {
        Method method = currentType.getMethod("is" + capitalized);
        Class<?> retType = method.getReturnType();
        if (retType == boolean.class || retType == Boolean.class) {
          rawType = retType;
          genericType = method.getGenericReturnType();
        }
      } catch (NoSuchMethodException e2) {
        // Ignore
      }
    }

    // Try record-style / fluent getter if standard getters weren't found
    if (rawType == null) {
      try {
        Method method = currentType.getMethod(fieldName);
        rawType = method.getReturnType();
        genericType = method.getGenericReturnType();
      } catch (NoSuchMethodException e3) {
        // Ignore
      }
    }

    // 3. Fallback to Fields SECOND (climbing the hierarchy)
    if (rawType == null) {
      Class<?> clazz = currentType;
      while (clazz != null && clazz != Object.class) {
        try {
          Field field = clazz.getDeclaredField(fieldName);
          rawType = field.getType();
          genericType = field.getGenericType();
          break; // Found it!
        } catch (NoSuchFieldException ignored) {
          clazz = clazz.getSuperclass(); // Check the parent class
        }
      }
    }

    // 4. Handle Not Found
    if (rawType == null) {
      // Dynamic map keys fallback
      if (currentType.getName().startsWith("java.")) {
        return Object.class;
      }
      if (validate) {
        throw new IllegalArgumentException(
            "Invalid projection path: '"
                + fieldName
                + "' not found on "
                + currentType.getName()
                + " or its superclasses.");
      }
      return Object.class;
    }

    // 5. Unwrap Generics (e.g., List<Role> -> Role)
    if (genericType instanceof ParameterizedType) {
      ParameterizedType paramType = (ParameterizedType) genericType;

      if (Collection.class.isAssignableFrom(rawType)) {
        Type typeArg = paramType.getActualTypeArguments()[0];
        if (typeArg instanceof Class) return (Class<?>) typeArg;
      }

      if (Map.class.isAssignableFrom(rawType)) {
        Type typeArg = paramType.getActualTypeArguments()[1]; // Maps resolve to Value type
        if (typeArg instanceof Class) return (Class<?>) typeArg;
      }
    }

    return rawType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Projection<?> that = (Projection<?>) o;
    return root == that.root
        && Objects.equals(type, that.type)
        && Objects.equals(children, that.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, children, root);
  }

  @Override
  public String toString() {
    return type.getSimpleName() + view;
  }
}
