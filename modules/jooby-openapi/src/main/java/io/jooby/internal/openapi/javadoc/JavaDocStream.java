/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;

public final class JavaDocStream {

  public static Predicate<DetailAST> tokens(Integer... types) {
    return tokens(Set.of(types));
  }

  private static Predicate<DetailAST> tokens(Set<Integer> types) {
    return it -> types.contains(it.getType());
  }

  public static Predicate<DetailNode> javadocToken(Integer... types) {
    return javadocToken(Set.of(types));
  }

  private static Predicate<DetailNode> javadocToken(Set<Integer> types) {
    return it -> types.contains(it.getType());
  }

  /**
   * Traverse the tree from current node to parent (backward).
   *
   * @param node Starting point
   * @return Stream.
   */
  public static Stream<DetailAST> backward(DetailAST node) {
    return backward(ASTNode.ast(node));
  }

  /**
   * Traverse the tree from the current node to children and sibling (forward).
   *
   * @param node Starting point
   * @return Stream.
   */
  public static Stream<DetailAST> forward(DetailAST node) {
    return forward(ASTNode.ast(node));
  }

  /**
   * Traverse the tree from the current node to children and sibling (forward) but keeping the scope
   * to the given node (root).
   *
   * @param node Root node.
   * @return Stream.
   */
  public static Stream<DetailAST> tree(DetailAST node) {
    return tree(ASTNode.ast(node));
  }

  public static Stream<DetailNode> tree(DetailNode node) {
    return tree(ASTNode.javadoc(node));
  }

  public static Stream<DetailAST> children(DetailAST node) {
    return stream(childrenIterator(ASTNode.ast(node)));
  }

  public static Stream<DetailNode> children(DetailNode node) {
    return stream(childrenIterator(ASTNode.javadoc(node)));
  }

  public static Stream<DetailNode> backward(DetailNode node) {
    return backward(ASTNode.javadoc(node));
  }

  public static Stream<DetailNode> forward(DetailNode node, Predicate<DetailNode> stopOn) {
    var nodes = forward(ASTNode.javadoc(node)).toList();
    var result = new ArrayList<DetailNode>();
    for (var it : nodes) {
      if (stopOn.test(it)) {
        break;
      }
      result.add(it);
    }
    return result.stream();
  }

  private static <T> Stream<T> backward(ASTNode<T> node) {
    return stream(backwardIterator(node));
  }

  private static <T> Stream<T> forward(ASTNode<T> node) {
    return stream(forwardIterator(node));
  }

  private static <T> Stream<T> tree(ASTNode<T> node) {
    return stream(treeIterator(node));
  }

  private static <T> Stream<T> stream(Iterator<T> iterator) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
  }

  private static <T> Iterator<T> treeIterator(ASTNode<T> node) {
    return forwardIterator(node, false);
  }

  private static <T> Iterator<T> forwardIterator(ASTNode<T> node) {
    return forwardIterator(node, true);
  }

  private static <T> Iterator<T> childrenIterator(ASTNode<T> node) {
    return new Iterator<>() {
      Function<ASTNode<T>, ASTNode<T>> direction = null;
      ASTNode<T> it = node;

      @Override
      public boolean hasNext() {
        if (direction == null) {
          direction = ASTNode::getFirstChild;
        } else {
          direction = ASTNode::getNextSibling;
        }
        return direction.apply(it) != null;
      }

      @Override
      public T next() {
        it = direction.apply(it);
        return it.getNode();
      }
    };
  }

  private static <T> Iterator<T> backwardIterator(ASTNode<T> node) {
    return new Iterator<>() {
      ASTNode<T> it = node;

      @Override
      public boolean hasNext() {
        return it.getNode() != null && it.getParent() != null;
      }

      @Override
      public T next() {
        it = it.getParent();
        return it.getNode();
      }
    };
  }

  private static <T> Iterator<T> forwardIterator(ASTNode<T> node, boolean full) {
    return new Iterator<>() {
      ASTNode<T> it = node;
      final Stack<ASTNode<T>> stack = new Stack<>();

      @Override
      public boolean hasNext() {
        return it != null && it.getNode() != null;
      }

      @Override
      public T next() {
        var sibling = it.getNextSibling();
        if (sibling != null) {
          if (full || it != node) {
            stack.push(sibling);
          }
        }
        var current = it;
        var child = it.getFirstChild();
        if (child == null) {
          if (!stack.isEmpty()) {
            it = stack.pop();
          } else {
            it = null;
          }
        } else {
          it = child;
        }
        return current.getNode();
      }
    };
  }

  private interface ASTNode<Node> {
    ASTNode<Node> getFirstChild();

    ASTNode<Node> getNextSibling();

    ASTNode<Node> getParent();

    Node getNode();

    static ASTNode<DetailAST> ast(DetailAST node) {
      return ast(node, DetailAST::getParent, DetailAST::getFirstChild, DetailAST::getNextSibling);
    }

    static ASTNode<DetailNode> javadoc(DetailNode node) {
      return ast(
          node, DetailNode::getParent, DetailNode::getFirstChild, DetailNode::getNextSibling);
    }

    static <N> ASTNode<N> ast(
        N node, Function<N, N> parent, Function<N, N> child, Function<N, N> sibling) {
      return new ASTNode<>() {

        private ASTNode<N> apply(Function<N, N> fn) {
          if (node == null) {
            return null;
          }
          var result = fn.apply(node);
          if (result == null) {
            return null;
          }
          return ast(result, parent, child, sibling);
        }

        @Override
        public ASTNode<N> getParent() {
          return apply(parent);
        }

        @Override
        public ASTNode<N> getFirstChild() {
          return apply(child);
        }

        @Override
        public N getNode() {
          return node;
        }

        @Override
        public ASTNode<N> getNextSibling() {
          return apply(sibling);
        }
      };
    }
  }
}
