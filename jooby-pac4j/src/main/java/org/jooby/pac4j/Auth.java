/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.pac4j;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Route;
import org.jooby.Router;
import org.jooby.Session;
import org.jooby.internal.pac4j.AuthCallback;
import org.jooby.internal.pac4j.AuthContext;
import org.jooby.internal.pac4j.AuthFilter;
import org.jooby.internal.pac4j.AuthLogout;
import org.jooby.internal.pac4j.AuthorizerFilter;
import org.jooby.internal.pac4j.BasicAuth;
import org.jooby.internal.pac4j.ClientType;
import org.jooby.internal.pac4j.ClientsProvider;
import org.jooby.internal.pac4j.ConfigProvider;
import org.jooby.internal.pac4j.FormAuth;
import org.jooby.internal.pac4j.FormFilter;
import org.jooby.scope.Providers;
import org.jooby.scope.RequestScoped;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.authorization.checker.AuthorizationChecker;
import org.pac4j.core.authorization.checker.DefaultAuthorizationChecker;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.finder.ClientFinder;
import org.pac4j.core.client.finder.DefaultClientFinder;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * <h1>pac4j module</h1>
 * <p>
 * Authentication module via: <a href="https://github.com/pac4j/pac4j">pac4j</a>.
 * </p>
 *
 * <h2>exposes</h2>
 * <ul>
 * <li>{@link Clients}</li>
 * <li>{@link WebContext} as {@link RequestScoped}</li>
 * <li>{@link Route.Filter} per each registered {@link Client}</li>
 * <li>Callback {@link Route.Filter}</li>
 * </ul>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *
 *   get("/public", () {@literal ->} ..);
 *
 *   use(new Auth());
 *
 *   get("/private", () {@literal ->} ..);
 * }
 * </pre>
 *
 * <p>
 * Previous example adds a very basic but ready to use form login auth every time you try to access
 * to <code>/private</code> or any route defined below the auth module.
 * </p>
 *
 * <h2>clients</h2>
 * <p>
 * <a href="https://github.com/pac4j/pac4j">pac4j</a> is a powerful library that supports multiple
 * clients and/or authentication protocols. In the next example, we will see how to configure the
 * most basic of them, but also some complex protocols.
 * </p>
 *
 * <h3>basic auth</h3>
 * <p>
 * If basic auth is all you need, then:
 * </p>
 *
 * <pre>
 * {
 *   use(new Auth().basic());
 * }
 * </pre>
 *
 * <p>
 * A {@link IndirectBasicAuthClient} depends on {@link Authenticator}, default is
 * {@link SimpleTestUsernamePasswordAuthenticator} which is great for development, but nothing good
 * for other environments. Next example setup a basic auth with a custom {@link Authenticator}:
 * </p>
 *
 * <pre>
 * {
 *   use(new Auth().basic("*", MyUsernamePasswordAuthenticator.class));
 * }
 * </pre>
 *
 * <h3>form auth</h3>
 * <p>
 * Form authentication will be activated by calling {@link #form()}:
 * </p>
 *
 * <pre>
 * {
 *   use(new Auth().form());
 * }
 * </pre>
 *
 * <p>
 * Form is the default authentication method so previous example is the same as:
 * </p>
 *
 * <pre>
 * {
 *   use(new Auth());
 * }
 * </pre>
 *
 * <p>
 * Like basic auth, form auth depends on a {@link Authenticator}.
 * </p>
 *
 * <p>
 * A login form will be ready under the path: <code>/login</code>. Again, it is a very basic login
 * form useful for development. If you need a custom login page, just add a route before the
 * {@link Auth} module, like:
 * </p>
 *
 * <pre>
 * {
 *   get("/login", () {@literal ->} Results.html("login"));
 *
 *   use(new Auth());
 * }
 * </pre>
 *
 * <p>
 * Simply and easy!
 * </p>
 *
 * <h3>oauth, openid, etc...</h3>
 *
 * <p>
 * Twitter, example:
 * </p>
 *
 * <pre>
 * {
 *   use(new Auth()
 *     .client(conf {@literal ->}
 *        new TwitterClient(conf.getString("twitter.key"), conf.getString("twitter.secret"))));
 * }
 * </pre>
 * <p>
 * Keep in mind you will have to add the require Maven dependency to your project, beside that it is
 * pretty straight forward.
 * </p>
 *
 * <h2>protecting urls</h2>
 * <p>
 * By default a {@link Client} will protect all the urls defined below the module, because routes in
 * {@link Jooby} are executed in the order they where defined.
 * </p>
 * <p>
 * You can customize what urls are protected by specifying a path pattern:
 * </p>
 *
 * <pre>
 * {
 *   use(new Auth().form("/private/**"));
 *
 *   get("/hello", () {@literal ->} "no auth");
 *
 *   get("/private", () {@literal ->} "auth");
 * }
 * </pre>
 *
 * <p>
 * Here the <code>/hello</code> path is un-protected, because the client will intercept everything
 * under <code>/private</code>.
 * </p>
 *
 * <h2>user profile</h2>
 * <p>
 * Jooby relies on {@link AuthStore} for saving and retrieving a {@link CommonProfile}. By default,
 * the {@link CommonProfile} is stored in the {@link Session} via {@link AuthSessionStore}.
 * </p>
 * <p>
 * After a successful authentication the {@link CommonProfile} is accessible as a request scoped
 * attribute:
 * </p>
 *
 * <pre>
 * {
 *   use(new Auth().form());
 *
 *   get("/private", req {@literal ->} req.require(HttpProfile.class));
 * }
 * </pre>
 *
 * facebook (or any oauth, openid, etc...)
 *
 * <pre>
 * {
 *   use(new Auth().client(new FacebookClient(key, secret));
 *
 *   get("/private", req {@literal ->} req.require(FacebookProfile.class));
 * }
 * </pre>
 *
 * <p>
 * Custom {@link AuthStore} is provided via {@link Auth#store(Class)} method:
 * </p>
 *
 * <pre>
 * {
 *   use(new Auth().store(MyDbStore.class));
 *
 *   get("/private", req {@literal ->} req.require(HttpProfile.class));
 * }
 * </pre>
 *
 * <h2>logout</h2>
 * <p>
 * A default <code>/logout</code> handler is provided it too. The handler will remove the profile
 * from {@link AuthStore} by calling the {@link AuthStore#unset(String)} method. The default login
 * will redirect to <code>/</code>.
 * </p>
 * <p>
 * A custom logout and redirect urls can be set via <code>.conf</code> file or programmatically:
 * </p>
 *
 * <pre>
 * {
 *   use(new Auth().logout("/mylogout", "/redirectTo"));
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.6.0
 */
public class Auth implements Jooby.Module {

  /** Name of the local request variable that holds the username. */
  public static final String ID = Auth.class.getName() + ".id";

  public static final String CNAME = Auth.class.getName() + ".client.id";

  private Multimap<String, BiFunction<Binder, Config, AuthFilter>> bindings = ArrayListMultimap
      .create();

  @SuppressWarnings("rawtypes")
  private Class<? extends AuthStore> storeClass = AuthSessionStore.class;

  private Optional<String> logoutUrl = Optional.empty();

  private Optional<String> redirecTo = Optional.empty();

  private Multimap<String, Map.Entry<String, Object>> authorizers = ArrayListMultimap.create();

  private Set<Object> bindedProfiles = new HashSet<>();

  /**
   * Protect one or more urls with an {@link Authorizer}. For example:
   *
   * <pre>
   * {
   *   use(new Auth()
   *     .form("*")
   *     .authorizer("admin", "/admin/**", new RequireAnyRoleAuthorizer("admin"))
   *     );
   * }
   * </pre>
   *
   * <p>
   * Previous example will protect any url with form authentication and require and admin role for
   * <code>/admin/</code> or subpath of it.
   * </p>
   * <p>
   * NOTE: make sure url is protected by one pac4j client.
   * </p>
   *
   * @param name Authorizer name.
   * @param pattern URL pattern to protected.
   * @param authorizer Authorizer to apply.
   * @return This module.
   */
  public Auth authorizer(final String name, final String pattern,
      final Authorizer<?> authorizer) {
    authorizer(authorizer, name, pattern);
    return this;
  }

  /**
   * Protect one or more urls with an {@link Authorizer}. For example:
   *
   * <pre>
   * {
   *   use(new Auth()
   *     .form("*")
   *     .authorizer("admin", "/admin/**", MyAuthorizer.class)
   *     );
   * }
   * </pre>
   *
   * <p>
   * Previous example will protect any url with form authentication and require and admin role for
   * <code>/admin/</code> or subpath of it.
   * </p>
   * <p>
   * NOTE: make sure url is protected by one pac4j client.
   * </p>
   *
   * @param name Authorizer name.
   * @param pattern URL pattern to protected.
   * @param authorizer Authorizer to apply.
   * @return This module.
   */
  @SuppressWarnings("rawtypes")
  public Auth authorizer(final String name, final String pattern,
      final Class<? extends Authorizer> authorizer) {
    authorizer(authorizer, name, pattern);
    return this;
  }

  /**
   * Protect one or more urls with an {@link Authorizer}. For example:
   *
   * <pre>
   * {
   *   use(new Auth()
   *     .form("*")
   *     .authorizer("admin", "/admin/**", MyAuthorizer.class)
   *     );
   * }
   * </pre>
   *
   * <p>
   * Previous example will protect any url with form authentication and require and admin role for
   * <code>/admin/</code> or subpath of it.
   * </p>
   *
   * @param name Authorizer name.
   * @param pattern URL pattern to protected.
   * @param authorizer Authorizer to apply.
   */
  private void authorizer(final Object authorizer, final String name, final String pattern) {
    requireNonNull(name, "An authorizer's name is required.");
    requireNonNull(pattern, "An authorizer's pattern is required.");
    requireNonNull(authorizer, "An authorizer is required.");
    authorizers.put(pattern, Maps.immutableEntry(name, authorizer));
  }

  /**
   * Add a form auth client.
   *
   * @param pattern URL pattern to protect.
   * @param authenticator Authenticator to use.
   * @return This module.
   */
  public Auth form(final String pattern,
      final Class<? extends Authenticator<UsernamePasswordCredentials>> authenticator) {
    bindings.put(pattern, (binder, conf) -> {
      TypeLiteral<Authenticator<UsernamePasswordCredentials>> usernamePasswordAuthenticator = new TypeLiteral<Authenticator<UsernamePasswordCredentials>>() {};
      binder.bind(usernamePasswordAuthenticator.getRawType()).to(authenticator);

      bindProfile(binder, CommonProfile.class);

      Multibinder.newSetBinder(binder, Client.class)
          .addBinding().toProvider(FormAuth.class);

      return new FormFilter(conf.getString("auth.form.loginUrl"), conf.getString("auth.callback"));
    });

    return this;
  }

  /**
   * Add a form auth client. It setup a {@link SimpleTestUsernamePasswordAuthenticator}.
   * Useful for development.
   *
   * @param pattern URL pattern to protect.
   * @return This module.
   */
  public Auth form(final String pattern) {
    return form(pattern, SimpleTestUsernamePasswordAuthenticator.class);
  }

  /**
   * Add a form auth client, protecting all the urls <code>*</code>. It setup a
   * {@link SimpleTestUsernamePasswordAuthenticator}. Useful for development.
   *
   * @return This module.
   */
  public Auth form() {
    return form("*");
  }

  /**
   * Add a basic auth client.
   *
   * @param pattern URL pattern to protect.
   * @param authenticator Authenticator to use.
   * @return This module.
   */
  public Auth basic(final String pattern,
      final Class<? extends Authenticator<UsernamePasswordCredentials>> authenticator) {
    bindings.put(pattern, (binder, config) -> {
      TypeLiteral<Authenticator<UsernamePasswordCredentials>> usernamePasswordAuthenticator = new TypeLiteral<Authenticator<UsernamePasswordCredentials>>() {};
      binder.bind(usernamePasswordAuthenticator.getRawType()).to(authenticator);

      bindProfile(binder, CommonProfile.class);

      Multibinder.newSetBinder(binder, Client.class)
          .addBinding().toProvider(BasicAuth.class);

      return new AuthFilter(IndirectBasicAuthClient.class, CommonProfile.class);
    });
    return this;
  }

  /**
   * Add a basic auth client. It setup a {@link SimpleTestUsernamePasswordAuthenticator}. Useful
   * for development.
   *
   * @param pattern URL pattern to protect.
   * @return This module.
   */
  public Auth basic(final String pattern) {
    return basic(pattern, SimpleTestUsernamePasswordAuthenticator.class);
  }

  /**
   * Add a basic auth client, protecting all the urls <code>*</code>. It setup a
   * {@link SimpleTestUsernamePasswordAuthenticator}. Useful for development.
   *
   * @return This module.
   */
  public Auth basic() {
    return basic("*");
  }

  /**
   * Add an auth client, like facebook, twitter, github, etc...Please note the require dependency
   * must be in the classpath.
   *
   * @param client Client to add.
   * @param <C> Credentials.
   * @param <U> CommonProfile.
   * @return This module.
   */
  public <C extends Credentials, U extends CommonProfile> Auth client(final Client<C, U> client) {
    return client("*", client);
  }

  /**
   * Add an auth client, like facebook, twitter, github, etc...Please note the require dependency
   * must be in the classpath.
   *
   * @param client Client to add.
   * @param <C> Credentials.
   * @param <U> CommonProfile.
   * @return This module.
   */
  public <C extends Credentials, U extends CommonProfile> Auth client(
      final Class<? extends Client<C, U>> client) {
    return client("*", client);
  }

  /**
   * Add an auth client, like facebook, twitter, github, etc...Please note the require dependency
   * must be in the classpath.
   *
   * @param pattern URL pattern to protect.
   * @param client Client to add.
   * @param <C> Credentials.
   * @param <U> CommonProfile.
   * @return This module.
   */
  public <C extends Credentials, U extends CommonProfile> Auth client(final String pattern,
      final Client<C, U> client) {
    return client(pattern, config -> client);
  }

  /**
   * Add an auth client, like facebook, twitter, github, etc...Please note the require dependency
   * must be in the classpath.
   *
   * @param provider Client to add.
   * @param <C> Credentials.
   * @param <U> CommonProfile.
   * @return This module.
   */
  public <C extends Credentials, U extends CommonProfile> Auth client(
      final Function<Config, Client<C, U>> provider) {
    return client("*", provider);
  }

  /**
   * Add an auth client, like facebook, twitter, github, etc...Please note the require dependency
   * must be in the classpath.
   *
   * @param pattern URL pattern to protect.
   * @param provider Client to add.
   * @param <C> Credentials.
   * @param <U> CommonProfile.
   * @return This module.
   */
  @SuppressWarnings({"unchecked", "rawtypes" })
  public <C extends Credentials, U extends CommonProfile> Auth client(final String pattern,
      final Function<Config, Client<C, U>> provider) {
    bindings.put(pattern, (binder, config) -> {

      Client<C, U> client = provider.apply(config);
      Multibinder.newSetBinder(binder, Client.class)
          .addBinding().toInstance(client);
      Class clientType = client.getClass();
      Class profileType = ClientType.typeOf(clientType);

      bindProfile(binder, profileType);

      return new AuthFilter(clientType, profileType);
    });
    return this;
  }

  /**
   * Add an auth client, like facebook, twitter, github, etc...Please note the require dependency
   * must be in the classpath.
   *
   * @param pattern URL pattern to protect.
   * @param client Client to add.
   * @param <C> Credentials.
   * @param <U> CommonProfile.
   * @return This module.
   */
  @SuppressWarnings({"rawtypes", "unchecked" })
  public <C extends Credentials, U extends CommonProfile> Auth client(final String pattern,
      final Class<? extends Client<C, U>> client) {
    bindings.put(pattern, (binder, config) -> {

      Multibinder.newSetBinder(binder, Client.class)
          .addBinding().to(client);

      Class profileType = ClientType.typeOf(client);

      bindProfile(binder, profileType);

      return new AuthFilter(client, profileType);

    });
    return this;
  }

  /**
   * Setup the {@link AuthStore} to use. Keep in mind the store is binded it as singleton.
   *
   * @param store Store to use.
   * @return This module.
   */
  public <U extends CommonProfile> Auth store(final Class<? extends AuthStore<U>> store) {
    this.storeClass = requireNonNull(store, "Store is required.");
    return this;
  }

  /**
   * Set the logout and redirect URL patterns.
   *
   * @param logoutUrl Logout url, default is <code>/logout</code>.
   * @param redirecTo Redirect url, default is <code>/</code>.
   * @return This module.
   */
  public Auth logout(final String logoutUrl, final String redirecTo) {
    this.logoutUrl = Optional.of(logoutUrl);
    this.redirecTo = Optional.of(redirecTo);

    return this;
  }

  /**
   * Set the logout and redirect URL patterns.
   *
   * @param logoutUrl Logout url, default is <code>/logout</code>.
   * @return This module.
   */
  public Auth logout(final String logoutUrl) {
    this.logoutUrl = Optional.of(logoutUrl);
    this.redirecTo = Optional.empty();

    return this;
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    binder.bind(Clients.class).toProvider(ClientsProvider.class);

    binder.bind(org.pac4j.core.config.Config.class).toProvider(ConfigProvider.class);

    OptionalBinder.newOptionalBinder(binder, AuthorizationChecker.class)
        .setDefault().toInstance(new DefaultAuthorizationChecker());

    OptionalBinder.newOptionalBinder(binder, ClientFinder.class)
        .setDefault().toInstance(new DefaultClientFinder());

    String fullcallback = config.getString("auth.callback");
    String callback = URI.create(fullcallback).getPath();

    Router routes = env.router();

    MapBinder<String, Authorizer> authorizers = MapBinder
        .newMapBinder(binder, String.class, Authorizer.class);

    routes.use("*", callback, (req, rsp, chain) -> req
        .require(AuthCallback.class).handle(req, rsp, chain))
        .excludes("/favicon.ico")
        .name("auth(Callback)");

    routes.use("*", logoutUrl.orElse(config.getString("auth.logout.url")),
        new AuthLogout(redirecTo.orElse(config.getString("auth.logout.redirectTo"))))
        .name("auth(Logout)");

    if (bindings.size() == 0) {
      // no auth client, go dev friendly and add a form auth
      form();
    }
    // bindings.values().forEach(it -> it.accept(binder, config));
    bindings.asMap().entrySet().forEach(e -> {
      String pattern = e.getKey();
      List<AuthFilter> filters = new ArrayList<>();
      e.getValue().forEach(it -> {
        AuthFilter filter = it.apply(binder, config);
        if (filters.size() == 0) {
          // push 1st filter
          filters.add(filter);
        } else {
          // push recentely created filter to head and discard it.
          filters.get(0).setName(filter.getName());
        }
      });
      AuthFilter head = filters.get(0);
      routes.use("*", pattern, head).name("auth(" + head.getName() + ")").excludes("/favicon.ico");
    });

    binder.bind(AuthCallback.class);

    binder.bind(AuthStore.class).to(storeClass);

    binder.bind(WebContext.class).to(AuthContext.class).in(RequestScoped.class);

    this.authorizers.asMap().entrySet().forEach(e -> {
      String pattern = e.getKey();
      String names = e.getValue().stream()
          .map(Map.Entry::getKey)
          .collect(Collectors.joining(Pac4jConstants.ELEMENT_SEPRATOR));
      // bind route
      routes.use("*", pattern, new AuthorizerFilter(names))
          .name("auth(" + names + ")");

      // bind authorizers
      e.getValue().forEach(v -> {
        String name = v.getKey();
        Object authorizer = v.getValue();
        if (authorizer instanceof Authorizer) {
          authorizers.addBinding(name).toInstance((Authorizer) authorizer);
        } else {
          authorizers.addBinding(name).to((Class) authorizer);
        }
      });
    });
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "auth.conf");
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private void bindProfile(final Binder binder, final Class root) {
    Class profile = root;
    while (profile != Object.class) {
      if (bindedProfiles.add(profile)) {
        binder.bind(profile).toProvider(Providers.outOfScope(profile)).in(RequestScoped.class);
      }
      profile = profile.getSuperclass();
    }
  }

}
