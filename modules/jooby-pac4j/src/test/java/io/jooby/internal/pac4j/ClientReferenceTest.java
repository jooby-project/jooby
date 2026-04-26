/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.pac4j.core.client.Client;

public class ClientReferenceTest {

  @Test
  void testConstructorWithClient() {
    Client client = mock(Client.class);
    ClientReference ref = new ClientReference(client);

    assertTrue(ref.isResolved());
    assertEquals(client, ref.getClient());

    // Resolve should be a no-op if already resolved
    ref.resolve(type -> null);
    assertEquals(client, ref.getClient());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testConstructorWithClassAndResolution() {
    Class<Client> clientClass = (Class) Client.class;
    ClientReference ref = new ClientReference(clientClass);

    assertFalse(ref.isResolved());
    assertThrows(IllegalStateException.class, ref::getClient);

    Client client = mock(Client.class);
    ref.resolve(
        type -> {
          assertEquals(clientClass, type);
          return client;
        });

    assertTrue(ref.isResolved());
    assertEquals(client, ref.getClient());
  }

  @Test
  void testRequireNonNull() {
    assertThrows(NullPointerException.class, () -> new ClientReference((Client) null));
    assertThrows(NullPointerException.class, () -> new ClientReference((Class<Client>) null));
  }

  @Test
  void testLazyClientNameList() {
    Client c1 = mock(Client.class);
    when(c1.getName()).thenReturn("Facebook");
    Client c2 = mock(Client.class);
    when(c2.getName()).thenReturn("Twitter");

    ClientReference ref1 = new ClientReference(c1);
    ClientReference ref2 = new ClientReference(c2);

    Supplier<String> supplier = ClientReference.lazyClientNameList(List.of(ref1, ref2));

    // First call computes
    assertEquals("Facebook,Twitter", supplier.get());
    // Second call uses memoized value
    assertEquals("Facebook,Twitter", supplier.get());

    // Verify name methods were called only once (memoization check)
    verify(c1, times(1)).getName();
    verify(c2, times(1)).getName();
  }

  @Test
  @SuppressWarnings("unchecked")
  void testLazyClientNameListUnresolved() {
    ClientReference ref = new ClientReference((Class) Client.class);
    Supplier<String> supplier = ClientReference.lazyClientNameList(List.of(ref));

    // Should throw because ref is not resolved yet
    assertThrows(IllegalStateException.class, supplier::get);
  }
}
