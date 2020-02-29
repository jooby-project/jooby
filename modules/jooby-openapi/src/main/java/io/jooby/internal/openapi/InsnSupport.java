/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class InsnSupport {

  private static class NodeIterator implements Iterator<org.objectweb.asm.tree.AbstractInsnNode> {

    private final AbstractInsnNode to;
    private AbstractInsnNode node;
    private Function<AbstractInsnNode, AbstractInsnNode> next;

    public NodeIterator(final AbstractInsnNode node,
        final Function<AbstractInsnNode, AbstractInsnNode> next) {
      this(node, null, next);
    }

    public NodeIterator(final AbstractInsnNode node, AbstractInsnNode to,
        final Function<AbstractInsnNode, AbstractInsnNode> next) {
      this.node = node;
      this.next = next;
      this.to = to;
    }

    @Override
    public boolean hasNext() {
      return node != to;
    }

    @Override
    public org.objectweb.asm.tree.AbstractInsnNode next() {
      org.objectweb.asm.tree.AbstractInsnNode it = node;
      node = next.apply(node);
      return it;
    }
  }

  public static <N extends AbstractInsnNode> Predicate<N> opcode(int opcode) {
    return n -> n.getOpcode() == opcode;
  }

  public static <N extends AbstractInsnNode> Predicate<N> varInsn(int opcode, int var) {
    return n -> n.getOpcode() == opcode && ((VarInsnNode) n).var == var;
  }

  public static Iterator<AbstractInsnNode> prevIterator(AbstractInsnNode node) {
    return new NodeIterator(node, AbstractInsnNode::getPrevious);
  }

  public static Stream<AbstractInsnNode> prev(AbstractInsnNode node) {
    return StreamSupport
        .stream(Spliterators.spliteratorUnknownSize(prevIterator(node), Spliterator.ORDERED),
            false);
  }

  public static Stream<AbstractInsnNode> prev(AbstractInsnNode from, AbstractInsnNode to) {
    return StreamSupport
        .stream(Spliterators
                .spliteratorUnknownSize(new NodeIterator(from, to, AbstractInsnNode::getPrevious),
                    Spliterator.ORDERED),
            false);
  }

  public static Stream<AbstractInsnNode> next(AbstractInsnNode node) {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new NodeIterator(node,
        AbstractInsnNode::getNext), Spliterator.ORDERED), false);
  }

  public static String toString(InvokeDynamicInsnNode node) {
    Handle handle = (Handle) node.bsmArgs[1];
    return handle.getOwner() + "." + node.name + node.desc;
  }

  public static String toString(MethodInsnNode node) {
    return node.owner + "." + node.name + node.desc;
  }
}
