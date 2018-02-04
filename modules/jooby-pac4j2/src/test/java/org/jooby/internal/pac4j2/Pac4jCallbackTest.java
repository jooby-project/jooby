package org.jooby.internal.pac4j2;

import static org.easymock.EasyMock.expect;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.http.HttpActionAdapter;

public class Pac4jCallbackTest {

  private MockUnit.Block webContext = unit -> {
    Request req = unit.get(Request.class);
    expect(req.require(WebContext.class)).andReturn(unit.get(WebContext.class));
  };

  private MockUnit.Block callback = unit -> {
    Config config = unit.get(Config.class);
    expect(config.getCallbackLogic()).andReturn(unit.get(CallbackLogic.class));
  };

  private MockUnit.Block actionAdapter = unit -> {
    Config config = unit.get(Config.class);
    expect(config.getHttpActionAdapter()).andReturn(unit.get(HttpActionAdapter.class));
  };

  @Test
  public void shouldExecuteCallback() throws Exception {
    String defaultUrl = "/myapp";
    boolean multiProfile = true;
    boolean renewSession = false;
    new MockUnit(Request.class, Response.class, Config.class, WebContext.class, CallbackLogic.class,
        HttpActionAdapter.class)
        .expect(webContext)
        .expect(callback)
        .expect(actionAdapter)
        .expect(execute(defaultUrl, multiProfile, renewSession))
        .run(unit -> {
          new Pac4jCallback(unit.get(Config.class), defaultUrl, multiProfile, renewSession)
              .handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  private MockUnit.Block execute(String defaultUrl, boolean multiProfile, boolean renewSession) {
    return unit -> {
      CallbackLogic callback = unit.get(CallbackLogic.class);
      expect(callback.perform(unit.get(WebContext.class), unit.get(Config.class),
          unit.get(HttpActionAdapter.class), defaultUrl, multiProfile, renewSession))
          .andReturn(null);
    };
  }
}
