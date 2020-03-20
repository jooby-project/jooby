package issues;

import io.jooby.Context;
import io.jooby.MockContext;
import io.jooby.MockRouter;
import io.jooby.MockSession;
import io.jooby.Session;
import io.jooby.Value;
import issues.i1570.App1570;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    assertEquals("{\"clientName\": \"ClientVader\"}",
        router.get("/registerClient/ClientVader").value());
    assertEquals("{\"clientName\": \"ClientVader\"}", router.get("/clientName", ctx).value());
  }

  @Test
  public void shouldAllowToSetSessionValueUsingSessionMock() {
    MockContext ctx = new MockContext();

    MockSession session = new MockSession(ctx);
    session.put("handle", "ClientVader");

    MockRouter router = new MockRouter(new App1570());
    assertEquals("{\"clientName\": \"ClientVader\"}", router.get("/registerClient/ClientVader").value());

    assertEquals("{\"clientName\": \"ClientVader\"}", router.get("/clientName", ctx).value());
  }

  @Test
  public void shouldAllowToSetSessionValueUsingGlobalSession() {
    MockRouter router = new MockRouter(new App1570());
    router.setSession(new MockSession());

    assertEquals("{\"clientName\": \"ClientVader\"}", router.get("/registerClient/ClientVader").value());

    assertEquals("{\"clientName\": \"ClientVader\"}", router.get("/clientName").value());
  }
}
