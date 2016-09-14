package org.jooby.internal.pac4j;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.jooby.Err;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.pac4j.Auth;
import org.jooby.pac4j.AuthStore;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.finder.ClientFinder;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.http.client.direct.ParameterClient;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javaslang.control.Try;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AuthFilter.class, Clients.class })
public class AuthFilterTest {

  private Block webctx = unit -> {
    Request req = unit.get(Request.class);
    expect(req.require(WebContext.class)).andReturn(unit.get(WebContext.class));
  };

  private Block cfinder = unit -> {
    Request req = unit.get(Request.class);
    expect(req.require(ClientFinder.class)).andReturn(unit.get(ClientFinder.class));
  };

  private Block astore = unit -> {
    Request req = unit.get(Request.class);
    expect(req.require(AuthStore.class)).andReturn(unit.get(AuthStore.class));
  };

  private Block clients = unit -> {
    Clients clients = unit.get(Clients.class);

    Request req = unit.get(Request.class);
    expect(req.require(Clients.class)).andReturn(clients);
  };

  private Block chainNext = unit -> {
    Route.Chain chain = unit.get(Route.Chain.class);
    chain.next(unit.get(Request.class), unit.get(Response.class));
  };

  @SuppressWarnings("rawtypes")
  private <C extends Client> Block findClient(final Class<C> clientType, final String client) {
    return unit -> {
      C c = Try.of(() -> unit.get(clientType))
          .getOrElse(() -> {
            C m = unit.mock(clientType);
            //C m = (C) new ParameterClient("token", new SimpleTestUsernamePasswordAuthenticator());
            unit.registerMock(clientType, m);
            return m;
          });

      ClientFinder finder = unit.get(ClientFinder.class);
      expect(finder.find(unit.get(Clients.class), unit.get(WebContext.class), client))
          .andReturn(Arrays.asList(c));
    };
  }

  @SuppressWarnings("rawtypes")
  private <C extends Client> Block findNoClient(final Class<C> clientType, final String client) {
    return unit -> {

      ClientFinder finder = unit.get(ClientFinder.class);
      expect(finder.find(unit.get(Clients.class), unit.get(WebContext.class), client))
          .andReturn(Collections.emptyList());
    };
  }

  private Block reqAuthID(final String id) {
    return unit -> {
      Request req = unit.get(Request.class);
      expect(req.ifGet(Auth.ID)).andReturn(Optional.ofNullable(id));
    };
  }

  @SuppressWarnings("rawtypes")
  private Block authStore(final String id, final CommonProfile profile) {
    return unit -> {
      AuthStore store = unit.get(AuthStore.class);
      expect(store.get(id)).andReturn(Optional.ofNullable(profile));
    };
  }

  @SuppressWarnings("rawtypes")
  private <C extends Client> Block creds(final Class<C> clientType) {
    return unit -> {
      Credentials creds = unit.mock(Credentials.class);
      unit.registerMock(Credentials.class, creds);

      WebContext ctx = unit.get(WebContext.class);
      C client = unit.get(clientType);
      expect(client.getCredentials(ctx)).andReturn(creds);
    };
  }

  @SuppressWarnings("rawtypes")
  private <C extends Client> Block noCreds(final Class<C> clientType) {
    return unit -> {
      WebContext ctx = unit.get(WebContext.class);
      C client = unit.get(clientType);
      expect(client.getCredentials(ctx))
          .andThrow(HttpAction.forbidden("intentional err", ctx));
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  private <C extends Client> Block userProfile(final Class<C> clientType,
      final CommonProfile profile) {
    return unit -> {
      Credentials creds = unit.get(Credentials.class);

      WebContext ctx = unit.get(WebContext.class);
      C client = unit.get(clientType);
      expect(client.getUserProfile(creds, ctx)).andReturn(profile);
    };
  }

  private Block setProfileId(final String id) {
    return unit -> {
      Request req = unit.get(Request.class);
      expect(req.set(Auth.ID, id)).andReturn(req);
    };
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Block setProfile(final CommonProfile profile) {
    return unit -> {
      AuthStore store = unit.get(AuthStore.class);
      store.set(profile);
    };
  }

  @SuppressWarnings("rawtypes")
  private Block seed(final Class type, final CommonProfile profile) {
    return unit -> {
      Request req = unit.get(Request.class);
      expect(req.set(type, profile)).andReturn(req);
    };
  }

  private Block clientName(final String name) {
    return unit -> {
      Clients clients = unit.get(Clients.class);
      expect(clients.getClientNameParameter()).andReturn("client_name");

      Mutant client_name = unit.mock(Mutant.class);
      expect(client_name.value(name)).andReturn(name);

      Request req = unit.get(Request.class);
      expect(req.param("client_name")).andReturn(client_name);
    };
  }

  @Test
  public void name() throws Exception {
    String profileId = "123";
    CommonProfile profile = new CommonProfile();
    profile.setId(profileId);

    new MockUnit(Request.class, Response.class, Route.Chain.class, WebContext.class,
        ClientFinder.class, AuthStore.class, Clients.class).run(unit -> {
          AuthFilter filter = new AuthFilter(ParameterClient.class, CommonProfile.class);
          assertEquals("ParameterClient", filter.getName());
          filter.setName("Basic");
          assertEquals("ParameterClient,Basic", filter.getName());
        });
  }

  @Test
  public void handleDirect() throws Exception {
    String profileId = "123";
    CommonProfile profile = new CommonProfile();
    profile.setId(profileId);

    new MockUnit(Request.class, Response.class, Route.Chain.class, WebContext.class,
        ClientFinder.class, AuthStore.class, Clients.class)
            .expect(webctx)
            .expect(cfinder)
            .expect(astore)
            .expect(clients)
            .expect(clientName("ParameterClient"))
            .expect(findClient(ParameterClient.class, "ParameterClient"))
            .expect(reqAuthID(profileId))
            .expect(reqAuthCNAME(ParameterClient.class))
            .expect(authStore(profileId, null))
            .expect(creds(ParameterClient.class))
            .expect(userProfile(ParameterClient.class, profile))
            .expect(setProfileId(profileId))
            .expect(setProfile(profile))
            .expect(seed(CommonProfile.class, profile))
            .expect(seed(CommonProfile.class, profile))
            .expect(seed(CommonProfile.class, profile))
            .expect(chainNext)
            .run(unit -> {
              new AuthFilter(ParameterClient.class, CommonProfile.class)
                  .handle(unit.get(Request.class), unit.get(Response.class),
                      unit.get(Route.Chain.class));
            });
  }

  @SuppressWarnings("rawtypes")
  private Block reqAuthCNAME(final Class<? extends Client> client) {
    return unit -> {
      Client c = unit.get(client);
      expect(c.getName()).andReturn(client.getSimpleName());

      Request req = unit.get(Request.class);
      expect(req.set(Auth.CNAME, client.getSimpleName())).andReturn(req);
    };
  }

  @Test
  public void handleDirectNoClient() throws Exception {
    String profileId = "123";
    CommonProfile profile = new CommonProfile();
    profile.setId(profileId);

    new MockUnit(Request.class, Response.class, Route.Chain.class, WebContext.class,
        ClientFinder.class, AuthStore.class, Clients.class)
            .expect(webctx)
            .expect(cfinder)
            .expect(astore)
            .expect(clients)
            .expect(clientName("ParameterClient"))
            .expect(findNoClient(ParameterClient.class, "ParameterClient"))
            .expect(findNoClient(ParameterClient.class, "ParameterClient"))
            .run(unit -> {
              try {
                new AuthFilter(ParameterClient.class, CommonProfile.class)
                    .handle(unit.get(Request.class), unit.get(Response.class));
                fail("expecting 401");
              } catch (Err ex) {
                assertEquals(401, ex.statusCode());
              }
            });
  }

  @Test(expected = TechnicalException.class)
  public void handleDirectNoCredentials() throws Exception {
    String profileId = "123";
    CommonProfile profile = new CommonProfile();
    profile.setId(profileId);

    new MockUnit(Request.class, Response.class, Route.Chain.class, WebContext.class,
        ClientFinder.class, AuthStore.class, Clients.class)
            .expect(webctx)
            .expect(cfinder)
            .expect(astore)
            .expect(clients)
            .expect(clientName("ParameterClient"))
            .expect(findClient(ParameterClient.class, "ParameterClient"))
            .expect(reqAuthID(profileId))
            .expect(authStore(profileId, null))
            .expect(noCreds(ParameterClient.class))
            .run(unit -> {
              new AuthFilter(ParameterClient.class, CommonProfile.class)
                  .handle(unit.get(Request.class), unit.get(Response.class),
                      unit.get(Route.Chain.class));
            });
  }

  @Test(expected = Err.class)
  public void handleDirectNoProfile() throws Exception {
    String profileId = "123";
    CommonProfile profile = null;

    new MockUnit(Request.class, Response.class, Route.Chain.class, WebContext.class,
        ClientFinder.class, AuthStore.class, Clients.class)
            .expect(webctx)
            .expect(cfinder)
            .expect(astore)
            .expect(clients)
            .expect(clientName("ParameterClient"))
            .expect(findClient(ParameterClient.class, "ParameterClient"))
            .expect(findClient(ParameterClient.class, "ParameterClient"))
            .expect(reqAuthID(profileId))
            .expect(authStore(profileId, null))
            .expect(creds(ParameterClient.class))
            .expect(userProfile(ParameterClient.class, profile))
            .expect(setProfileId(profileId))
            .expect(setProfile(profile))
            .run(unit -> {
              new AuthFilter(ParameterClient.class, CommonProfile.class)
                  .handle(unit.get(Request.class), unit.get(Response.class),
                      unit.get(Route.Chain.class));
            });
  }

}
