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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.openapi.DebugOption;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;

public class ParserContext {

  public static class TypeLiteral {
    public JavaType type;

    public TypeLiteral() {}
  }

  private String mainClass;
  private final ModelConverters converters;
  private final Type router;
  private final Map<Type, ClassNode> nodes;
  private final ClassSource source;
  private final Set<Object> instructions = new HashSet<>();
  private final Set<DebugOption> debug;
  private final ConcurrentMap<String, SchemaRef> schemas = new ConcurrentHashMap<>();

  public ParserContext(ClassSource source, Type router, Set<DebugOption> debug) {
    this(source, new HashMap<>(), router, debug);
  }

  private ParserContext(
      ClassSource source, Map<Type, ClassNode> nodes, Type router, Set<DebugOption> debug) {
    this.router = router;
    this.source = source;
    this.debug = Optional.ofNullable(debug).orElse(Collections.emptySet());
    this.nodes = nodes;

    List<ObjectMapper> mappers = asList(Json.mapper(), Yaml.mapper());
    jacksonModules(source.getClassLoader(), mappers);
    this.converters = ModelConverters.getInstance();
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
    /** Java8/Optional: */
    modules.add(new Jdk8Module());
    modules.forEach(module -> mappers.forEach(mapper -> mapper.registerModule(module)));
    /** Set class loader: */
    mappers.stream()
        .forEach(
            mapper -> mapper.setTypeFactory(mapper.getTypeFactory().withClassLoader(classLoader)));
  }

  public Collection<Schema> schemas() {
    return schemas.values().stream().map(ref -> ref.schema).collect(Collectors.toList());
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
      return new IntegerSchema().format(null);
    }
    if (BigDecimal.class == type) {
      return new NumberSchema().format(null);
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
      StringSchema schema = new StringSchema();
      EnumSet.allOf(type).forEach(e -> schema.addEnumItem(((Enum) e).name()));
      return schema;
    }
    SchemaRef schemaRef = schemas.get(type.getName());
    if (schemaRef == null) {
      ResolvedSchema resolvedSchema = converters.readAllAsResolvedSchema(type);
      if (resolvedSchema.schema == null) {
        throw new IllegalArgumentException("Unsupported type: " + type);
      }
      schemaRef =
          new SchemaRef(
              resolvedSchema.schema, RefUtils.constructRef(resolvedSchema.schema.getName()));
      schemas.put(type.getName(), schemaRef);

      if (resolvedSchema.referencedSchemas != null) {
        for (Map.Entry<String, Schema> e : resolvedSchema.referencedSchemas.entrySet()) {
          if (!e.getKey().equals(schemaRef.schema.getName())) {
            SchemaRef dependency =
                new SchemaRef(e.getValue(), RefUtils.constructRef(e.getValue().getName()));
            schemas.putIfAbsent(e.getKey(), dependency);
          }
        }
      }
    }
    return schemaRef.toSchema();
  }

  public Optional<SchemaRef> schemaRef(String type) {
    return Optional.ofNullable(schemas.get(type));
  }

  public Schema schema(Type type) {
    if (isArray(type)) {
      // For array we need internal name :S
      return schema(type.getInternalName());
    } else {
      return schema(type.getClassName());
    }
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
      TypeLiteral literal = Json.mapper().readValue(json, TypeLiteral.class);
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

  public <T extends ClassVisitor> T createClassVisitor(Function<Integer, T> factory) {
    return factory.apply(Opcodes.ASM9);
  }

  public boolean isRouter(Type type) {
    return asList(router, JOOBY, KOOBY, ROUTER, COROUTINE_ROUTER).contains(type);
  }

  public boolean process(AbstractInsnNode instruction) {
    return instructions.add(instruction);
  }

  public ParserContext newContext(Type router) {
    return new ParserContext(source, nodes, router, debug);
  }

  public String getMainClass() {
    return mainClass;
  }

  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }
}
