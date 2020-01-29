package io.jooby.internal.openapi;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

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
  private final boolean debug;

  public ExecutionContext(ClassSource source, Type mainType, boolean debug) {
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
    if (debug) {
      Printer printer = new ASMifier();
      PrintWriter output = new PrintWriter(System.out);
      TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, printer, output);
      reader.accept(traceClassVisitor, 0);
    }
    ClassNode node = createClassVisitor(ClassNode::new);
    reader.accept(node, 0);
    return node;
  }

  public Type getMainType() {
    return mainType;
  }

  public <T extends ClassVisitor> T createClassVisitor(Function<Integer, T> factory) {
    return factory.apply(Opcodes.ASM7);
  }

  public boolean isRouter(Type type) {
    return Stream.of(mainType, TypeFactory.JOOBY, TypeFactory.KOOBY, TypeFactory.ROUTER)
        .anyMatch(it -> it.equals(type));
  }

  public boolean process(AbstractInsnNode instruction) {
    return instructions.add(instruction);
  }
}
