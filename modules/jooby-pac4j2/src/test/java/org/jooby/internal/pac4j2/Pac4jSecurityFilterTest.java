package org.jooby.internal.pac4j2;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.easymock.IExpectationSetters;
import org.jooby.Err;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.Status;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.http.HttpActionAdapter;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Pac4jSecurityFilterTest {

  private MockUnit.Block webContext = unit -> {
    Request req = unit.get(Request.class);
    expect(req.require(WebContext.class)).andReturn(unit.get(WebContext.class));
  };

  private MockUnit.Block getSecurityLogic = unit -> {
    SecurityLogic action = unit.get(SecurityLogic.class);
    Config config = unit.get(Config.class);
    expect(config.getSecurityLogic()).andReturn(action);
  };

  private MockUnit.Block getActionAdapter = unit -> {
    Config config = unit.get(Config.class);
    expect(config.getHttpActionAdapter()).andReturn(unit.get(HttpActionAdapter.class));
  };

  @Test
  public void shouldExecuteCallback() throws Exception {
    String clients = "FormClient";
    String authorizers = null;
    String matchers = null;
    Set<String> excludes = Sets.newHashSet("/profile");
    boolean multiProfile = false;
    new MockUnit(Request.class, Response.class, Config.class, WebContext.class, SecurityLogic.class,
        HttpActionAdapter.class, Route.Chain.class)
        .expect(webContext)
        .expect(getSecurityLogic)
        .expect(getActionAdapter)
        .expect(getSessionAttribute(Pac4jConstants.REQUESTED_URL, null))
        .expect(requestMatches("/profile", false))
        .expect(executeCallback(clients, authorizers, matchers, multiProfile))
        .run(unit -> {
          Pac4jSecurityFilter action = new Pac4jSecurityFilter(unit.get(Config.class), clients,
              authorizers, matchers, multiProfile, excludes);

          assertEquals("FormClient", action.toString());

          action.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        });
  }

  @Test
  public void shouldResetRequestedUrl() throws Exception {
    String clients = "FormClient";
    String authorizers = null;
    String matchers = null;
    Set<String> excludes = ImmutableSet.of("/**", "/facebook");
    boolean multiProfile = false;
    new MockUnit(Request.class, Response.class, Config.class, WebContext.class, SecurityLogic.class,
        HttpActionAdapter.class, Route.Chain.class, Session.class)
        .expect(webContext)
        .expect(getSecurityLogic)
        .expect(getActionAdapter)
        .expect(getSessionAttribute(Pac4jConstants.REQUESTED_URL, "/previous"))
        .expect(requestMatches("/facebook", true))
        .expect(executeCallback(clients, authorizers, matchers, multiProfile))
        .expect(ifSession(true))
        .expect(setSessionAttribute(Pac4jConstants.REQUESTED_URL, "/previous"))
        .run(unit -> {
          Pac4jSecurityFilter action = new Pac4jSecurityFilter(unit.get(Config.class), clients,
              authorizers, matchers, multiProfile, excludes);

          assertEquals("FormClient", action.toString());

          action.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        });
  }

  @Test
  public void shouldIgnoreResetRequestedUrlIfNoSession() throws Exception {
    String clients = "FormClient";
    String authorizers = null;
    String matchers = null;
    Set<String> excludes = ImmutableSet.of("/**", "/facebook");
    boolean multiProfile = false;
    new MockUnit(Request.class, Response.class, Config.class, WebContext.class, SecurityLogic.class,
        HttpActionAdapter.class, Route.Chain.class, Session.class)
        .expect(webContext)
        .expect(getSecurityLogic)
        .expect(getActionAdapter)
        .expect(getSessionAttribute(Pac4jConstants.REQUESTED_URL, "/previous"))
        .expect(requestMatches("/facebook", true))
        .expect(executeCallback(clients, authorizers, matchers, multiProfile))
        .expect(ifSession(false))
        .run(unit -> {
          Pac4jSecurityFilter action = new Pac4jSecurityFilter(unit.get(Config.class), clients,
              authorizers, matchers, multiProfile, excludes);

          assertEquals("FormClient", action.toString());

          action.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        });
  }

  @Test
  public void shouldExecuteCallbackWithClients() throws Exception {
    String clients = "FormClient";
    String authorizers = null;
    String matchers = null;
    boolean multiProfile = false;
    Set<String> excludes = new HashSet<>();
    new MockUnit(Request.class, Response.class, Config.class, WebContext.class, SecurityLogic.class,
        HttpActionAdapter.class, Route.Chain.class)
        .expect(webContext)
        .expect(getSecurityLogic)
        .expect(getActionAdapter)
        .expect(getSessionAttribute(Pac4jConstants.REQUESTED_URL, null))
        .expect(executeCallback(clients + ",FacebookClient", authorizers, matchers, multiProfile))
        .run(unit -> {
          Pac4jSecurityFilter action = new Pac4jSecurityFilter(unit.get(Config.class), clients,
              authorizers, matchers, multiProfile, excludes)
              .addClient("FacebookClient");

          assertEquals("FormClient,FacebookClient", action.toString());

          action.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        });
  }

  @Test(expected = TechnicalException.class)
  public void shouldRethrowTechnicalException() throws Exception {
    String clients = "FormClient";
    String authorizers = null;
    String matchers = null;
    boolean multiProfile = false;
    Set<String> excludes = new HashSet<>();
    TechnicalException x = new TechnicalException("intentional err");
    new MockUnit(Request.class, Response.class, Config.class, WebContext.class, SecurityLogic.class,
        HttpActionAdapter.class, Route.Chain.class)
        .expect(webContext)
        .expect(getSecurityLogic)
        .expect(getActionAdapter)
        .expect(getSessionAttribute(Pac4jConstants.REQUESTED_URL, null))
        .expect(executeCallback(clients, authorizers, matchers, multiProfile, x))
        .run(unit -> {
          Pac4jSecurityFilter action = new Pac4jSecurityFilter(unit.get(Config.class), clients,
              authorizers, matchers, multiProfile, excludes);

          action.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        });
  }

  @Test(expected = Err.class)
  public void shouldThrowJoobyErrWhenTechnicalExceptionWrapsIt() throws Exception {
    String clients = "FormClient";
    String authorizers = null;
    String matchers = null;
    boolean multiProfile = false;
    Set<String> excludes = new HashSet<>();
    TechnicalException x = new TechnicalException(new Err(Status.UNAUTHORIZED, "intentional err"));
    new MockUnit(Request.class, Response.class, Config.class, WebContext.class, SecurityLogic.class,
        HttpActionAdapter.class, Route.Chain.class)
        .expect(webContext)
        .expect(getSecurityLogic)
        .expect(getActionAdapter)
        .expect(getSessionAttribute(Pac4jConstants.REQUESTED_URL, null))
        .expect(executeCallback(clients, authorizers, matchers, multiProfile, x))
        .run(unit -> {
          Pac4jSecurityFilter action = new Pac4jSecurityFilter(unit.get(Config.class), clients,
              authorizers, matchers, multiProfile, excludes);

          action.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        });
  }

  private MockUnit.Block executeCallback(String defaultUrl, String logoutUrlPattern,
      String matchers, boolean multiProfile) {
    return executeCallback(defaultUrl, logoutUrlPattern, matchers, multiProfile, null);
  }

  private MockUnit.Block executeCallback(String defaultUrl, String logoutUrlPattern,
      String matchers, boolean multiProfile, Throwable x) {
    return unit -> {
      SecurityLogic action = unit.get(SecurityLogic.class);

      IExpectationSetters<Object> expect = expect(
          action.perform(eq(unit.get(WebContext.class)), eq(unit.get(Config.class)),
              isA(Pac4jGrantAccessAdapter.class), eq(unit.get(HttpActionAdapter.class)),
              eq(defaultUrl), eq(logoutUrlPattern), eq(matchers), eq(multiProfile)));
      if (x == null)
        expect.andReturn(null);
      else
        expect.andThrow(x);
    };
  }

  private MockUnit.Block requestMatches(String pattern, boolean matches) {
    return unit -> {
      Request request = unit.get(Request.class);
      expect(request.matches(pattern)).andReturn(matches);
    };
  }

  private MockUnit.Block clientParameterName(String name, String clients, String defaultClient) {
    return unit -> {
      Mutant value = unit.mock(Mutant.class);
      expect(value.value(defaultClient)).andReturn(clients);
      Request req = unit.get(Request.class);
      expect(req.param(name)).andReturn(value);

      expect(req.set("pac4j." + name, clients)).andReturn(req);
    };
  }

  private MockUnit.Block getSessionAttribute(String name, String value) {
    return unit -> {
      WebContext ctx = unit.get(WebContext.class);
      expect(ctx.getSessionAttribute(name)).andReturn(value);
    };
  }

  private MockUnit.Block setSessionAttribute(String name, String value) {
    return unit -> {
      WebContext ctx = unit.get(WebContext.class);
      ctx.setSessionAttribute(name, value);
    };
  }

  private MockUnit.Block ifSession(boolean exists) {
    return unit -> {
      Request request = unit.get(Request.class);
      Session session = null;
      if (exists) {
        session = unit.get(Session.class);
      }
      expect(request.ifSession()).andReturn(Optional.ofNullable(session));
    };
  }
}
