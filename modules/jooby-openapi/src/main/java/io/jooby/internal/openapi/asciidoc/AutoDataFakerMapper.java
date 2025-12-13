/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

import net.datafaker.Faker;
import net.datafaker.providers.base.AbstractProvider;

public class AutoDataFakerMapper {

  private final Faker faker;

  // --- REGISTRIES (Functional) ---
  private final Map<String, Supplier<String>> specificRegistry = new HashMap<>();
  private final Map<String, Supplier<String>> genericRegistry = new HashMap<>();
  private final Map<String, Supplier<String>> typeRegistry = new HashMap<>();

  // --- SYNONYMS ---
  private final Map<String, String> synonymMap;
  private static final Map<String, String> DEFAULT_SYNONYMS = new HashMap<>();

  static {
    registerDefault("surname", "lastname");
    registerDefault("familyname", "lastname");
    registerDefault("login", "username");
    registerDefault("user", "username");
    registerDefault("fullname", "name");
    registerDefault("displayname", "name");
    registerDefault("social", "ssnvalid");
    registerDefault("ssn", "ssnvalid");
    registerDefault("mail", "emailaddress");
    registerDefault("email", "emailaddress");
    registerDefault("subject", "emailsubject");
    registerDefault("web", "url");
    registerDefault("homepage", "url");
    registerDefault("link", "url");
    registerDefault("uri", "url");
    registerDefault("avatar", "image");
    registerDefault("pic", "image");
    registerDefault("pwd", "password");
    registerDefault("pass", "password");
    registerDefault("cell", "cellphone");
    registerDefault("mobile", "cellphone");
    registerDefault("tel", "phonenumber");
    registerDefault("fax", "phonenumber");
    registerDefault("addr", "fulladdress");
    registerDefault("street", "streetaddress");
    registerDefault("postcode", "zipcode");
    registerDefault("postal", "zipcode");
    registerDefault("zip", "zipcode");
    registerDefault("town", "city");
    registerDefault("province", "state");
    registerDefault("region", "state");
    registerDefault("lat", "latitude");
    registerDefault("lon", "longitude");
    registerDefault("lng", "longitude");
    registerDefault("qty", "quantity");
    registerDefault("cost", "price");
    registerDefault("amount", "price");
    registerDefault("desc", "sentence");
    registerDefault("description", "paragraph");
    registerDefault("dept", "industry");
    registerDefault("role", "title");
    registerDefault("position", "title");
    registerDefault("dob", "birthday");
    registerDefault("born", "birthday");
    registerDefault("created", "date");
    registerDefault("modified", "date");
    registerDefault("timestamp", "date");
    registerDefault("guid", "uuid");
  }

  private static void registerDefault(String key, String value) {
    DEFAULT_SYNONYMS.put(key, value);
  }

  // --- CONSTRUCTORS ---

  public AutoDataFakerMapper() {
    this.faker = new Faker();
    this.synonymMap = new HashMap<>(DEFAULT_SYNONYMS);

    initializeReflectionRegistry();
    initializeTypeRegistry();
  }

  public void synonyms(Map<String, String> synonyms) {
    synonyms.forEach((k, v) -> this.synonymMap.put(normalize(k), normalize(v)));
  }

  private void initializeReflectionRegistry() {
    Arrays.stream(Faker.class.getMethods())
        .filter(this::isProviderMethod)
        .forEach(this::registerProvider);
  }

  private void registerType(String type, Supplier<String> supplier, String description) {
    String cleanType = normalize(type);
    typeRegistry.put(cleanType, fakeSupplier(supplier, description));
  }

  private static Supplier<String> fakeSupplier(Supplier<String> supplier, String signature) {
    return new Supplier<>() {
      @Override
      public String get() {
        return supplier.get();
      }

      @Override
      public String toString() {
        return signature;
      }
    };
  }

  private void initializeTypeRegistry() {
    // domains
    specificRegistry.put(
        "book.isbn", fakeSupplier(() -> faker.code().isbn13(), "faker.code().isbn13()"));

    // We now register the Description alongside the Supplier
    registerType("uuid", () -> faker.internet().uuid(), "faker.internet().uuid()");
    registerType("email", () -> faker.internet().emailAddress(), "faker.internet().emailAddress()");
    registerType(
        "password", () -> faker.credentials().password(), "faker.credentials().password()");
    registerType("ipv4", () -> faker.internet().ipV4Address(), "faker.internet().ipV4Address()");
    registerType("ipv6", () -> faker.internet().ipV6Address(), "faker.internet().ipV6Address()");
    registerType("uri", () -> faker.internet().url(), "faker.internet().url()");
    registerType("url", () -> faker.internet().url(), "faker.internet().url()");
    registerType("hostname", () -> faker.internet().domainName(), "faker.internet().domainName()");

    registerType(
        "date", () -> faker.timeAndDate().birthday().toString(), "faker.timeAndDate().birthday()");
    registerType(
        "datetime",
        () -> faker.timeAndDate().past(365, java.util.concurrent.TimeUnit.DAYS).toString(),
        "faker.timeAndDate().past()");
    registerType(
        "time",
        () -> faker.timeAndDate().birthday().toString().split(" ")[1],
        "faker.timeAndDate().birthday() (time-part)");

    registerType(
        "integer",
        () -> String.valueOf(faker.number().numberBetween(1, 100)),
        "faker.number().numberBetween(1, 100)");
    registerType(
        "int32",
        () -> String.valueOf(faker.number().numberBetween(1, 100)),
        "faker.number().numberBetween(1, 100)");
    registerType(
        "int64",
        () -> String.valueOf(faker.number().numberBetween(1, 100)),
        "faker.number().numberBetween(1, 100)");
    registerType(
        "float",
        () -> String.valueOf(faker.number().randomDouble(2, 0, 100)),
        "faker.number().randomDouble()");
    registerType(
        "double",
        () -> String.valueOf(faker.number().randomDouble(4, 0, 100)),
        "faker.number().randomDouble()");
    registerType(
        "number",
        () -> String.valueOf(faker.number().numberBetween(0, 100)),
        "faker.number().numberBetween(1, 100)");

    registerType("boolean", () -> String.valueOf(faker.bool().bool()), "faker.bool().bool()");

    registerType("string", () -> "string", "string");
  }

  private void registerProvider(Method providerMethod) {
    try {
      Object providerInstance = providerMethod.invoke(faker);
      String domainName = normalize(providerMethod.getName());

      Arrays.stream(providerInstance.getClass().getMethods())
          .filter(this::isValidGeneratorMethod)
          .forEach(method -> registerMethod(domainName, providerInstance, method));

    } catch (Exception ignored) {
    }
  }

  private void registerMethod(String domainName, Object providerInstance, Method method) {
    String fieldName = normalize(method.getName());
    String signature = "faker.%s().%s()".formatted(domainName, method.getName());

    Supplier<String> rawGenerator = fakerSupplier(providerInstance, method, signature);

    // 1. Specific Registry
    specificRegistry.put(domainName + "." + fieldName, rawGenerator);

    // add base generic only
    if (method.getDeclaringClass().getPackage().equals(AbstractProvider.class.getPackage())) {
      // 2. Generic Registry (First one wins)
      genericRegistry.putIfAbsent(fieldName, rawGenerator);
    }
  }

  // --- CORE LOGIC (Unchanged) ---
  public Supplier<String> getGenerator(
      String className, String fieldName, String fieldType, String defaultValue) {
    var cleanClass = normalize(className);
    var cleanField = normalize(fieldName);
    var cleanType = normalize(fieldType);

    String resolvedField = synonymMap.getOrDefault(cleanField, cleanField);

    var specific = specificRegistry.get(cleanClass + "." + resolvedField);
    if (specific != null) return wrap(specific, defaultValue);

    var generic = genericRegistry.get(resolvedField);
    if (generic != null) return wrap(generic, defaultValue);

    var fuzzy =
        genericRegistry.entrySet().stream()
            .filter(entry -> resolvedField.contains(entry.getKey()) && entry.getKey().length() > 3)
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);

    if (fuzzy != null) return wrap(fuzzy, defaultValue);

    if (!cleanType.isEmpty()) {
      var typeGen = typeRegistry.get(cleanType);
      if (typeGen != null) return wrap(typeGen, defaultValue);
    }

    return () -> defaultValue;
  }

  // --- CAPABILITY MAP (IMPROVED) ---

  /** Returns a structured map of all available capabilities with Source details. */
  public Map<String, Object> getCapabilityMap() {
    // 1. Domains Tree: "book" -> { "title": "faker.book().title()" }
    Map<String, Map<String, String>> domainsTree = new TreeMap<>();
    specificRegistry.forEach(
        (key, signature) -> {
          int dotIndex = key.indexOf('.');
          if (dotIndex > 0) {
            String domain = key.substring(0, dotIndex);
            String field = key.substring(dotIndex + 1);
            domainsTree
                .computeIfAbsent(domain, k -> new TreeMap<>())
                .put(field, signature.toString());
          }
        });

    // 2. Generics Map: "title" -> "faker.book().title()"
    // (Using TreeMap for sorting)
    Map<String, String> genericsMap = new TreeMap<>();
    genericRegistry.forEach(
        (key, signature) -> {
          genericsMap.put(key, signature.toString());
        });

    // 3. Types Map: "uuid" -> "faker.internet().uuid()"
    Map<String, String> typesMap = new TreeMap<>();
    typeRegistry.forEach(
        (key, signature) -> {
          typesMap.put(key, signature.toString());
        });

    // 4. Synonyms Copy
    Map<String, String> synonymsCopy = new TreeMap<>(synonymMap);

    return Map.of(
        "domains", domainsTree,
        "generics", genericsMap,
        "types", typesMap,
        "synonyms", synonymsCopy);
  }

  private static Supplier<String> fakerSupplier(Object instance, Method method, String signature) {
    return new Supplier<>() {
      @Override
      public String get() {
        try {
          return (String) method.invoke(instance);
        } catch (Exception ignored) {
          return null;
        }
      }

      @Override
      public String toString() {
        return signature;
      }
    };
  }

  private static Supplier<String> wrap(Supplier<String> supplier, String defaultValue) {
    return new Supplier<>() {
      @Override
      public String get() {
        try {
          String res = supplier.get();
          return res != null ? res : defaultValue;
        } catch (Exception e) {
          return defaultValue;
        }
      }

      @Override
      public String toString() {
        return supplier.toString();
      }
    };
  }

  private boolean isProviderMethod(Method m) {
    return m.getParameterCount() == 0 && AbstractProvider.class.isAssignableFrom(m.getReturnType());
  }

  private boolean isValidGeneratorMethod(Method m) {
    return Modifier.isPublic(m.getModifiers())
        && m.getParameterCount() == 0
        && m.getReturnType().equals(String.class)
        && !isStandardMethod(m.getName());
  }

  private boolean isStandardMethod(String name) {
    return "toString".equals(name);
  }

  private String normalize(String input) {
    if (input == null || input.isBlank()) return "";
    return input.toLowerCase().trim().replaceAll("[^a-z0-9]", "");
  }
}
