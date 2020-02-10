package io.jooby.internal.openapi;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.jooby.internal.openapi.TypeFactory.COROUTINE_ROUTER;
import static io.jooby.internal.openapi.TypeFactory.JOOBY;
import static io.jooby.internal.openapi.TypeFactory.KOOBY;
import static io.jooby.internal.openapi.TypeFactory.ROUTER;

public class ExecutionContext {
  private final Type router;
  private final Map<Type, ClassNode> nodes;
  private final ClassSource source;
  private final Set<Object> instructions = new HashSet<>();
  private final Set<DebugOption> debug;

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
