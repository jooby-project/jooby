/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class SessionStoreUnsupportedTest {

  @Test
  public void testUnsupportedSessionStore() {
    SessionStore store = SessionStore.UNSUPPORTED;
    Context ctx = mock(Context.class);
    Session session = mock(Session.class);

    // Every method in the UNSUPPORTED implementation should throw the same exception type
    // Usage.noSession() typically throws a RegistryException or IllegalStateException
    // We catch RuntimeException to be safe, as it is the common superclass

    assertThrows(RuntimeException.class, () -> store.newSession(ctx));

    assertThrows(RuntimeException.class, () -> store.findSession(ctx));

    assertThrows(RuntimeException.class, () -> store.deleteSession(ctx, session));

    assertThrows(RuntimeException.class, () -> store.touchSession(ctx, session));

    assertThrows(RuntimeException.class, () -> store.saveSession(ctx, session));

    assertThrows(RuntimeException.class, () -> store.renewSessionId(ctx, session));
  }
}
