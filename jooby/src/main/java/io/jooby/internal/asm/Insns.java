/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
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
