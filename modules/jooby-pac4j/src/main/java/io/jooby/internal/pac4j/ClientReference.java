/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import org.pac4j.core.client.Client;
import org.pac4j.core.util.Pac4jConstants;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

public class ClientReference {

  private Class<Client> clientClass;
  private Client client;

  public ClientReference(Class<Client> clientClass) {
    this.clientClass = requireNonNull(clientClass);
  }

  public ClientReference(Client<?> client) {
    this.client = requireNonNull(client);
  }

  public Client getClient() {
    if (isResolved()) {
      return client;
    }
    throw new IllegalStateException("Client of class " + clientClass + " has not been resolved yet.");
  }

  public boolean isResolved() {
    return client != null;
  }

  public void resolve(Function<Class<Client>, Client> resolver) {
    if (!isResolved()) {
      client = resolver.apply(clientClass);
    }
  }

  /* Thread-safe memoized supplier that computes the client list exactly once;
   * all clients must be resolved at the time of invocation of the supplier. */
  public static Supplier<String> lazyClientNameList(List<ClientReference> references) {
    AtomicReference<String> value = new AtomicReference<>();
    return () -> {
      String val = value.get();
      if (val == null) {
        synchronized(value) {
          val = value.get();
          if (val == null) {
            val = references.stream()
                .map(ClientReference::getClient)
                .map(Client::getName)
                .collect(joining(Pac4jConstants.ELEMENT_SEPARATOR));
            value.set(val);
          }
        }
      }
      return val;
    };
  }
}
