package org.jooby.internal.elasticsearch;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class ManagedNodeTest {

  private MockUnit.Block start = unit -> {
    Node node = unit.get(Node.class);
    expect(node.start()).andReturn(node);

    NodeBuilder nb = unit.get(NodeBuilder.class);
    expect(nb.build()).andReturn(node);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(NodeBuilder.class)
        .run(unit -> {
          new ManagedNode(unit.get(NodeBuilder.class));
        });
  }

  @Test
  public void start() throws Exception {
    new MockUnit(NodeBuilder.class, Node.class)
        .expect(start)
        .run(unit -> {
          ManagedNode node = new ManagedNode(unit.get(NodeBuilder.class));
          node.start();
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(NodeBuilder.class, Node.class)
        .expect(start)
        .run(unit -> {
          ManagedNode node = new ManagedNode(unit.get(NodeBuilder.class));
          node.start();
          assertEquals(unit.get(Node.class), node.get());
        });
  }

  @Test
  public void stop() throws Exception {
    new MockUnit(NodeBuilder.class, Node.class)
        .expect(start)
        .expect(unit -> {
          Node node = unit.get(Node.class);
          node.close();
        })
        .run(unit -> {
          ManagedNode node = new ManagedNode(unit.get(NodeBuilder.class));
          node.start();
          node.stop();
          node.stop();
        });
  }

}
