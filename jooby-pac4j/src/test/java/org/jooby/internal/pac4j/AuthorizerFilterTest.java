package org.jooby.internal.pac4j;

import static org.easymock.EasyMock.expect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jooby.Err;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.authorization.checker.AuthorizationChecker;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;

@SuppressWarnings("rawtypes")
public class AuthorizerFilterTest {

  Map<String, Authorizer> authorizers = new HashMap<>();

  private MockUnit.Block config = unit -> {
    Config config = unit.get(Config.class);
    Request req = unit.get(Request.class);
    expect(req.require(Config.class)).andReturn(config);

    expect(config.getAuthorizers()).andReturn(authorizers);
  };

  private MockUnit.Block ctx = unit -> {
    WebContext ctx = unit.get(WebContext.class);
    Request req = unit.get(Request.class);
    expect(req.require(WebContext.class)).andReturn(ctx);
  };

  private MockUnit.Block profile = unit -> {
    CommonProfile profile = unit.get(CommonProfile.class);
    Request req = unit.get(Request.class);
    expect(req.require(CommonProfile.class)).andReturn(profile);
  };

  private MockUnit.Block authorizerChecker = unit -> {
    AuthorizationChecker checker = unit.get(AuthorizationChecker.class);
    Request req = unit.get(Request.class);
    expect(req.require(AuthorizationChecker.class)).andReturn(checker);
  };

  private MockUnit.Block pass = unit -> {
    AuthorizationChecker checker = unit.get(AuthorizationChecker.class);

    expect(checker.isAuthorized(unit.get(WebContext.class), Arrays.asList(unit.get(CommonProfile.class)), "admin",
        authorizers)).andReturn(true);
  };

  private MockUnit.Block forbidden = unit -> {
    AuthorizationChecker checker = unit.get(AuthorizationChecker.class);

    expect(checker.isAuthorized(unit.get(WebContext.class), Arrays.asList(unit.get(CommonProfile.class)), "admin",
        authorizers)).andReturn(false);
  };

  @Test
  public void pass() throws Exception {
    new MockUnit(Request.class, Response.class, Config.class, WebContext.class, CommonProfile.class,
        AuthorizationChecker.class)
            .expect(config)
            .expect(ctx)
            .expect(profile)
            .expect(authorizerChecker)
            .expect(pass)
            .run(unit -> {
              new AuthorizerFilter("admin")
                  .handle(unit.get(Request.class), unit.get(Response.class));
            });
  }

  @Test(expected = Err.class)
  public void forbidden() throws Exception {
    new MockUnit(Request.class, Response.class, Config.class, WebContext.class, CommonProfile.class,
        AuthorizationChecker.class)
            .expect(config)
            .expect(ctx)
            .expect(profile)
            .expect(authorizerChecker)
            .expect(forbidden)
            .run(unit -> {
              new AuthorizerFilter("admin")
                  .handle(unit.get(Request.class), unit.get(Response.class));
            });
  }

}
