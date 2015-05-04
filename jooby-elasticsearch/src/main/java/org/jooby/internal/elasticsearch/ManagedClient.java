package org.jooby.internal.elasticsearch;

import static java.util.Objects.requireNonNull;

import javax.inject.Provider;

import org.elasticsearch.client.Client;
import org.jooby.Managed;

public class ManagedClient implements Managed, Provider<Client> {

  private ManagedNode node;

  private Client client;

  public ManagedClient(final ManagedNode node) {
    this.node = requireNonNull(node, "A node is required.");
  }

  @Override
  public Client get() {
    return client;
  }

  @Override
  public void start() throws Exception {
    this.client = node.get().client();
  }

  @Override
  public void stop() throws Exception {
    if (this.client != null) {
      this.client.close();
      this.client = null;
    }
  }

}
