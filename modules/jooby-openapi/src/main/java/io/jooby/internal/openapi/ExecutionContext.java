package io.jooby.internal.openapi;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
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

public class ExecutionContext {
  public final ClassNode root;
  private final Type mainType;
  private final Map<Type, ClassNode> nodes = new HashMap<>();
  private final ClassSource source;
  private final Set<Object> instructions = new HashSet<>();
  private final Set<Type> routers = new HashSet<>();
  private final Set<DebugOption> debug;

  public ExecutionContext(ClassSource source, Type mainType, Set<DebugOption> debug) {
    this.mainType = mainType;
    this.source = source;
    this.debug = debug;
    this.root = newClassNode(mainType);
  }

  public ClassNode classNode(Type type) {
    if (type.equals(mainType)) {
      return root;
    }
    return nodes.computeIfAbsent(type, this::newClassNode);
  }

  private ClassNode newClassNode(Type type) {
    ClassReader reader = new ClassReader(source.byteCode(type.getClassName()));
    if (debug.contains(DebugOption.CLASS)) {
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
    Printer printer = new ASMifier();
    TraceMethodVisitor traceClassVisitor = new TraceMethodVisitor(null, printer);
    node.accept(traceClassVisitor);
    PrintWriter writer = new PrintWriter(System.out);
    printer.print(writer);
    writer.flush();
  }

  public void debugHandlerLink(MethodNode node) {
    if (debug.contains(DebugOption.HANDLER_LINK)) {
      debug(node);
    }
  }

  public Type getMainType() {
    return mainType;
  }

  public void addRouter(Type router) {
    routers.add(router);
  }

  public void removeRouter(Type route) {
    for (MethodNode method : classNode(route).methods) {
      for (AbstractInsnNode instruction : method.instructions) {
        this.instructions.remove(instruction);
      }
    }
    routers.remove(route);
  }

  public <T extends ClassVisitor> T createClassVisitor(Function<Integer, T> factory) {
    return factory.apply(Opcodes.ASM7);
  }

  public boolean isRouter(Type type) {
    return Stream.concat(Stream.of(mainType, TypeFactory.JOOBY, TypeFactory.KOOBY, TypeFactory.ROUTER,
        TypeFactory.COROUTINE_ROUTER), routers.stream())
        .anyMatch(it -> it.equals(type));
  }

  public boolean process(AbstractInsnNode instruction) {
    return instructions.add(instruction);
  }
}
