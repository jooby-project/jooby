/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pac4j;

import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.jooby.internal.pac4j.ActionAdapterImpl;
import io.jooby.internal.pac4j.CallbackFilterImpl;
import io.jooby.internal.pac4j.DevLoginForm;
import io.jooby.internal.pac4j.ForwardingAuthorizer;
import io.jooby.internal.pac4j.LogoutImpl;
import io.jooby.internal.pac4j.SavedRequestHandlerImpl;
import io.jooby.internal.pac4j.SecurityFilterImpl;
import io.jooby.internal.pac4j.UrlResolverImpl;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.engine.DefaultLogoutLogic;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.engine.LogoutLogic;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.core.exception.http.ForbiddenAction;
import org.pac4j.core.exception.http.UnauthorizedAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * Pac4j module: https://jooby.io/modules/pac4j.
 *
 * Usage:
 *
 * - Add pac4j dependency
 *
 * - Add pac4j client dependency, like oauth, openid, etc... (Optional)
 *
 * - Install them
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
 * Previous example install a simple login form and give you access to profile details
 * via {@link Context#getUser()}.
 *
 * @author edgar
 * @since 2.4.0.
 */
public class Pac4jModule implements Extension {

  private static class ProtectedPath {
    private List<String> authorizers = new ArrayList<>();

    private List<Object> clients = new ArrayList<>();

    public ProtectedPath add(String authorizer, Object client) {
      this.authorizers.add(authorizer);
      this.clients.add(client);
      return this;
    }
  }

  private final Config pac4j;

  private Pac4jOptions options;

  private Map<String, ProtectedPath> clientMap;

  /**
   * Creates a new pac4j module.
   */
  public Pac4jModule() {
    this(new Pac4jOptions(), new Config());
  }

  /**
   * Creates a new pac4j module.
   *
   * @param options Options.
   * @param pac4j Pac4j advance configuration options.
   */
  public Pac4jModule(Pac4jOptions options, Config pac4j) {
    this.options = options;
    this.pac4j = pac4j;
  }

  /**
   * Creates a new pac4j module.
   *
   * @param pac4j Pac4j advance configuration options.
   */
  public Pac4jModule(Config pac4j) {
    this(new Pac4jOptions(), pac4j);
  }

  /**
   * Creates a new pac4j module.
   *
   * @param options Options.
   */
  public Pac4jModule(Pac4jOptions options) {
    this(options, new Config());
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
   * NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param authorizer Authorizer to use. Authorizer must be provisioned by application registry.
   * @param provider Client factory.
   * @return This module.
   */
  public Pac4jModule client(Class<? extends Authorizer> authorizer,
      Function<com.typesafe.config.Config, Client> provider) {
    return client("*", authorizer, provider);
  }

  /**
   * Register a default security filter.
   *
   * NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param authorizer Authorizer to use.
   * @param provider Client factory.
   * @return This module.
   */
  public @Nonnull Pac4jModule client(@Nonnull Authorizer authorizer,
      @Nonnull Function<com.typesafe.config.Config, Client> provider) {
    return client("*", authorizer, provider);
  }

  /**
   * Register a security filter under the given path.
   *
   * NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param pattern Protected pattern. Use <code>*</code> for intercept all calls.
   * @param authorizer Authorizer to use. Authorizer must be provisioned by application registry.
   * @param provider Client factory.
   * @return This module.
   */
  public @Nonnull Pac4jModule client(@Nonnull String pattern,
      @Nonnull Class<? extends Authorizer> authorizer,
      @Nonnull Function<com.typesafe.config.Config, Client> provider) {
    return client(pattern, registerAuthorizer(authorizer, new ForwardingAuthorizer(authorizer)),
        provider);
  }

  /**
   * Register a security filter under the given path.
   *
   * NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param pattern Protected pattern. Use <code>*</code> for intercept all calls.
   * @param authorizer Authorizer to use.
   * @param provider Client factory.
   * @return This module.
   */
  public @Nonnull Pac4jModule client(@Nonnull String pattern, @Nonnull Authorizer authorizer,
      @Nonnull Function<com.typesafe.config.Config, Client> provider) {
    return client(pattern, registerAuthorizer(authorizer.getClass(), authorizer), provider);
  }

  /**
   * Register a filter under the given path.
   *
   * NOTE: the authorizer is attached to the given path pattern (not the client). All the
   * authorizers added to the path applies to all the registered clients.
   *
   * @param pattern Protected pattern. Use <code>*</code> for intercept all calls.
   * @param authorizer Authorizer to use. Must be registered via
   *     {@link Config#addAuthorizer(String, Authorizer)}. Null is allowed.
   * @param provider Client factory.
   * @return This module.
   */
  public @Nonnull Pac4jModule client(@Nonnull String pattern, @Nullable String authorizer,
      @Nonnull Function<com.typesafe.config.Config, Client> provider) {
    if (clientMap == null) {
      clientMap = initializeClients(pac4j);
    }
    clientMap.computeIfAbsent(pattern, k -> new ProtectedPath()).add(authorizer, provider);
    return this;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    Clients clients = pac4j.getClients();
    /** No client? set a default one: */
    if (clients == null) {
      clients = new Clients();
    }

    /** No client instance added from DSL, init them from pac4j config. */
    if (clientMap == null) {
      clientMap = initializeClients(pac4j);
    }

    String contextPath = application.getContextPath().equals("/")
        ? ""
        : application.getContextPath();

    Map<String, List<Client>> allClients = new LinkedHashMap<>();

    /** Should add simple login form? */
    boolean devLogin = false;
    if (clientMap.isEmpty()) {
      devLogin = true;
      allClients.computeIfAbsent("*", k -> new ArrayList<>())
          .add(new FormClient(contextPath + "/login",
              new SimpleTestUsernamePasswordAuthenticator()));
    } else {
      com.typesafe.config.Config conf = application.getConfig();
      /** Initialize clients from DSL: */
      for (Map.Entry<String, ProtectedPath> routing : clientMap.entrySet()) {
        List<Client> localClients = allClients
            .computeIfAbsent(routing.getKey(), k -> new ArrayList<>());
        ProtectedPath path = routing.getValue();
        for (Object candidate : path.clients) {
          if (candidate instanceof Client) {
            localClients.add((Client) candidate);
          } else {
            Function<com.typesafe.config.Config, Client> clientProvider = (Function<com.typesafe.config.Config, Client>) candidate;
            localClients.add(clientProvider.apply(conf));
          }
        }
        allClients.put(routing.getKey(), localClients);

        // check for forwarding authorizers
        for (String authorizerName : path.authorizers) {
          Authorizer authorizer = pac4j.getAuthorizers().get(authorizerName);
          if (authorizer instanceof ForwardingAuthorizer) {
            ((ForwardingAuthorizer) authorizer).setRegistry(application);
          }
        }
      }
    }

    /** Default callback URL if none was set at client level: */
    clients.setCallbackUrl(ofNullable(clients.getCallbackUrl())
        .orElse(contextPath + options.getCallbackPath()));
    /** Default URL resolver if none was set at client level: */
    clients.setUrlResolver(
        ofNullable(clients.getUrlResolver())
            .orElseGet(() -> newUrlResolver(options.isTrustProxy())));

    /** Clients are ready, set all them: */
    clients.setClients(
        allClients.values().stream().flatMap(List::stream).collect(Collectors.toList()));
    pac4j.setClients(clients);

    /** Set default http action adapter: */
    pac4j.setHttpActionAdapter(ofNullable(pac4j.getHttpActionAdapter())
        .orElseGet(Pac4jModule::newActionAdapter));

    if (devLogin) {
      application
          .get("/login", new DevLoginForm(pac4j, options, contextPath + options.getCallbackPath()));
    }

    /** If we have multiple clients on specific paths, we collect those path and configure pac4j
     * to ignore them. So after login they are redirected to the requested url and not to one of
     * this sign-in endpoints.
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
    Set<String> excludes = allClients.keySet().stream()
        .filter(it -> !it.equals("*"))
        .map(it -> contextPath + it).collect(Collectors.toSet());

    CallbackLogic callbackLogic = pac4j.getCallbackLogic();
    if (callbackLogic == null) {
      pac4j.setCallbackLogic(newCallbackLogic(excludes));
    }

    CallbackFilterImpl callbackFilter = new CallbackFilterImpl(pac4j, options);
    application.get(options.getCallbackPath(), callbackFilter);
    application.post(options.getCallbackPath(), callbackFilter);

    SecurityLogic securityLogic = pac4j.getSecurityLogic();
    if (securityLogic == null) {
      pac4j.setSecurityLogic(newSecurityLogic(excludes));
    }

    /** For each client to a specific path, add a security handler. */
    for (Map.Entry<String, List<Client>> entry : allClients.entrySet()) {
      String pattern = entry.getKey();
      if (!pattern.equals("*")) {
        List<String> keys = Router.pathKeys(pattern);
        if (keys.size() == 0) {
          application.get(pattern, new SecurityFilterImpl(null, pac4j, options, entry.getValue(),
              clientMap.get(pattern).authorizers));
        } else {
          application.decorator(new SecurityFilterImpl(pattern, pac4j, options, entry.getValue(),
              clientMap.get(pattern).authorizers));
        }
      }
    }

    /** Is there is a global client, use it as decorator/filter (default client): */
    List<Client> defaultSecurityFilter = allClients.get("*");
    if (defaultSecurityFilter != null) {
      options.setDefaultClient(Optional.ofNullable(options.getDefaultClient())
          .orElse(defaultSecurityFilter.get(0).getName()));
      application.decorator(new SecurityFilterImpl(null, pac4j, options, defaultSecurityFilter,
          clientMap.get("*").authorizers));
    }

    /** Logout configuration: */
    LogoutLogic logoutLogic = pac4j.getLogoutLogic();
    if (logoutLogic == null) {
      pac4j.setLogoutLogic(newLogoutLogic());
    }
    application.get(options.getLogoutPath(), new LogoutImpl(pac4j, options));

    /** Better response code for some errors. */
    application.errorCode(UnauthorizedAction.class, StatusCode.UNAUTHORIZED);
    application.errorCode(ForbiddenAction.class, StatusCode.FORBIDDEN);

    /** Compute default url as next available route. We only select static path patterns. */
    if (options.getDefaultUrl() == null) {
      int index = application.getRoutes().size();
      application.onStarted(() -> {
        List<Route> routes = application.getRoutes();
        String defaultUrl = application.getContextPath();
        if (index < routes.size()) {
          Route route = routes.get(index);
          if (route.getPathKeys().isEmpty()) {
            defaultUrl = contextPath + route.getPattern();
          }
        }
        options.setDefaultUrl(defaultUrl);
      });
    }
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
  public static final DefaultCallbackLogic newCallbackLogic(Set<String> excludes) {
    DefaultCallbackLogic callbackLogic = new DefaultCallbackLogic();
    callbackLogic.setSavedRequestHandler(new SavedRequestHandlerImpl(excludes));
    return callbackLogic;
  }

  /**
   * Default url resolver.
   *
   * @param trustProxy When true, we reconstruct urls from X-Proto-* header.
   * @return Default url resolver.
   */
  public static final UrlResolver newUrlResolver(boolean trustProxy) {
    return new UrlResolverImpl(trustProxy);
  }

  private Map<String, ProtectedPath> initializeClients(Config pac4j) {
    Map<String, ProtectedPath> result = new LinkedHashMap<>();
    ofNullable(pac4j.getClients()).map(Clients::getClients)
        .map(list -> list == null ? Stream.empty() : list.stream())
        .ifPresent(list ->
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
    pac4j.getAuthorizers().putIfAbsent(authorizerName, authorizer);
    return authorizerName;
  }
}
