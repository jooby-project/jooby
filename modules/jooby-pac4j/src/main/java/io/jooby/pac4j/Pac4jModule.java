/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pac4j;

import static io.jooby.internal.pac4j.ClientReference.lazyClientNameList;
import static java.util.Optional.ofNullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.DirectClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.engine.DefaultLogoutLogic;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.engine.LogoutLogic;
import org.pac4j.core.exception.http.ForbiddenAction;
import org.pac4j.core.exception.http.UnauthorizedAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.core.profile.factory.ProfileManagerFactory;
import org.pac4j.core.util.serializer.Serializer;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.jooby.internal.pac4j.*;

/**
 * Pac4j module: https://jooby.io/modules/pac4j.
 *
 * <p>Usage:
 *
 * <p>- Add pac4j dependency
 *
 * <p>- Add pac4j client dependency, like oauth, openid, etc... (Optional)
 *
 * <p>- Install them
 *
 * <pre>{@code
 * {
 *   install(new Pac4jModule());
 * }
 * }</pre>
 *
 * - Use it
 *
 * <pre>{@code
 * {
 *
 *   get("/", ctx -> {
 *     UserProfile user = ctx.getUser();
 *   });
 *
 * }
 * }</pre>
 *
 * Previous example install a simple login form and give you access to profile details via {@link
 * Context#getUser()}.
 *
 * @author edgar
 * @since 2.4.0.
 */
public class Pac4jModule implements Extension {

  private static class ProtectedPath {
    private final List<String> authorizers = new ArrayList<>();

    private final List<Object> clients = new ArrayList<>();

    public void add(String authorizer, Object client) {
      this.authorizers.add(authorizer);
      this.clients.add(client);
    }
  }

  private final Pac4jOptions options;

  private Map<String, ProtectedPath> clientMap;

  /** Creates a new pac4j module. */
  public Pac4jModule() {
    this(new Pac4jOptions());
  }

  /**
   * Creates a new pac4j module.
   *
   * @param options Options.
   */
  public Pac4jModule(Pac4jOptions options) {
    this.options = options;
  }

  /**
   * Creates a new pac4j module.
   *
   * @param options Options.
   */
  public Pac4jModule(Config options) {
    this(Pac4jOptions.from(options));
  }

  /**
   * Register a default security filter.
   *
   * @param provider Client factory.
   * @return This module.
   */
  public Pac4jModule client(Function<com.typesafe.config.Config, Client> provider) {
    return client("*", provider);
  }

  /**
   * Register a security filter under the given path.
   *
   * @param pattern Protected pattern.
   * @param provider Client factory.
   * @return This module.
   */
  public Pac4jModule client(String pattern, Function<com.typesafe.config.Config, Client> provider) {
    return client(pattern, (String) null, provider);
  }

  /**
   * Register a default security filter.
   *
   * <p>NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param authorizer Authorizer to use. Authorizer must be provisioned by application registry.
   * @param provider Client factory.
   * @return This module.
   */
  public Pac4jModule client(
      Class<? extends Authorizer> authorizer,
      Function<com.typesafe.config.Config, Client> provider) {
    return client("*", authorizer, provider);
  }

  /**
   * Register a default security filter.
   *
   * <p>NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param authorizer Authorizer to use.
   * @param provider Client factory.
   * @return This module.
   */
  public @NonNull Pac4jModule client(
      @NonNull Authorizer authorizer,
      @NonNull Function<com.typesafe.config.Config, Client> provider) {
    return client("*", authorizer, provider);
  }

  /**
   * Register a security filter under the given path.
   *
   * <p>NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param pattern Protected pattern. Use <code>*</code> for intercept all calls.
   * @param authorizer Authorizer to use. Authorizer must be provisioned by application registry.
   * @param provider Client factory.
   * @return This module.
   */
  public @NonNull Pac4jModule client(
      @NonNull String pattern,
      @NonNull Class<? extends Authorizer> authorizer,
      @NonNull Function<com.typesafe.config.Config, Client> provider) {
    return client(
        pattern, registerAuthorizer(authorizer, new ForwardingAuthorizer(authorizer)), provider);
  }

  /**
   * Register a security filter under the given path.
   *
   * <p>NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param pattern Protected pattern. Use <code>*</code> for intercept all calls.
   * @param authorizer Authorizer to use.
   * @param provider Client factory.
   * @return This module.
   */
  public @NonNull Pac4jModule client(
      @NonNull String pattern,
      @NonNull Authorizer authorizer,
      @NonNull Function<com.typesafe.config.Config, Client> provider) {
    return client(pattern, registerAuthorizer(authorizer.getClass(), authorizer), provider);
  }

  /**
   * Register a filter under the given path.
   *
   * <p>NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param pattern Protected pattern. Use <code>*</code> for intercept all calls.
   * @param authorizer Authorizer to use. Must be registered via {@link Config#addAuthorizer(String,
   *     Authorizer)}. Null is allowed.
   * @param provider Client factory.
   * @return This module.
   */
  public @NonNull Pac4jModule client(
      @NonNull String pattern,
      @Nullable String authorizer,
      @NonNull Function<com.typesafe.config.Config, Client> provider) {
    if (clientMap == null) {
      clientMap = initializeClients(options);
    }
    clientMap.computeIfAbsent(pattern, k -> new ProtectedPath()).add(authorizer, provider);
    return this;
  }

  /**
   * Register a default security filter.
   *
   * @param client Client class.
   * @return This module.
   */
  public Pac4jModule client(@NonNull Class<? extends Client> client) {
    return client("*", client);
  }

  /**
   * Register a security filter under the given path.
   *
   * @param pattern Protected pattern.
   * @param client Client class.
   * @return This module.
   */
  public Pac4jModule client(@NonNull String pattern, @NonNull Class<? extends Client> client) {
    return client(pattern, (String) null, client);
  }

  /**
   * Register a default security filter.
   *
   * <p>NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param authorizer Authorizer to use. Authorizer must be provisioned by application registry.
   * @param client Client class.
   * @return This module.
   */
  public Pac4jModule client(
      @NonNull Class<? extends Authorizer> authorizer, @NonNull Class<? extends Client> client) {
    return client("*", authorizer, client);
  }

  /**
   * Register a default security filter.
   *
   * <p>NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param authorizer Authorizer to use.
   * @param client Client class.
   * @return This module.
   */
  public @NonNull Pac4jModule client(
      @NonNull Authorizer authorizer, @NonNull Class<? extends Client> client) {
    return client("*", authorizer, client);
  }

  /**
   * Register a security filter under the given path.
   *
   * <p>NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param pattern Protected pattern. Use <code>*</code> for intercept all calls.
   * @param authorizer Authorizer to use. Authorizer must be provisioned by application registry.
   * @param client Client class.
   * @return This module.
   */
  public @NonNull Pac4jModule client(
      @NonNull String pattern,
      @NonNull Class<? extends Authorizer> authorizer,
      @NonNull Class<? extends Client> client) {
    return client(
        pattern, registerAuthorizer(authorizer, new ForwardingAuthorizer(authorizer)), client);
  }

  /**
   * Register a security filter under the given path.
   *
   * <p>NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param pattern Protected pattern. Use <code>*</code> for intercept all calls.
   * @param authorizer Authorizer to use.
   * @param client Client class.
   * @return This module.
   */
  public @NonNull Pac4jModule client(
      @NonNull String pattern,
      @NonNull Authorizer authorizer,
      @NonNull Class<? extends Client> client) {
    return client(pattern, registerAuthorizer(authorizer.getClass(), authorizer), client);
  }

  /**
   * Register a filter under the given path.
   *
   * <p>NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param pattern Protected pattern. Use <code>*</code> for intercept all calls.
   * @param authorizer Authorizer to use. Must be registered via {@link Config#addAuthorizer(String,
   *     Authorizer)}. Null is allowed.
   * @param client Client class.
   * @return This module.
   */
  public @NonNull Pac4jModule client(
      @NonNull String pattern,
      @Nullable String authorizer,
      @NonNull Class<? extends Client> client) {
    if (clientMap == null) {
      clientMap = initializeClients(options);
    }
    clientMap.computeIfAbsent(pattern, k -> new ProtectedPath()).add(authorizer, client);
    return this;
  }

  @Override
  public void install(@NonNull Jooby app) throws Exception {
    var services = app.getServices();
    services.putIfAbsent(Pac4jOptions.class, options);
    app.getServices().put(Config.class, options);

    // Set defaults:
    services.putIfAbsent(Serializer.class, options.getSerializer());

    var clients =
        ofNullable(options.getClients())
            /* No client? set a default one: */
            .orElseGet(Clients::new);

    /* No client instance added from DSL, init them from pac4j config. */
    if (clientMap == null) {
      clientMap = initializeClients(options);
    }

    var contextPath = app.getContextPath().equals("/") ? "" : app.getContextPath();

    Map<String, List<ClientReference>> allClients = new LinkedHashMap<>();

    /* Should add simple login form? */
    var devLogin = false;
    if (clientMap.isEmpty()) {
      devLogin = true;
      client(
          conf ->
              new FormClient(
                  contextPath + "/login", new SimpleTestUsernamePasswordAuthenticator()));
    }
    var conf = app.getConfig();
    /* Initialize clients from DSL: */
    for (var routing : clientMap.entrySet()) {
      var localClients = allClients.computeIfAbsent(routing.getKey(), k -> new ArrayList<>());
      var path = routing.getValue();
      for (var candidate : path.clients) {
        if (candidate instanceof Class) {
          localClients.add(new ClientReference((Class<Client>) candidate));
        } else if (candidate instanceof Client) {
          localClients.add(new ClientReference((Client) candidate));
        } else {
          Function<com.typesafe.config.Config, Client> clientProvider =
              (Function<com.typesafe.config.Config, Client>) candidate;
          localClients.add(new ClientReference(clientProvider.apply(conf)));
        }
      }
      allClients.put(routing.getKey(), localClients);

      // check for forwarding authorizers
      for (var authorizerName : path.authorizers) {
        Authorizer authorizer = options.getAuthorizers().get(authorizerName);
        if (authorizer instanceof ForwardingAuthorizer) {
          ((ForwardingAuthorizer) authorizer).setRegistry(app);
        }
      }
    }

    options.getAuthorizers().put(NoopAuthorizer.NAME, new NoopAuthorizer());

    /* Default callback URL if none was set at client level: */
    clients.setCallbackUrl(
        ofNullable(clients.getCallbackUrl()).orElse(contextPath + options.getCallbackPath()));
    /* Default URL resolver if none was set at client level: */
    clients.setUrlResolver(
        ofNullable(clients.getUrlResolver()).orElseGet(Pac4jModule::newUrlResolver));

    /* Set resolved clients: */
    clients.setClients(
        allClients.values().stream()
            .flatMap(List::stream)
            .filter(ClientReference::isResolved)
            .map(ClientReference::getClient)
            .collect(Collectors.toList()));
    options.setClients(clients);

    /* Delay setting unresolved clients: */
    var unresolved =
        allClients.values().stream().flatMap(List::stream).filter(r -> !r.isResolved()).toList();

    if (!unresolved.isEmpty()) {
      app.onStarting(
          () -> {
            List<Client> clientList = new ArrayList<>(clients.getClients());
            unresolved.stream()
                .peek(r -> r.resolve(app::require))
                .map(ClientReference::getClient)
                .forEachOrdered(clientList::add);
            clients.setClients(clientList);

            /** If the global client was unresolved at initialization, try to set it now: */
            List<ClientReference> defaultSecurityFilter = allClients.get("*");
            if (defaultSecurityFilter != null && options.getDefaultClient() == null) {
              options.setDefaultClient(defaultSecurityFilter.get(0).getClient().getName());
            }
          });
    }

    /* Set default http action adapter: */
    options.setHttpActionAdapter(
        ofNullable(options.getHttpActionAdapter()).orElseGet(Pac4jModule::newActionAdapter));

    /* WebContextFactory: */
    options.setWebContextFactory(
        ofNullable(options.getWebContextFactory()).orElseGet(ContextFactoryImpl::new));
    options.setSessionStoreFactory(
        ofNullable(options.getSessionStoreFactory()).orElseGet(SessionStoreFactoryImpl::new));
    options.setProfileManagerFactory(
        ofNullable(options.getProfileManagerFactory()).orElse(ProfileManagerFactory.DEFAULT));

    if (devLogin) {
      app.get("/login", new DevLoginForm(options, contextPath + options.getCallbackPath()));
    }

    /*
     * If we have multiple clients on specific paths, we collect those path and configure pac4j to
     * ignore them. So after login they are redirected to the requested url and not to one of this
     * sign-in endpoints.
     *
     * <pre>
     *   install(new Pac4jModule()
     *     .client("/google", ...)
     *     .client("/twitter", ....);
     *   );
     * </pre>
     *
     * So <code>google</code> and <code>twitter</code> paths are never saved as requested urls.
     */
    var excludes =
        allClients.keySet().stream()
            .filter(it -> !it.equals("*"))
            .map(it -> contextPath + it)
            .collect(Collectors.toSet());

    var callbackLogic = options.getCallbackLogic();
    if (callbackLogic == null) {
      options.setCallbackLogic(newCallbackLogic(excludes));
    }
    var direct = clients.getClients().stream().allMatch(it -> it instanceof DirectClient);
    if (!direct || options.isForceCallbackRoutes()) {
      CallbackFilterImpl callbackFilter = new CallbackFilterImpl(options, options);
      app.get(options.getCallbackPath(), callbackFilter);
      app.post(options.getCallbackPath(), callbackFilter);
    }

    var securityLogic = options.getSecurityLogic();
    if (securityLogic == null) {
      options.setSecurityLogic(newSecurityLogic(excludes));
    }
    app.use(new UntrustedSessionDataDetector());

    /** For each client to a specific path, add a security handler. */
    for (var entry : allClients.entrySet()) {
      String pattern = entry.getKey();
      if (!pattern.equals("*")) {
        List<String> keys = Router.pathKeys(pattern);
        if (keys.isEmpty()) {
          SecurityFilterImpl securityFilter =
              new SecurityFilterImpl(
                  null,
                  options,
                  options,
                  lazyClientNameList(entry.getValue()),
                  clientMap.get(pattern).authorizers);
          app.get(pattern, securityFilter);
          // POST for direct authentication
          app.post(pattern, securityFilter);
        } else {
          app.use(
              new SecurityFilterImpl(
                  pattern,
                  options,
                  options,
                  lazyClientNameList(entry.getValue()),
                  clientMap.get(pattern).authorizers));
        }
      }
    }

    /* Is there is a global client, use it as decorator/filter (default client): */
    var defaultSecurityFilter = allClients.get("*");
    if (defaultSecurityFilter != null) {
      if (options.getDefaultClient() == null && defaultSecurityFilter.get(0).isResolved()) {
        options.setDefaultClient(defaultSecurityFilter.get(0).getClient().getName());
      }
      app.use(
          new SecurityFilterImpl(
              null,
              options,
              options,
              lazyClientNameList(defaultSecurityFilter),
              clientMap.get("*").authorizers));
    }

    /* Logout configuration: */
    var logoutLogic = options.getLogoutLogic();
    if (logoutLogic == null) {
      options.setLogoutLogic(newLogoutLogic());
    }
    if (!direct || options.isForceLogoutRoutes()) {
      app.get(options.getLogoutPath(), new LogoutImpl(options, options));
    }

    /* Better response code for some errors. */
    app.errorCode(UnauthorizedAction.class, StatusCode.UNAUTHORIZED);
    app.errorCode(ForbiddenAction.class, StatusCode.FORBIDDEN);

    /* Compute default url as next available route. We only select static path patterns. */
    options.setDefaultUrl(Optional.ofNullable(options.getDefaultUrl()).orElse("/"));

    /* Set current user provider */
    app.setCurrentUser(new Pac4jCurrentUser(options));
    // cleanup
    clientMap.clear();
  }

  /**
   * Creates a default logout logic.
   *
   * @return Default logout logic.
   */
  public static LogoutLogic newLogoutLogic() {
    return new DefaultLogoutLogic();
  }

  /**
   * Default action adapter.
   *
   * @return Default action adapter.
   */
  public static HttpActionAdapter newActionAdapter() {
    return new ActionAdapterImpl();
  }

  /**
   * Default security logic and optionally specify the pattern to excludes while saving user
   * requested urls.
   *
   * @param excludes Pattern to ignores.
   * @return Default security logic.
   */
  public static DefaultSecurityLogic newSecurityLogic(Set<String> excludes) {
    DefaultSecurityLogic securityLogic = new DefaultSecurityLogic();
    securityLogic.setSavedRequestHandler(new SavedRequestHandlerImpl(excludes));
    return securityLogic;
  }

  /**
   * Default callback logic and optionally specify the pattern to excludes while saving user
   * requested urls.
   *
   * @param excludes Pattern to ignores.
   * @return Default callback logic.
   */
  public static DefaultCallbackLogic newCallbackLogic(Set<String> excludes) {
    DefaultCallbackLogic callbackLogic = new DefaultCallbackLogic();
    callbackLogic.setSavedRequestHandler(new SavedRequestHandlerImpl(excludes));
    return callbackLogic;
  }

  /**
   * Default url resolver.
   *
   * @return Default url resolver.
   */
  public static UrlResolver newUrlResolver() {
    return new UrlResolverImpl();
  }

  private Map<String, ProtectedPath> initializeClients(Config pac4j) {
    Map<String, ProtectedPath> result = new LinkedHashMap<>();
    ofNullable(pac4j.getClients())
        .map(Clients::getClients)
        .map(list -> list == null ? Stream.empty() : list.stream())
        .ifPresent(
            list ->
                list.forEach(
                    it -> result.computeIfAbsent("*", k -> new ProtectedPath()).add(null, it)));

    return result;
  }

  private String authorizerName(Class authorizer) {
    String name = authorizer.getSimpleName();
    // lambda doesn't have a simple name
    return name.isEmpty() ? authorizer.getName() : name;
  }

  private String registerAuthorizer(Class type, Authorizer authorizer) {
    String authorizerName = authorizerName(type);
    options.getAuthorizers().putIfAbsent(authorizerName, authorizer);
    return authorizerName;
  }
}
