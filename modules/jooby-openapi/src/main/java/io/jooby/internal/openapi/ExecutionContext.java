package io.jooby.internal.openapi;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.objectweb.asm.AnnotationVisitor;
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

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.jooby.internal.openapi.TypeFactory.COROUTINE_ROUTER;
import static io.jooby.internal.openapi.TypeFactory.JOOBY;
import static io.jooby.internal.openapi.TypeFactory.KOOBY;
import static io.jooby.internal.openapi.TypeFactory.ROUTER;

public class ExecutionContext {

  public static class TypeLiteral {
    public JavaType type;
  }

  private final Type router;
  private final Map<Type, ClassNode> nodes;
  private final ClassSource source;
  private final Set<Object> instructions = new HashSet<>();
  private final Set<DebugOption> debug;
  private final ConcurrentMap<String, SchemaRef> schemas = new ConcurrentHashMap<>();

  public ExecutionContext(ClassSource source, Type router, Set<DebugOption> debug) {
    this(source, new HashMap<>(), router, debug);
  }

  private ExecutionContext(ClassSource source, Map<Type, ClassNode> nodes, Type router,
      Set<DebugOption> debug) {
    this.router = router;
    this.source = source;
    this.debug = debug;
    this.nodes = nodes;
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
      return new StringSchema()
          .minLength(0)
          .maxLength(1);
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
    if (type == Object.class || type == void.class || type == Void.class) {
      return new ObjectSchema();
    }
    return schemas.computeIfAbsent(type.getName(), k -> {
      ResolvedSchema resolvedSchema = ModelConverters.getInstance().readAllAsResolvedSchema(type);
      return new SchemaRef(resolvedSchema.schema,
          RefUtils.constructRef(resolvedSchema.schema.getName()));
    }).toSchema();
  }

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
      if (literal.type.isCollectionLikeType()) {
        ArraySchema array = new ArraySchema();
        Class<?> itemType = literal.type.getContentType().getRawClass();
        array.setItems(schema(itemType));
        return array;
      } else if (literal.type.getRawClass() == Optional.class) {
        List<JavaType> typeParameters = literal.type.getBindings().getTypeParameters();
        Class<?> itemType = typeParameters.get(0).getRawClass();
        return schema(itemType);
      }
      return schema(literal.type.getRawClass());
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private boolean isVoid(String type) {
    return Context.class.getName().equals(type) || void.class.getName().equals(type) || Void.class.getName().equals(type);
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

  private ClassNode newClassNode(Type type) {
    ClassReader reader = new ClassReader(source.byteCode(type.getClassName()));
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
    return factory.apply(Opcodes.ASM7);
  }

  public boolean isRouter(Type type) {
    return Stream.of(router, JOOBY, KOOBY, ROUTER, COROUTINE_ROUTER)
        .anyMatch(it -> it.equals(type));
  }

  public boolean process(AbstractInsnNode instruction) {
    return instructions.add(instruction);
  }

  public ExecutionContext newContext(Type router) {
    ExecutionContext ctx = new ExecutionContext(source, nodes, router, debug);
    return ctx;
  }
}
