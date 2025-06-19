/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i1570;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;
import io.jooby.test.MockSession;
import io.jooby.value.Value;

public class Issue1570 {

  @Test
  public void shouldAllowToSetSessionValueUsingMockingLib() {
    Value value = mock(Value.class);
    when(value.value()).thenReturn("ClientVader");
    Session session = mock(Session.class);
    when(session.get("handle")).thenReturn(value);
    Context ctx = mock(Context.class);
    when(ctx.session()).thenReturn(session);

    MockRouter router = new MockRouter(new App1570());
    assertEquals(
        "{\"clientName\": \"ClientVader\"}", router.get("/registerClient/ClientVader").value());
    assertEquals("{\"clientName\": \"ClientVader\"}", router.get("/clientName", ctx).value());
  }

  @Test
  public void shouldAllowToSetSessionValueUsingSessionMock() {
    MockContext ctx = new MockContext();

    MockSession session = new MockSession(ctx);
    session.put("handle", "ClientVader");

    MockRouter router = new MockRouter(new App1570());
    assertEquals(
        "{\"clientName\": \"ClientVader\"}", router.get("/registerClient/ClientVader").value());

    assertEquals("{\"clientName\": \"ClientVader\"}", router.get("/clientName", ctx).value());
  }

  @Test
  public void shouldAllowToSetSessionValueUsingGlobalSession() {
    MockRouter router = new MockRouter(new App1570());
    router.setSession(new MockSession());

    assertEquals(
        "{\"clientName\": \"ClientVader\"}", router.get("/registerClient/ClientVader").value());

    assertEquals("{\"clientName\": \"ClientVader\"}", router.get("/clientName").value());
  }
}
