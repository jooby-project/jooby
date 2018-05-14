package org.jooby.internal.pac4j2;

import static org.easymock.EasyMock.expect;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.LogoutLogic;
import org.pac4j.core.http.HttpActionAdapter;

import java.util.Optional;

public class Pac4jLogoutTest {

  private MockUnit.Block webContext = unit -> {
    Request req = unit.get(Request.class);
    expect(req.require(WebContext.class)).andReturn(unit.get(WebContext.class));
  };

  private MockUnit.Block getLogout = unit -> {
    LogoutLogic logout = unit.get(LogoutLogic.class);
    Config config = unit.get(Config.class);
    expect(config.getLogoutLogic()).andReturn(logout);
  };

  private MockUnit.Block getActionAdapter = unit -> {
    Config config = unit.get(Config.class);
    expect(config.getHttpActionAdapter()).andReturn(unit.get(HttpActionAdapter.class));
  };

  @Test
  public void shouldExecuteCallback() throws Exception {
    String defaultUrl = "/";
    String logoutUrlPattern = "/.*";
    boolean localLogout = true;
    boolean destroySession = true;
    boolean centralLogout = false;
    new MockUnit(Request.class, Response.class, Config.class, WebContext.class, LogoutLogic.class,
        HttpActionAdapter.class)
        .expect(webContext)
        .expect(getLogout)
        .expect(getActionAdapter)
        .expect(redirectTo(Optional.empty()))
        .expect(executeCallback(defaultUrl, logoutUrlPattern, localLogout, destroySession,
            centralLogout))
        .run(unit -> {
          Pac4jLogout action = new Pac4jLogout(unit.get(Config.class), defaultUrl, logoutUrlPattern,
              localLogout, destroySession, centralLogout);

          action.handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void shouldExecuteCallbackWithOptions() throws Exception {
    String defaultUrl = "/x";
    String logoutUrlPattern = "/.*xx";
    boolean localLogout = false;
    boolean destroySession = false;
    boolean centralLogout = true;
    new MockUnit(Request.class, Response.class, Config.class, WebContext.class, LogoutLogic.class,
        HttpActionAdapter.class)
        .expect(webContext)
        .expect(getLogout)
        .expect(getActionAdapter)
        .expect(redirectTo(Optional.empty()))
        .expect(executeCallback(defaultUrl, logoutUrlPattern, localLogout, destroySession,
            centralLogout))
        .run(unit -> {
          Pac4jLogout action = new Pac4jLogout(unit.get(Config.class), defaultUrl, logoutUrlPattern,
              localLogout, destroySession, centralLogout);

          action.handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  private MockUnit.Block executeCallback(String defaultUrl, String logoutUrlPattern,
      boolean localLogout, boolean destroySession, boolean centralLogout) {
    return unit -> {
      LogoutLogic logout = unit.get(LogoutLogic.class);

      expect(logout.perform(unit.get(WebContext.class), unit.get(Config.class),
          unit.get(HttpActionAdapter.class), defaultUrl, logoutUrlPattern, localLogout,
          destroySession, centralLogout)).andReturn(null);
    };
  }

  private MockUnit.Block redirectTo(Optional<String> redirectTo) {
    return unit -> {
      Request req = unit.get(Request.class);
      expect(req.<String>ifGet("pac4j.logout.redirectTo")).andReturn(redirectTo);
    };
  }
}
