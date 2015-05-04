package org.jooby.internal.elasticsearch;

import static java.util.Objects.requireNonNull;

import javax.inject.Provider;

import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.jooby.Managed;

public class ManagedNode implements Provider<Node>, Managed {

  private NodeBuilder nb;

  private Node node;

  public ManagedNode(final NodeBuilder nb) {
    this.nb = requireNonNull(nb, "Node builder is required.");
  }

  @Override
  public void start() throws Exception {
    get().start();
  }

  @Override
  public void stop() throws Exception {
    if (node != null) {
      this.node.close();
      this.node = null;
    }
  }

  @Override
  public Node get() {
    if (node == null) {
      node = nb.build();
    }
    return node;
  }

}
