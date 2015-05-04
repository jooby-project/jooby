package org.jooby.internal.elasticsearch;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.jooby.MockUnit;
import org.junit.Test;

public class ManagedClientTest {

  private MockUnit.Block start = unit -> {
    Node node = unit.get(Node.class);
    expect(node.client()).andReturn(unit.get(Client.class));

    NodeBuilder nb = unit.get(NodeBuilder.class);
    expect(nb.build()).andReturn(node);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(NodeBuilder.class)
        .run(unit -> {
          new ManagedClient(new ManagedNode(unit.get(NodeBuilder.class)));
        });
  }

  @Test
  public void start() throws Exception {
    new MockUnit(NodeBuilder.class, Client.class, Node.class)
        .expect(start)
        .run(unit -> {
          ManagedClient client = new ManagedClient(new ManagedNode(unit.get(NodeBuilder.class)));
          client.start();
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(NodeBuilder.class, Client.class, Node.class)
        .expect(start)
        .run(unit -> {
          ManagedClient client = new ManagedClient(new ManagedNode(unit.get(NodeBuilder.class)));
          client.start();
          assertEquals(unit.get(Client.class), client.get());
        });
  }

  @Test
  public void stop() throws Exception {
    new MockUnit(NodeBuilder.class, Client.class, Node.class)
        .expect(start)
        .expect(unit -> {
          Client client = unit.get(Client.class);
          client.close();
        })
        .run(unit -> {
          ManagedClient client = new ManagedClient(new ManagedNode(unit.get(NodeBuilder.class)));
          client.start();
          client.stop();
          client.stop();
        });
  }

}
