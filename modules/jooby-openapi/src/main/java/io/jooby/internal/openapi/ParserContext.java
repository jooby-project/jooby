/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import static io.jooby.internal.openapi.TypeFactory.COROUTINE_ROUTER;
import static io.jooby.internal.openapi.TypeFactory.JOOBY;
import static io.jooby.internal.openapi.TypeFactory.KOOBY;
import static io.jooby.internal.openapi.TypeFactory.ROUTER;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.internal.openapi.javadoc.JavaDocParser;
import io.jooby.openapi.DebugOption;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.*;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;

public class ParserContext {

  public static class TypeLiteral {
    public JavaType type;

    public TypeLiteral() {}
  }

  private final SpecVersion specVersion;
  private String mainClass;
  private final ObjectMapper json;
  private final ObjectMapper yaml;
  private final ModelConvertersExt converters;
  private final Type router;
  private final Map<Type, ClassNode> nodes;
  private final ClassSource source;
  private final Set<Object> instructions = new HashSet<>();
  private final Set<DebugOption> debug;
  private final ConcurrentMap<String, SchemaRef> schemas = new ConcurrentHashMap<>();
  private final JavaDocParser javadocParser;

  public ParserContext(
      SpecVersion specVersion,
      ObjectMapper json,
      ObjectMapper yaml,
      ClassSource source,
      Type router,
      JavaDocParser javadocParser,
      Set<DebugOption> debug) {
    this(specVersion, json, yaml, source, new HashMap<>(), router, javadocParser, debug);
  }

  private ParserContext(
      SpecVersion specVersion,
      ObjectMapper json,
      ObjectMapper yaml,
      ClassSource source,
      Map<Type, ClassNode> nodes,
      Type router,
      JavaDocParser javadocParser,
      Set<DebugOption> debug) {
    this.specVersion = specVersion;
    this.json = json;
    this.yaml = yaml;
    this.router = router;
    this.source = source;
    this.debug = Optional.ofNullable(debug).orElse(Collections.emptySet());
    this.nodes = nodes;
    this.javadocParser = javadocParser;

    var mappers = List.of(json);
    jacksonModules(source.getClassLoader(), mappers);
    this.converters = new ModelConvertersExt(specVersion);
    mappers.stream().map(ModelConverterExt::new).forEach(converters::addConverter);
  }

  private void jacksonModules(ClassLoader classLoader, List<ObjectMapper> mappers) {
    /** Kotlin module? */
    List<Module> modules = new ArrayList<>(2);
    try {
      var kotlinModuleClass =
          classLoader.loadClass("com.fasterxml.jackson.module.kotlin.KotlinModule");
      var constructor = kotlinModuleClass.getDeclaredConstructor();
      Module module = (Module) constructor.newInstance();
      modules.add(module);
    } catch (ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | NoSuchMethodException
        | InvocationTargetException x) {
      // Sshhhhh
    }
    /** Ignore some conflictive setter in Jooby API: */
    modules.add(
        new SimpleModule("jooby-openapi") {
          @Override
          public void setupModule(SetupContext context) {
            super.setupModule(context);
            context.insertAnnotationIntrospector(new ConflictiveSetter());
          }
        });
    /* Java8/Optional: */
    modules.add(new Jdk8Module());
    modules.forEach(module -> mappers.forEach(mapper -> mapper.registerModule(module)));
    /* Set class loader: */
    mappers.forEach(
        mapper -> mapper.setTypeFactory(mapper.getTypeFactory().withClassLoader(classLoader)));
    /* Mixin */
    mappers.forEach(MixinHook::mixin);
  }

  public Collection<Schema> schemas() {
    return schemas.values().stream().map(ref -> ref.schema).collect(Collectors.toList());
  }

  public JavaDocParser javadoc() {
    return javadocParser;
  }

  public Schema schema(Class type) {
    if (isVoid(type.getName())) {
      return null;
    }
    if (type == String.class) {
      return new StringSchema();
    }
    if (type == Byte.class || type == byte.class) {
      return new IntegerSchema()
          .minimum(BigDecimal.valueOf(Byte.MIN_VALUE))
          .maximum(BigDecimal.valueOf(Byte.MAX_VALUE));
    }
    if (type == Character.class || type == char.class) {
      return new StringSchema().minLength(0).maxLength(1);
    }
    if (type == Boolean.class || type == boolean.class) {
      return new BooleanSchema();
    }
    if (type == Short.class || type == short.class) {
      return new IntegerSchema()
          .minimum(BigDecimal.valueOf(Short.MIN_VALUE))
          .maximum(BigDecimal.valueOf(Short.MAX_VALUE));
    }
    if (type == Integer.class || type == int.class) {
      return new IntegerSchema();
    }
    if (type == Long.class || type == long.class) {
      return new IntegerSchema().format("int64");
    }
    if (type == Float.class || type == float.class) {
      return new NumberSchema().format("float");
    }
    if (type == Double.class || type == double.class) {
      return new NumberSchema().format("double");
    }
    if (Set.class.isAssignableFrom(type)) {
      return new ArraySchema().uniqueItems(true);
    }
    if (Collection.class.isAssignableFrom(type)) {
      return new ArraySchema();
    }
    if (File.class.isAssignableFrom(type)
        || Path.class.isAssignableFrom(type)
        || InputStream.class.isAssignableFrom(type)) {
      return new BinarySchema();
    }
    if (FileUpload.class == type) {
      return new FileSchema();
    }
    if (Reader.class.isAssignableFrom(type)) {
      return new StringSchema();
    }
    if (byte[].class == type || ByteBuffer.class == type) {
      return new ByteArraySchema();
    }
    if (UUID.class == type) {
      return new UUIDSchema();
    }
    if (URI.class == type || URL.class == type) {
      return new StringSchema().format(type.getSimpleName().toLowerCase());
    }
    if (BigInteger.class == type) {
      return new IntegerSchema().format("int64");
    }
    if (BigDecimal.class == type) {
      return new NumberSchema().format("decimal");
    }
    if (Date.class == type || LocalDate.class == type) {
      return new DateSchema();
    }
    if (LocalDateTime.class == type
        || Instant.class == type
        || OffsetDateTime.class == type
        || ZonedDateTime.class == type) {
      return new DateTimeSchema();
    }
    if (Period.class == type
        || Duration.class == type
        || Currency.class == type
        || Locale.class == type) {
      return new StringSchema();
    }
    if (type.isArray()) {
      return new ArraySchema();
    }
    if (Map.class.isAssignableFrom(type)) {
      return new MapSchema();
    }
    if (type == Object.class || type == void.class || type == Void.class) {
      return new ObjectSchema();
    }
    if (type.isEnum()) {
      var schema = new EnumSchema();
      EnumSet.allOf(type).forEach(e -> schema.addEnumItem(((Enum) e).name()));
      return schema;
    }
    SchemaRef schemaRef = schemas.get(type.getName());
    if (schemaRef == null) {
      var resolvedSchema = converters.readAllAsResolvedSchema(type);
      if (resolvedSchema.schema == null) {
        throw new IllegalArgumentException("Unsupported type: " + type);
      }
      schemaRef =
          new SchemaRef(
              resolvedSchema.schema, RefUtils.constructRef(resolvedSchema.schema.getName()));
      schemas.put(type.getName(), schemaRef);
      document(type, resolvedSchema.schema, resolvedSchema);
      if (resolvedSchema.referencedSchemas != null) {
        for (var e : resolvedSchema.referencedSchemas.entrySet()) {
          if (!e.getKey().equals(schemaRef.schema.getName())) {
            SchemaRef dependency =
                new SchemaRef(e.getValue(), RefUtils.constructRef(e.getValue().getName()));
            schemas.putIfAbsent(e.getKey(), dependency);
          }
        }
        for (var e : resolvedSchema.referencedSchemasByType.entrySet()) {
          var qualifiedTypeName = toClass(e.getKey());
          if (qualifiedTypeName instanceof Class<?> classType) {
            document(classType, e.getValue(), resolvedSchema);
          }
        }
      }
    }
    return schemaRef.toSchema();
  }

  private java.lang.reflect.Type toClass(java.lang.reflect.Type type) {
    if (type instanceof Class) {
      return type;
    }
    if (type instanceof SimpleType simpleType) {
      return simpleType.getRawClass();
    }
    return type;
  }

  private void document(Class typeName, Schema schema, ResolvedSchemaExt resolvedSchema) {
    javadocParser
        .parse(typeName.getName())
        .ifPresent(
            javadoc -> {
              Optional.ofNullable(javadoc.getText()).ifPresent(schema::setDescription);
              // make a copy
              Map<String, Schema> properties = schema.getProperties();
              if (properties != null) {
                new LinkedHashMap<>(properties)
                    .forEach(
                        (key, value) -> {
                          var text = javadoc.getPropertyDoc(key);
                          var propertyType = getPropertyType(typeName, key);
                          var isEnum =
                              propertyType != null
                                  && propertyType.isEnum()
                                  && resolvedSchema.referencedSchemasByType.keySet().stream()
                                      .map(this::toClass)
                                      .anyMatch(it -> !it.equals(propertyType));
                          if (isEnum) {
                            javadocParser
                                .parse(propertyType.getName())
                                .ifPresent(
                                    enumDoc -> {
                                      var enumDesc = enumDoc.getEnumDescription(text);
                                      if (enumDesc != null) {
                                        EnumSchema enumSchema;
                                        if (!(value instanceof EnumSchema)) {
                                          enumSchema = new EnumSchema();
                                          value.getEnum().stream()
                                              .forEach(
                                                  enumValue ->
                                                      enumSchema.addEnumItemObject(
                                                          enumValue.toString()));
                                          properties.put(key, enumSchema);
                                        } else {
                                          enumSchema = (EnumSchema) value;
                                        }
                                        for (var field : enumSchema.getEnum()) {
                                          var enumItemDesc = enumDoc.getEnumItemDescription(field);
                                          if (enumItemDesc != null) {
                                            enumSchema.setDescription(field, enumItemDesc);
                                          }
                                        }
                                        enumSchema.setDescription(enumDesc);
                                      }
                                    });
                          } else {
                            value.setDescription(text);
                            var example = javadoc.getPropertyExample(key);
                            if (example != null) {
                              value.setExample(example);
                            }
                          }
                        });
              }
            });
  }

  public Class getPropertyType(Class clazz, String name) {
    Class type = null;
    while (type == null && clazz != Object.class) {
      type = getGetter(clazz, List.of(name, getName(name)));
      if (type == null) {
        type = getField(clazz, name);
      }
      clazz = clazz.getSuperclass();
    }
    return type;
  }

  private Class<?> getField(Class clazz, String name) {
    try {
      return clazz.getDeclaredField(name).getType();
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  private Class<?> getGetter(Class clazz, List<String> names) {
    for (String name : names) {
      try {
        return clazz.getDeclaredMethod(name).getReturnType();
      } catch (NoSuchMethodException ignored) {
      }
    }
    return null;
  }

  private String getName(String name) {
    return "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }

  public Optional<SchemaRef> schemaRef(String type) {
    return Optional.ofNullable(schemas.get(type));
  }

  public Schema schema(Type type) {
    // For array we need internal name :S
    return schema(isArray(type) ? type.getInternalName() : type.getClassName());
  }

  private boolean isArray(Type type) {
    return type.getDescriptor().charAt(0) == '[';
  }

  /**
   * TODO: This method should be private and replaced with {@link #schema(Type)}
   *
   * <p>There are some difference on how to handle array of primitives vs normal class names See
   * https://github.com/jooby-project/jooby/issues/2542
   */
  public Schema schema(String type) {
    if (isVoid(type)) {
      return null;
    }
    SchemaRef schema = schemas.get(type);
    if (schema != null) {
      return schema.toSchema();
    }
    String json = "{\"type\":\"" + type + "\"}";
    try {
      TypeLiteral literal = json().readValue(json, TypeLiteral.class);
      return schema(literal.type);
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private Schema schema(JavaType type) {
    if (type.isArrayType() && type.getContentType().hasRawClass(byte.class)) {
      return new ByteArraySchema();
    } else if (type.isCollectionLikeType() || type.isArrayType()) {
      ArraySchema array = new ArraySchema();
      Optional.ofNullable(schema(type.getContentType())).ifPresent(array::setItems);
      return array;
    } else if (type.getRawClass() == Optional.class) {
      List<JavaType> typeParameters = type.getBindings().getTypeParameters();
      return schema(typeParameters.get(0));
    } else if (type.isMapLikeType()) {
      MapSchema mapSchema = new MapSchema();
      mapSchema.setAdditionalProperties(schema(type.getContentType()));
      return mapSchema;
    } else if (type.getRawClass() == Page.class) {
      // must be embedded it mimics a List<T>. This is bc it might have a different item type
      // per operation.
      var pageSchema = converters.read(type.getRawClass()).get("Page");
      // force loading of PageRequest
      schema(PageRequest.class);

      var params = type.getBindings().getTypeParameters();
      if (params != null && !params.isEmpty()) {
        Schema<?> contentSchema = (Schema<?>) pageSchema.getProperties().get("content");
        contentSchema.setItems(schema(params.getFirst()));
      }
      return pageSchema;
    }
    return schema(type.getRawClass());
  }

  private boolean isVoid(String type) {
    return Context.class.getName().equals(type)
        || void.class.getName().equals(type)
        || Void.class.getName().equals(type)
        || StatusCode.class.getName().equals(type);
  }

  public ClassNode classNode(Type type) {
    return nodes.computeIfAbsent(type, this::newClassNode);
  }

  public MethodNode findMethodNode(Type type, String name) {
    return nodes.computeIfAbsent(type, this::newClassNode).methods.stream()
        .filter(it -> it.name.equals(name))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Method not found: " + type + "." + name));
  }

  public ClassNode classNodeOrNull(Type type) {
    try {
      return nodes.computeIfAbsent(type, this::newClassNode);
    } catch (Exception x) {
      return null;
    }
  }

  public byte[] loadResource(String path) throws IOException {
    return source.loadResource(path);
  }

  private ClassNode newClassNode(Type type) {
    ClassReader reader = new ClassReader(source.loadClass(type.getClassName()));
    if (debug.contains(DebugOption.ALL)) {
      debug(reader);
    }
    ClassNode node = createClassVisitor(ClassNode::new);
    reader.accept(node, 0);
    return node;
  }

  private void debug(ClassReader reader) {
    Printer printer = new ASMifier();
    PrintWriter output = new PrintWriter(System.out);
    TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, printer, output);
    reader.accept(traceClassVisitor, 0);
  }

  public void debugHandler(MethodNode node) {
    if (debug.contains(DebugOption.HANDLER)) {
      debug(node);
    }
  }

  private void debug(MethodNode node) {
    System.out.println(Type.getReturnType(node.desc).getClassName() + " " + node.name + " {");
    Printer printer = new ASMifier();
    TraceMethodVisitor traceClassVisitor = new TraceMethodVisitor(null, printer);
    node.accept(traceClassVisitor);
    PrintWriter writer = new PrintWriter(System.out);
    printer.print(writer);
    writer.flush();
    System.out.println("}");
  }

  public void debugHandlerLink(MethodNode node) {
    if (debug.contains(DebugOption.HANDLER_LINK)) {
      debug(node);
    }
  }

  public Type getRouter() {
    return router;
  }

  public ObjectMapper json() {
    return json;
  }

  public ObjectMapper yaml() {
    return yaml;
  }

  public <T extends ClassVisitor> T createClassVisitor(Function<Integer, T> factory) {
    return factory.apply(Opcodes.ASM9);
  }

  public boolean isRouter(Type type) {
    return asList(router, JOOBY, KOOBY, ROUTER, COROUTINE_ROUTER).contains(type)
        || (router.getClassName() + "Kt").equals(type.getClassName());
  }

  public boolean process(AbstractInsnNode instruction) {
    return instructions.add(instruction);
  }

  public ParserContext newContext(Type router) {
    return new ParserContext(specVersion, json, yaml, source, nodes, router, javadocParser, debug);
  }

  public String getMainClass() {
    return mainClass;
  }

  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }
}
