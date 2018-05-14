package org.jooby.pac4j;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jooby.Env;
import org.jooby.Registry;
import org.jooby.Route;
import org.jooby.Router;
import org.jooby.funzy.Throwing;
import org.jooby.internal.pac4j2.Pac4jActionAdapter;
import org.jooby.internal.pac4j2.Pac4jAuthorizer;
import org.jooby.internal.pac4j2.Pac4jCallback;
import org.jooby.internal.pac4j2.Pac4jContext;
import org.jooby.internal.pac4j2.Pac4jLoginForm;
import org.jooby.internal.pac4j2.Pac4jLogout;
import org.jooby.internal.pac4j2.Pac4jProfileManager;
import org.jooby.internal.pac4j2.Pac4jSecurityFilter;
import org.jooby.internal.pac4j2.Pac4jSessionStore;
import org.jooby.scope.RequestScoped;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.authorization.authorizer.IsAnonymousAuthorizer;
import org.pac4j.core.authorization.authorizer.ProfileAuthorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.engine.DefaultLogoutLogic;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.engine.LogoutLogic;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Pac4j.class, Pac4jLoginForm.class, Clients.class, FormClient.class,
    SimpleTestUsernamePasswordAuthenticator.class, DefaultLogoutLogic.class})
public class Pac4jTest {

  static final TypeLiteral<SessionStore<WebContext>> SSTORE = new TypeLiteral<SessionStore<WebContext>>() {
  };

  private MockUnit.Block router = unit -> {
    Env env = unit.get(Env.class);
    expect(env.router()).andReturn(unit.get(Router.class));
  };

  private MockUnit.Block webContext = unit -> {
    AnnotatedBindingBuilder<WebContext> abb = unit.mock(AnnotatedBindingBuilder.class);
    expect(abb.to(Pac4jContext.class)).andReturn(abb);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(WebContext.class)).andReturn(abb);
  };

  private MockUnit.Block profileManager = unit -> {
    AnnotatedBindingBuilder<ProfileManager> abb = unit.mock(AnnotatedBindingBuilder.class);
    expect(abb.toProvider(Pac4jProfileManager.class)).andReturn(abb);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(ProfileManager.class)).andReturn(abb);
  };

  private MockUnit.Block pac4jConfig = unit -> {
    org.pac4j.core.config.Config config = unit.constructor(org.pac4j.core.config.Config.class)
        .build();
    config.setClients(unit.get(Clients.class));
    config.setHttpActionAdapter(isA(Pac4jActionAdapter.class));
    expect(config.getClients()).andReturn(unit.get(Clients.class));

    unit.registerMock(org.pac4j.core.config.Config.class, config);

    AnnotatedBindingBuilder<org.pac4j.core.config.Config> abb = unit
        .mock(AnnotatedBindingBuilder.class);
    abb.toInstance(isA(org.pac4j.core.config.Config.class));

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(org.pac4j.core.config.Config.class)).andReturn(abb);
  };

  @Test
  public void installPac4jModule() throws Exception {
    Config config = config("/");
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class)
        .expect(newLoginFormClient())
        .expect(newClients("http://localhost:8080/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(null))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FormClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(null))
        .expect(loginForm("/callback"))
        .expect(callback(null, "/", "/callback", false, false))
        .expect(securityLogic(null, "/**", null, null, false, Clients.DEFAULT_CLIENT_NAME_PARAMETER,
            ImmutableSet.of("/callback", "/**"), FormClient.class))
        .expect(logoutLogic(null, "/", "/.*", true, true, false))
        .expect(guiceAuthorizers())
        .expect(setupGuiceAuthorizers())
        .run(unit -> {
          new Pac4j()
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, setupGuiceAuthorizers());
  }

  @Test
  public void withContextPath() throws Exception {
    Config config = config("/myapp");
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class)
        .expect(newLoginFormClient())
        .expect(newClients("http://localhost:8080/myapp/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(null))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FormClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(null))
        .expect(loginForm("/myapp/callback"))
        .expect(callback(null, "/myapp", "/callback", false, false))
        .expect(securityLogic(null, "/**", null, null, false, Clients.DEFAULT_CLIENT_NAME_PARAMETER,
            ImmutableSet.of("/callback", "/**"), FormClient.class))
        .expect(logoutLogic(null, "/myapp", "/.*", true, true, false))
        .expect(guiceAuthorizers())
        .expect(setupGuiceAuthorizers())
        .run(unit -> {
          new Pac4j()
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, setupGuiceAuthorizers());
  }

  @Test
  public void setMultiprofile() throws Exception {
    Config config = config("/");
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class)
        .expect(newLoginFormClient())
        .expect(newClients("http://localhost:8080/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(null))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FormClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(null))
        .expect(loginForm("/callback"))
        .expect(callback(null, "/", "/callback", true, false))
        .expect(securityLogic(null, "/**", null, null, true, Clients.DEFAULT_CLIENT_NAME_PARAMETER,
            ImmutableSet.of("/callback", "/**"), FormClient.class))
        .expect(logoutLogic(null, "/", "/.*", true, true, false))
        .expect(guiceAuthorizers())
        .expect(setupGuiceAuthorizers())
        .run(unit -> {
          new Pac4j()
              .multiProfile(true)
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, setupGuiceAuthorizers());
  }

  @Test
  public void withProfileManagerFactory() throws Exception {
    Config config = config("/");
    Function<WebContext, ProfileManager> pmf = ProfileManager::new;
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class,
        Function.class)
        .expect(newLoginFormClient())
        .expect(newClients("http://localhost:8080/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(null))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FormClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(pmf))
        .expect(loginForm("/callback"))
        .expect(callback(null, "/", "/callback", false, false))
        .expect(securityLogic(null, "/**", null, null, false, Clients.DEFAULT_CLIENT_NAME_PARAMETER,
            ImmutableSet.of("/callback", "/**"), FormClient.class))
        .expect(logoutLogic(null, "/", "/.*", true, true, false))
        .expect(guiceAuthorizers())
        .expect(setupGuiceAuthorizers())
        .run(unit -> {
          new Pac4j()
              .doWith(pac4j -> {
                pac4j.setProfileManagerFactory(pmf);
              })
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, setupGuiceAuthorizers());
  }

  @Test
  public void withSessionStore() throws Exception {
    Config config = config("/");
    SessionStore ss = new Pac4jSessionStore(null);
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class,
        Function.class)
        .expect(newLoginFormClient())
        .expect(newClients("http://localhost:8080/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(ss))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FormClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(null))
        .expect(loginForm("/callback"))
        .expect(callback(null, "/", "/callback", false, false))
        .expect(securityLogic(null, "/**", null, null, false, Clients.DEFAULT_CLIENT_NAME_PARAMETER,
            ImmutableSet.of("/callback", "/**"), FormClient.class))
        .expect(logoutLogic(null, "/", "/.*", true, true, false))
        .expect(guiceAuthorizers())
        .expect(setupGuiceAuthorizers())
        .run(unit -> {
          new Pac4j()
              .doWith(pac4j -> {
                pac4j.setSessionStore(ss);
              })
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, setupGuiceAuthorizers());
  }

  @Test
  public void withSecurityLogic() throws Exception {
    Config config = config("/");
    SecurityLogic action = new DefaultSecurityLogic();
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class,
        Function.class)
        .expect(newLoginFormClient())
        .expect(newClients("http://localhost:8080/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(null))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FormClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(null))
        .expect(loginForm("/callback"))
        .expect(callback(null, "/", "/callback", false, false))
        .expect(
            securityLogic(action, "/**", null, null, false, Clients.DEFAULT_CLIENT_NAME_PARAMETER,
                ImmutableSet.of("/callback", "/**"), FormClient.class))
        .expect(logoutLogic(null, "/", "/.*", true, true, false))
        .expect(guiceAuthorizers())
        .expect(setupGuiceAuthorizers())
        .run(unit -> {
          new Pac4j()
              .doWith(pac4j -> {
                pac4j.setSecurityLogic(action);
              })
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, setupGuiceAuthorizers());
  }

  @Test
  public void withCallbackLogic() throws Exception {
    Config config = config("/");
    CallbackLogic action = new DefaultCallbackLogic();
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class,
        Function.class)
        .expect(newLoginFormClient())
        .expect(newClients("http://localhost:8080/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(null))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FormClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(null))
        .expect(loginForm("/callback"))
        .expect(callback(action, "/", "/callback", false, false))
        .expect(securityLogic(null, "/**", null, null, false, Clients.DEFAULT_CLIENT_NAME_PARAMETER,
            ImmutableSet.of("/callback", "/**"), FormClient.class))
        .expect(logoutLogic(null, "/", "/.*", true, true, false))
        .expect(guiceAuthorizers())
        .expect(setupGuiceAuthorizers())
        .run(unit -> {
          new Pac4j()
              .doWith(pac4j -> {
                pac4j.setCallbackLogic(action);
              })
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, setupGuiceAuthorizers());
  }

  @Test
  public void withLogoutLogic() throws Exception {
    Config config = config("/");
    LogoutLogic action = new DefaultLogoutLogic();
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class,
        Function.class)
        .expect(newLoginFormClient())
        .expect(newClients("http://localhost:8080/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(null))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FormClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(null))
        .expect(loginForm("/callback"))
        .expect(callback(null, "/", "/callback", false, false))
        .expect(securityLogic(null, "/**", null, null, false, Clients.DEFAULT_CLIENT_NAME_PARAMETER,
            ImmutableSet.of("/callback", "/**"), FormClient.class))
        .expect(logoutLogic(action, "/", "/.*", true, true, false))
        .expect(guiceAuthorizers())
        .expect(setupGuiceAuthorizers())
        .run(unit -> {
          new Pac4j()
              .doWith(pac4j -> {
                pac4j.setLogoutLogic(action);
              })
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, setupGuiceAuthorizers());
  }

  @Test
  public void withConfigurer() throws Exception {
    Config config = config("/");
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class)
        .expect(newLoginFormClient())
        .expect(newClients("http://localhost:8080/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(null))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FormClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(null))
        .expect(loginForm("/callback"))
        .expect(callback(null, "/", "/callback", false, false))
        .expect(securityLogic(null, "/**", null, null, false, Clients.DEFAULT_CLIENT_NAME_PARAMETER,
            ImmutableSet.of("/callback", "/**"), FormClient.class))
        .expect(logoutLogic(null, "/", "/.*", true, true, false))
        .expect(guiceAuthorizers())
        .expect(setupGuiceAuthorizers())
        .run(unit -> {
          CountDownLatch counter = new CountDownLatch(1);
          new Pac4j()
              .doWith(pac4j -> {
                assertNotNull(pac4j);
                counter.countDown();
              })
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
          counter.await();
        }, setupGuiceAuthorizers());
  }

  @Test
  public void pac4jAddClient() throws Exception {
    Config config = config("/");
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class,
        FacebookClient.class)
        .expect(newClients("http://localhost:8080/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(null))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FacebookClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(null))
        .expect(callback(null, "/", "/callback", false, false))
        .expect(securityLogic(null, "/**", null, null, false, Clients.DEFAULT_CLIENT_NAME_PARAMETER,
            ImmutableSet.of("/callback", "/**"), FacebookClient.class))
        .expect(logoutLogic(null, "/", "/.*", true, true, false))
        .expect(guiceAuthorizers())
        .expect(setupGuiceAuthorizers())
        .run(unit -> {
          new Pac4j()
              .client(conf -> {
                assertNotNull(conf);
                return unit.get(FacebookClient.class);
              })
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, setupGuiceAuthorizers());
  }

  @Test
  public void pac4jAddClientWithPattern() throws Exception {
    Config config = config("/");
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class,
        FacebookClient.class)
        .expect(newClients("http://localhost:8080/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(null))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FacebookClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(null))
        .expect(callback(null, "/", "/callback", false, false))
        .expect(securityLogic(null, "/admin/**", null, null, false,
            Clients.DEFAULT_CLIENT_NAME_PARAMETER, ImmutableSet.of("/callback", "/admin/**"),
            FacebookClient.class))
        .expect(logoutLogic(null, "/", "/.*", true, true, false))
        .expect(guiceAuthorizers())
        .expect(setupGuiceAuthorizers())
        .run(unit -> {
          new Pac4j()
              .client("/admin/**", conf -> {
                assertNotNull(conf);
                return unit.get(FacebookClient.class);
              })
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, setupGuiceAuthorizers());
  }

  @Test
  public void pac4jMultiClientWith() throws Exception {
    Config config = config("/");
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class,
        FacebookClient.class, TwitterClient.class)
        .expect(newClients("http://localhost:8080/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(null))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FacebookClient.class, TwitterClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(null))
        .expect(callback(null, "/", "/callback", true, false))
        .expect(securityLogic(null, "/**", "ProfileAuthorizer", null, true,
            Clients.DEFAULT_CLIENT_NAME_PARAMETER, ImmutableSet.of("/callback", "/**"),
            FacebookClient.class,
            TwitterClient.class))
        .expect(logoutLogic(null, "/", "/.*", true, true, false))
        .expect(guiceAuthorizers())
        .expect(setupGuiceAuthorizers())
        .expect(authorizer("ProfileAuthorizer", Pac4jAuthorizer.class))
        .expect(authorizer("ProfileAuthorizer", Pac4jAuthorizer.class))
        .run(unit -> {
          new Pac4j()
              .client("*", ProfileAuthorizer.class, conf -> {
                assertNotNull(conf);
                return unit.get(FacebookClient.class);
              })
              .client("*", ProfileAuthorizer.class, conf -> {
                assertNotNull(conf);
                return unit.get(TwitterClient.class);
              })
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, setupGuiceAuthorizers());
  }

  @Test
  public void pac4jAddClientWithAuthorizer() throws Exception {
    Config config = config("/");
    new MockUnit(Env.class, Binder.class, Router.class, WebContext.class, Registry.class,
        FacebookClient.class, Pac4jAuthorizer.class)
        .expect(newClients("http://localhost:8080/callback"))
        .expect(pac4jConfig)
        .expect(sessionStore(null))
        .expect(profile(CommonProfile.class))
        .expect(profile(UserProfile.class))
        .expect(router)
        .expect(setClients(FacebookClient.class))
        .expect(webContext)
        .expect(profileManager)
        .expect(profileManagerFactory(null))
        .expect(callback(null, "/", "/callback", false, false))
        .expect(securityLogic(null, "/admin/**", "ProfileAuthorizer", null, false,
            Clients.DEFAULT_CLIENT_NAME_PARAMETER, ImmutableSet.of("/callback", "/admin/**"),
            FacebookClient.class))
        .expect(logoutLogic(null, "/", "/.*", true, true, false))
        .expect(guiceAuthorizers(Pac4jAuthorizer.class))
        .expect(authorizer("ProfileAuthorizer", Pac4jAuthorizer.class))
        .run(unit -> {
          Pac4j pac4j = new Pac4j();
          pac4j.client("/admin/**", ProfileAuthorizer.class, conf -> {
            return unit.get(FacebookClient.class);
          });
          pac4j.configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, setupGuiceAuthorizers());
  }

  @Test
  public void testAuthorizerName() {
    assertEquals("ProfileAuthorizer",
        Pac4j.authorizerName(new Pac4jAuthorizer(ProfileAuthorizer.class)));

    assertEquals("IsAnonymousAuthorizer", Pac4j.authorizerName(new IsAnonymousAuthorizer()));
  }

  @Test
  public void shouldLoadConfig() {
    assertEquals(ConfigFactory.parseResources(Pac4j.class, "pac4j.conf"), new Pac4j().config());
  }

  private MockUnit.Block authorizer(String name, Class<? extends Authorizer> implementor) {
    return unit -> {
      org.pac4j.core.config.Config pac4j = unit.get(org.pac4j.core.config.Config.class);
      pac4j.addAuthorizer(eq(name), isA(implementor));
    };
  }

  private MockUnit.Block guiceAuthorizers(Class<? extends Authorizer>... authorizers) {
    return unit -> {
      Map<String, Authorizer> hash = Arrays.asList(authorizers).stream()
          .map(it -> unit.get(it))
          .collect(Collectors.toMap(it -> it.getClass().getSimpleName(), Function.identity()));
      org.pac4j.core.config.Config pac4j = unit.get(org.pac4j.core.config.Config.class);
      expect(pac4j.getAuthorizers()).andReturn(hash);

      Env env = unit.get(Env.class);
      expect(env.onStart(unit.capture(Throwing.Consumer.class))).andReturn(env);
      hash.values().stream()
          .filter(Pac4jAuthorizer.class::isInstance)
          .map(Pac4jAuthorizer.class::cast)
          .forEach(it -> expect(it.setRegistry(unit.get(Registry.class))).andReturn(it));
    };
  }

  private MockUnit.Block setupGuiceAuthorizers() {
    return unit -> {
      unit.captured(Throwing.Consumer.class)
          .forEach(it -> it.accept(unit.get(Registry.class)));
    };
  }

  private MockUnit.Block logoutLogic(LogoutLogic action, String defaultUrl, String logoutUrlPattern,
      boolean localLogout, boolean destroySession, boolean centralLogout) {
    return unit -> {
      org.pac4j.core.config.Config pac4j = unit.get(org.pac4j.core.config.Config.class);
      expect(pac4j.getLogoutLogic()).andReturn(action);

      if (action == null) {
        DefaultLogoutLogic defaultLogic = unit.constructor(DefaultLogoutLogic.class)
            .build();
        pac4j.setLogoutLogic(defaultLogic);
      } else {
        pac4j.setLogoutLogic(action);
      }

      Pac4jLogout filter = unit.constructor(Pac4jLogout.class)
          .build(pac4j, defaultUrl, logoutUrlPattern, localLogout, destroySession, centralLogout);

      Route.Definition route = unit.mock(Route.Definition.class);
      expect(route.name("pac4j(Logout)")).andReturn(route);

      Router router = unit.get(Router.class);
      expect(router.use("*", "/logout", filter)).andReturn(route);
    };
  }

  private MockUnit.Block sessionStore(SessionStore<WebContext> ss) {
    return unit -> {
      org.pac4j.core.config.Config config = unit.get(org.pac4j.core.config.Config.class);
      expect(config.getSessionStore()).andReturn(ss);

      if (ss != null) {
        config.setSessionStore(ss);
      }

      AnnotatedBindingBuilder aab = unit.mock(AnnotatedBindingBuilder.class);
      if (ss == null) {
        expect(aab.to(Pac4jSessionStore.class)).andReturn(aab);
        expect(aab.to(Pac4jSessionStore.class)).andReturn(aab);
      } else {
        aab.toInstance(ss);
        aab.toInstance(ss);
      }

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(SSTORE)).andReturn(aab);
      expect(binder.bind(SessionStore.class)).andReturn(aab);
    };
  }

  private MockUnit.Block profileManagerFactory(Function<WebContext, ProfileManager> pmf) {
    return unit -> {
      org.pac4j.core.config.Config pac4j = unit.get(org.pac4j.core.config.Config.class);
      expect(pac4j.getProfileManagerFactory()).andReturn(pmf);
      if (pmf == null) {
        pac4j.setProfileManagerFactory(isA(Function.class));
      } else {
        pac4j.setProfileManagerFactory(pmf);
      }
    };
  }

  private MockUnit.Block setClients(Class<? extends Client>... clientTypes) {
    return unit -> {
      List<Client> clientList = Arrays.asList(clientTypes).stream()
          .map(type -> unit.get(type))
          .collect(Collectors.toList());

      Clients clients = unit.get(Clients.class);
      clients.setClients(clientList);
      expect(clients.getClientNameParameter()).andReturn(Clients.DEFAULT_CLIENT_NAME_PARAMETER);
    };
  }

  private MockUnit.Block newLoginFormClient() {
    return unit -> {
      SimpleTestUsernamePasswordAuthenticator authenticator = unit
          .constructor(SimpleTestUsernamePasswordAuthenticator.class)
          .build();

      FormClient formClient = unit.constructor(FormClient.class)
          .build("/login", authenticator);
      unit.registerMock(FormClient.class, formClient);
    };
  }

  private MockUnit.Block newClients(String url, Class<? extends Client>... clientTypes) {
    return unit -> {
      List<Client> clientList = Arrays.asList(clientTypes).stream()
          .map(type -> unit.get(type))
          .collect(Collectors.toList());
      Clients clients = unit.constructor(Clients.class)
          .build(url, new ArrayList<>());
      expect(clients.getClients()).andReturn(clientList);

      unit.registerMock(Clients.class, clients);
    };
  }

  private MockUnit.Block callback(CallbackLogic action, String contextPath, String callback,
      boolean multiProfile, boolean renewSession) {
    return unit -> {

      org.pac4j.core.config.Config config = unit.get(org.pac4j.core.config.Config.class);
      expect(config.getCallbackLogic()).andReturn(action);

      if (action == null) {
        DefaultCallbackLogic logic = unit.constructor(DefaultCallbackLogic.class)
            .build();
        unit.registerMock(CallbackLogic.class, logic);
        config.setCallbackLogic(logic);
      } else {
        config.setCallbackLogic(action);
      }

      Route.Definition route = unit.mock(Route.Definition.class);
      expect(route.name("pac4j(Callback)")).andReturn(route);
      expect(route.excludes(Arrays.asList("/favicon.ico"))).andReturn(route);

      Router router = unit.get(Router.class);
      Pac4jCallback form = unit.constructor(Pac4jCallback.class)
          .build(unit.get(org.pac4j.core.config.Config.class), contextPath, multiProfile,
              renewSession);
      expect(router.use("*", callback, form)).andReturn(route);
    };
  }

  private MockUnit.Block securityLogic(SecurityLogic action, String pattern, String authorizers,
      String matchers, boolean multiProfile, String clientName, Set<String> excludes,
      Class<? extends Client>... clientTypes) {
    return unit -> {
      List<Client> clientList = Arrays.asList(clientTypes).stream()
          .map(type -> unit.get(type))
          .collect(Collectors.toList());

      clientList.forEach(client -> {
        expect(client.getName()).andReturn(client.getClass().getSuperclass().getSimpleName());
      });

      org.pac4j.core.config.Config config = unit.get(org.pac4j.core.config.Config.class);
      expect(config.getSecurityLogic()).andReturn(action);

      if (action != null) {
        config.setSecurityLogic(action);
      }

      if (action == null) {
        DefaultSecurityLogic logic = unit.constructor(DefaultSecurityLogic.class)
            .build();
        unit.registerMock(SecurityLogic.class, logic);
        config.setSecurityLogic(logic);
      }

      Router router = unit.get(Router.class);

      List<String> clients = clientList.stream()
          .map(it -> it.getClass().getSuperclass().getSimpleName())
          .collect(Collectors.toList());

      Pac4jSecurityFilter filter = unit.constructor(Pac4jSecurityFilter.class)
          .args(org.pac4j.core.config.Config.class, String.class, String.class, String.class,
              boolean.class, String.class, Set.class)
          .build(unit.get(org.pac4j.core.config.Config.class), clients.get(0), authorizers,
              matchers, multiProfile, clientName, excludes);
      clients.stream().skip(1)
          .forEach(it -> {
            expect(filter.addClient(it)).andReturn(filter);
          });

      Route.Definition route = unit.mock(Route.Definition.class);
      expect(route.name("pac4j(" + filter + ")")).andReturn(route);
      expect(route.excludes(Arrays.asList("/favicon.ico", "/callback"))).andReturn(route);

      expect(router.use("*", pattern, filter)).andReturn(route);
    };
  }

  private MockUnit.Block loginForm(String path) {
    return unit -> {
      Route.Definition route = unit.mock(Route.Definition.class);
      expect(route.name("pac4j(LoginForm)")).andReturn(route);

      Router router = unit.get(Router.class);
      Pac4jLoginForm form = unit.constructor(Pac4jLoginForm.class)
          .build(path);
      expect(router.get("/login", form)).andReturn(route);
    };
  }

  private MockUnit.Block profile(Class profileClass) {
    return unit -> {
      AnnotatedBindingBuilder abb = unit.mock(AnnotatedBindingBuilder.class);
      expect(abb.toProvider(isA(Provider.class))).andReturn(abb);
      abb.in(RequestScoped.class);

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(profileClass)).andReturn(abb);
    };
  }

  private Config config(String path) {
    return ConfigFactory.parseResources(Pac4j.class, "pac4j.conf")
        .withValue("application.path", ConfigValueFactory.fromAnyRef(path))
        .withValue("application.host", ConfigValueFactory.fromAnyRef("localhost"))
        .withValue("application.port", ConfigValueFactory.fromAnyRef(8080))
        .resolve();
  }

}
