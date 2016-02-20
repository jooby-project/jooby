package org.jooby.internal.pac4j;

import static org.easymock.EasyMock.expect;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.pac4j.Auth;
import org.jooby.pac4j.AuthStore;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.UserProfile;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AuthCallback.class, Clients.class })
public class AuthCallbackTest {

  private Block ctx = unit -> {
    Request req = unit.get(Request.class);
    expect(req.require(WebContext.class)).andReturn(unit.get(WebContext.class));
  };

  private Block localRedirect = unit -> {
    Request req = unit.get(Request.class);
    expect(req.ifGet("pac4jRequestedUrl")).andReturn(Optional.empty());
  };

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Block auth = unit -> {
    WebContext ctx = unit.get(WebContext.class);
    Credentials creds = unit.get(Credentials.class);
    Client client = unit.get(Client.class);
    expect(client.getCredentials(ctx)).andReturn(creds);
    expect(client.getUserProfile(creds, ctx)).andReturn(unit.get(UserProfile.class));
  };

  private Block setProfileId = unit -> {
    UserProfile profile = unit.get(UserProfile.class);
    String profileId = "profileId";
    expect(profile.getId()).andReturn(profileId);

    Mutant requestedURL = unit.mock(Mutant.class);
    expect(requestedURL.toOptional()).andReturn(Optional.of("/"));

    Session session = unit.mock(Session.class);
    expect(session.set(Auth.ID, profileId)).andReturn(session);
    expect(session.unset(Pac4jConstants.REQUESTED_URL)).andReturn(requestedURL);

    Request req = unit.get(Request.class);
    expect(req.set(Auth.ID, profileId)).andReturn(req);
    expect(req.session()).andReturn(session);

    Response rsp = unit.get(Response.class);
    rsp.redirect("/");
  };

  private Block setProfileId2 = unit -> {
    UserProfile profile = unit.get(UserProfile.class);
    String profileId = "profileId";
    expect(profile.getId()).andReturn(profileId);

    Mutant requestedURL = unit.mock(Mutant.class);
    expect(requestedURL.toOptional()).andReturn(Optional.of("/"));

    Session session = unit.mock(Session.class);
    expect(session.set(Auth.ID, profileId)).andReturn(session);
    expect(session.unset(Pac4jConstants.REQUESTED_URL)).andReturn(requestedURL);

    Request req = unit.get(Request.class);
    expect(req.set(Auth.ID, profileId)).andReturn(req);
    expect(req.session()).andReturn(session);

    Response rsp = unit.get(Response.class);
    rsp.redirect("/home");
  };

  @SuppressWarnings("unchecked")
  private Block onSuccess = unit -> {
    UserProfile profile = unit.get(UserProfile.class);

    unit.get(AuthStore.class)
        .set(profile);

  };

  @SuppressWarnings("rawtypes")
  private Block oneClient = unit -> {
    Client client = unit.get(Client.class);
    List<Client> clientList = ImmutableList.of(client);

    Clients clients = unit.get(Clients.class);
    expect(clients.findAllClients()).andReturn(clientList);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Clients.class, AuthStore.class)
        .run(unit -> {
          new AuthCallback(unit.get(Clients.class), unit.get(AuthStore.class), "/");
        });
  }

  @Test
  public void handleWith1Client() throws Exception {
    new MockUnit(Clients.class, Client.class, AuthStore.class, Request.class, Response.class,
        Route.Chain.class, WebContext.class, Credentials.class, UserProfile.class)
        .expect(ctx)
        .expect(localRedirect)
        .expect(auth)
        .expect(setProfileId)
        .expect(onSuccess)
        .expect(oneClient)
        .run(unit -> {
          new AuthCallback(unit.get(Clients.class), unit.get(AuthStore.class), "/")
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  @Test
  public void handleWith2Client() throws Exception {
    new MockUnit(Clients.class, Client.class, AuthStore.class, Request.class, Response.class,
        Route.Chain.class, WebContext.class, Credentials.class, UserProfile.class)
        .expect(ctx)
        .expect(localRedirect)
        .expect(auth)
        .expect(setProfileId2)
        .expect(onSuccess)
        .expect(oneClient)
        .run(unit -> {
          new AuthCallback(unit.get(Clients.class), unit.get(AuthStore.class), "/home")
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void handleNoProfile() throws Exception {
    new MockUnit(Clients.class, Client.class, AuthStore.class, Request.class, Response.class,
        Route.Chain.class, WebContext.class, Credentials.class, UserProfile.class)
        .expect(ctx)
        .expect(localRedirect)
        .expect(unit -> {
          WebContext ctx = unit.get(WebContext.class);
          Credentials creds = unit.get(Credentials.class);
          Client client = unit.get(Client.class);
          expect(client.getCredentials(ctx)).andReturn(creds);
          expect(client.getUserProfile(creds, ctx)).andReturn(null);
        })
        .expect(oneClient)
        .expect(unit -> {
          Mutant requestedURL = unit.mock(Mutant.class);
          expect(requestedURL.toOptional()).andReturn(Optional.of("/"));

          Session session = unit.mock(Session.class);
          expect(session.unset(Pac4jConstants.REQUESTED_URL)).andReturn(requestedURL);

          Request req = unit.get(Request.class);
          expect(req.session()).andReturn(session);

          Response rsp = unit.get(Response.class);
          rsp.redirect("/");
        })
        .run(unit -> {
          new AuthCallback(unit.get(Clients.class), unit.get(AuthStore.class), "/")
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void handleWithNClient() throws Exception {
    new MockUnit(Clients.class, Client.class, AuthStore.class, Request.class, Response.class,
        Route.Chain.class, WebContext.class, Credentials.class, UserProfile.class)
        .expect(ctx)
        .expect(localRedirect)
        .expect(auth)
        .expect(setProfileId)
        .expect(onSuccess)
        .expect(unit -> {
          Client client1 = unit.get(Client.class);
          Client client2 = unit.mock(Client.class);
          List<Client> clientList = ImmutableList.of(client1, client2);

          Clients clients = unit.get(Clients.class);
          expect(clients.findAllClients()).andReturn(clientList);
        })
        .expect(unit -> {
          Client client = unit.get(Client.class);

          WebContext ctx = unit.get(WebContext.class);
          Clients clients = unit.get(Clients.class);
          expect(clients.findClient(ctx)).andReturn(client);
        })
        .run(unit -> {
          new AuthCallback(unit.get(Clients.class), unit.get(AuthStore.class), "/")
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  @SuppressWarnings({"rawtypes" })
  @Test
  public void requireAction() throws Exception {
    new MockUnit(Clients.class, Client.class, AuthStore.class, Request.class, Response.class,
        Route.Chain.class, WebContext.class, Credentials.class)
        .expect(ctx)
        .expect(unit -> {
          WebContext ctx = unit.get(WebContext.class);

          Client client = unit.get(Client.class);
          expect(client.getCredentials(ctx)).andThrow(
              RequiresHttpAction.redirect("m", ctx(), "url"));
        })
        .expect(oneClient)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.committed()).andReturn(true);
        })
        .run(unit -> {
          new AuthCallback(unit.get(Clients.class), unit.get(AuthStore.class), "/")
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  private WebContext ctx() {
    return new WebContext() {

      @Override
      public void writeResponseContent(final String content) {
      }

      @Override
      public void setSessionAttribute(final String name, final Object value) {
      }

      @Override
      public void setResponseStatus(final int code) {
      }

      @Override
      public void setResponseHeader(final String name, final String value) {
      }

      @Override
      public Object getSessionAttribute(final String name) {
        return null;
      }

      @Override
      public int getServerPort() {
        return 0;
      }

      @Override
      public boolean isSecure() {
        return false;
      }

      @Override
      public String getPath() {
        return null;
      }

      @Override
      public String getServerName() {
        return null;
      }

      @Override
      public String getScheme() {
        return null;
      }

      @Override
      public Map<String, String[]> getRequestParameters() {
        return null;
      }

      @Override
      public String getRequestParameter(final String name) {
        return null;
      }

      @Override
      public String getRequestMethod() {
        return null;
      }

      @Override
      public String getRequestHeader(final String name) {
        return null;
      }

      @Override
      public String getFullRequestURL() {
        return null;
      }

      @Override
      public Object getRequestAttribute(final String name) {
        return null;
      }

      @Override
      public void setRequestAttribute(final String name, final Object value) {
      }

      @Override
      public Object getSessionIdentifier() {
        return null;
      }

      @Override
      public String getRemoteAddr() {
        return null;
      }

      @Override
      public void setResponseCharacterEncoding(final String encoding) {
      }

      @Override
      public void setResponseContentType(final String content) {
      }

      @Override
      public Collection<Cookie> getRequestCookies() {
        return null;
      }

      @Override
      public void addResponseCookie(final Cookie cookie) {
      }
    };
  }

}
