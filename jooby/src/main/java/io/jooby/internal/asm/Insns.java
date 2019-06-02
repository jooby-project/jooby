/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;

public class Insns {

  private static class InsnIterator implements Iterator<AbstractInsnNode> {
    private AbstractInsnNode node;
    private Function<AbstractInsnNode, AbstractInsnNode> next;

    public InsnIterator(AbstractInsnNode node,
        Function<AbstractInsnNode, AbstractInsnNode> next) {
      this.node = node;
      this.next = next;
    }

    @Override public boolean hasNext() {
      return node != null;
    }

    @Override public AbstractInsnNode next() {
      AbstractInsnNode it = node;
      node = next.apply(it);
      return it;
    }
  }

  public static Stream<AbstractInsnNode> next(AbstractInsnNode node) {
    return stream(node, AbstractInsnNode::getNext);
  }

  public static Stream<AbstractInsnNode> previous(AbstractInsnNode node) {
    return stream(node, AbstractInsnNode::getPrevious);
  }

  public static Stream<AbstractInsnNode> last(InsnList node) {
    return previous(node.getLast());
  }

  private static Stream<AbstractInsnNode> stream(AbstractInsnNode node,
      Function<AbstractInsnNode, AbstractInsnNode> next) {
    Spliterator<AbstractInsnNode> iterator = spliteratorUnknownSize(
        new InsnIterator(node, next),
        ORDERED);
    return StreamSupport.stream(iterator, false);
  }
}
