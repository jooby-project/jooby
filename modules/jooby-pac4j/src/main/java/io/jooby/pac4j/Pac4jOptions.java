/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pac4j;

import java.util.List;
import java.util.Optional;

import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.util.serializer.JavaSerializer;
import org.pac4j.core.util.serializer.Serializer;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.SameSite;

/**
 * Options to configure pac4j security, callback and logout actions.
 *
 * <p>If you run behind a reverse proxy that has been configured to send the X-Forwarded-* header,
 * please consider to set {@link io.jooby.RouterOptions#setTrustProxy(boolean)} option.
 *
 * @author edgar
 * @since 2.4.0
 */
public class Pac4jOptions extends Config {

  private String defaultUrl = "/";

  private Boolean saveInSession;

  private Boolean multiProfile;

  private Boolean renewSession;

  private String defaultClient;

  private String callbackPath = "/callback";

  private String logoutPath = "/logout";

  private boolean localLogout = true;

  private String logoutUrlPattern;

  private boolean destroySession = true;

  private boolean centralLogout;

  private SameSite cookieSameSite;

  private boolean forceCallbackRoutes = false;

  private boolean forceLogoutRoutes = false;

  private Serializer serializer = new JavaSerializer();

  private Pac4jOptions(Config config) {
    setClients(config.getClients());
    Optional.ofNullable(config.getAuthorizers()).ifPresent(this::setAuthorizers);
    Optional.ofNullable(config.getMatchers()).ifPresent(this::setMatchers);
    setSecurityLogic(config.getSecurityLogic());
    setCallbackLogic(config.getCallbackLogic());
    setLogoutLogic(config.getLogoutLogic());
    setWebContextFactory(config.getWebContextFactory());
    setSessionStoreFactory(config.getSessionStoreFactory());
    setProfileManagerFactory(config.getProfileManagerFactory());
    setHttpActionAdapter(config.getHttpActionAdapter());
    setSessionLogoutHandler(config.getSessionLogoutHandler());
  }

  /** Default constructor. */
  public Pac4jOptions() {}

  /**
   * Creates a new options.
   *
   * @param clients Clients to use.
   */
  public Pac4jOptions(Clients clients) {
    super(clients);
  }

  /**
   * Creates a new options.
   *
   * @param client Client to use.
   */
  public Pac4jOptions(Client client) {
    super(client);
  }

  /**
   * Creates a new options.
   *
   * @param clients Clients to use.
   */
  public Pac4jOptions(List<Client> clients) {
    super(clients);
  }

  /**
   * Creates a new options.
   *
   * @param callbackPath Callback path.
   * @param client Client to use.
   */
  public Pac4jOptions(String callbackPath, Client client) {
    super(callbackPath, client);
  }

  /**
   * Creates a new options.
   *
   * @param callbackPath Callback path.
   * @param clients Clients to use.
   */
  public Pac4jOptions(String callbackPath, List<Client> clients) {
    super(callbackPath, clients);
  }

  /**
   * Get a Pac4j options instance of {@link Config}.
   *
   * @param config Config object.
   * @return Pac4j options.
   */
  public static Pac4jOptions from(@NonNull Config config) {
    return config instanceof Pac4jOptions options ? options : new Pac4jOptions(config);
  }

  /**
   * Default url to redirect to after successful login. Used by {@link
   * org.pac4j.core.engine.CallbackLogic}.
   *
   * @return Default url.
   */
  public @Nullable String getDefaultUrl() {
    return defaultUrl;
  }

  /**
   * Set default url to redirect to after successful login.
   *
   * @param defaultUrl Default url to redirect to after successful login.
   * @return This options.
   */
  public Pac4jOptions setDefaultUrl(@Nullable String defaultUrl) {
    this.defaultUrl = defaultUrl;
    return this;
  }

  /**
   * True to save profile/user data into session. Default is true for indirect clients. Used by
   * {@link org.pac4j.core.engine.CallbackLogic}.
   *
   * @return True to save profile/user data into session. Default is true for indirect clients.
   */
  public @Nullable Boolean getSaveInSession() {
    return saveInSession;
  }

  /**
   * Set whenever profile/data must be save in HTTP session.
   *
   * @param saveInSession True to save profile in HTTP session.
   * @return This session.
   */
  public @NonNull Pac4jOptions setSaveInSession(@Nullable Boolean saveInSession) {
    this.saveInSession = saveInSession;
    return this;
  }

  /**
   * Whether multi profiles are supported. Used by {@link org.pac4j.core.engine.CallbackLogic}.
   *
   * @return Whether multi profiles are supported.
   */
  public @Nullable Boolean getMultiProfile() {
    return multiProfile;
  }

  /**
   * Set Whether multi profiles are supported.
   *
   * @param multiProfile Whether multi profiles are supported.
   * @return This options.
   */
  public Pac4jOptions setMultiProfile(@Nullable Boolean multiProfile) {
    this.multiProfile = multiProfile;
    return this;
  }

  /**
   * Whether the session must be renewed. Used by {@link org.pac4j.core.engine.CallbackLogic}.
   *
   * @return Whether the session must be renewed.
   */
  public @Nullable Boolean getRenewSession() {
    return renewSession;
  }

  /**
   * Set whether the session must be renewed.
   *
   * @param renewSession whether the session must be renewed.
   * @return This options.
   */
  public Pac4jOptions setRenewSession(@Nullable Boolean renewSession) {
    this.renewSession = renewSession;
    return this;
  }

  /**
   * Default client to use. Used by {@link org.pac4j.core.engine.CallbackLogic}.
   *
   * @return Default client to use.
   */
  public @Nullable String getDefaultClient() {
    return defaultClient;
  }

  /**
   * Set default client to use.
   *
   * @param defaultClient Default client to use.
   * @return This options.
   */
  public Pac4jOptions setDefaultClient(@Nullable String defaultClient) {
    this.defaultClient = defaultClient;
    return this;
  }

  /**
   * Callback path, defaults to <code>/callback</code>. Used by {@link
   * org.pac4j.core.engine.CallbackLogic}.
   *
   * @return Callback path, defaults to <code>/callback</code>.
   */
  public String getCallbackPath() {
    return callbackPath;
  }

  /**
   * Set callback path.
   *
   * @param callbackPath Callback path.
   * @return This options.
   */
  public Pac4jOptions setCallbackPath(@NonNull String callbackPath) {
    this.callbackPath = callbackPath;
    return this;
  }

  /**
   * Callback path, defaults to <code>/logout</code>. Used by {@link
   * org.pac4j.core.engine.LogoutLogic}.
   *
   * @return Logout path, defaults to <code>/logout</code>.
   */
  public String getLogoutPath() {
    return logoutPath;
  }

  /**
   * Set logout path.
   *
   * @param logoutPath Logout path.
   * @return This options.
   */
  public Pac4jOptions setLogoutPath(@NonNull String logoutPath) {
    this.logoutPath = logoutPath;
    return this;
  }

  /**
   * Local logout option. Defaults: true. Used by {@link org.pac4j.core.engine.LogoutLogic}.
   *
   * @return Local logout option.
   */
  public boolean isLocalLogout() {
    return localLogout;
  }

  /**
   * Set logout option.
   *
   * @param localLogout Logout option.
   * @return This options.
   */
  public Pac4jOptions setLocalLogout(boolean localLogout) {
    this.localLogout = localLogout;
    return this;
  }

  /**
   * Central logout option. Defaults: false. Used by {@link org.pac4j.core.engine.LogoutLogic}.
   *
   * @return Central logout option.
   */
  public boolean isCentralLogout() {
    return centralLogout;
  }

  /**
   * Set central logout option.
   *
   * @param centralLogout Central logout option.
   * @return This options.
   */
  public Pac4jOptions setCentralLogout(boolean centralLogout) {
    this.centralLogout = centralLogout;
    return this;
  }

  /**
   * Whether to destroy session after logout. Defaults: true. Used by {@link
   * org.pac4j.core.engine.LogoutLogic}.
   *
   * @return Whether to destroy session after logout.
   */
  public boolean isDestroySession() {
    return destroySession;
  }

  /**
   * Set destroy session logout option.
   *
   * @param destroySession Destroy session option.
   * @return This options.
   */
  public Pac4jOptions setDestroySession(boolean destroySession) {
    this.destroySession = destroySession;
    return this;
  }

  /**
   * Returns the 'SameSite' parameter value used for cookies generated by the pac4j security engine.
   *
   * @return An instance of {@link SameSite} or {@code null} if the 'SameSite' parameter should be
   *     omitted.
   */
  @Nullable public SameSite getCookieSameSite() {
    return cookieSameSite;
  }

  /**
   * Sets the 'SameSite' parameter value used for cookies generated by the pac4j security engine,
   * pass {@code null} to omit the parameter.
   *
   * @param sameSite Value for the 'SameSite' parameter or {@code null} to omit it.
   * @return This options.
   */
  public Pac4jOptions setCookieSameSite(@Nullable SameSite sameSite) {
    cookieSameSite = sameSite;
    return this;
  }

  /**
   * The <code>/callback</code> routes are off when pac4j is configured with {@link
   * org.pac4j.core.client.DirectClient} clients only. These routes are not required for direct
   * clients. Setting this to <code>true</code> will still add the <code>/callback</code> routes.
   *
   * @return When the callback route is available.
   */
  public boolean isForceCallbackRoutes() {
    return forceCallbackRoutes;
  }

  /**
   * The <code>/logout</code> routes are off when pac4j is configured with {@link
   * org.pac4j.core.client.DirectClient} clients only. These routes are not required for direct
   * clients. Setting this to <code>true</code> will still add the <code>/logout</code> routes.
   *
   * @return When the logout route is available.
   */
  public boolean isForceLogoutRoutes() {
    return forceLogoutRoutes;
  }

  public Pac4jOptions setForceCallbackRoutes(boolean forceCallbackRoutes) {
    this.forceCallbackRoutes = forceCallbackRoutes;
    return this;
  }

  public Pac4jOptions setForceLogoutRoutes(boolean forceLogoutRoutes) {
    this.forceLogoutRoutes = forceLogoutRoutes;
    return this;
  }

  /**
   * It’s the logout URL pattern that the url parameter must match. It is an optional parameter and
   * only relative URLs are allowed by default.
   *
   * @return Logout URL Pattern.
   */
  public @Nullable String getLogoutUrlPattern() {
    return logoutUrlPattern;
  }

  /**
   * It’s the logout URL pattern that the url parameter must match. It is an optional parameter and
   * only relative URLs are allowed by default.
   *
   * @param logoutUrlPattern It’s the logout URL pattern that the url parameter must match. It is an
   *     optional parameter and only relative URLs are allowed by default.
   * @return This instance.
   */
  public @NonNull Pac4jOptions setLogoutUrlPattern(String logoutUrlPattern) {
    this.logoutUrlPattern = logoutUrlPattern;
    return this;
  }

  /**
   * Used for save complex object into session while using indirect clients.
   *
   * @return Serializer, defaults to {@link JavaSerializer}.
   */
  public @NonNull Serializer getSerializer() {
    return serializer;
  }

  /**
   * Set serializer for saving complex object into session while using indirect clients.
   *
   * @param serializer Serializer.
   * @return This instance.
   */
  public @NonNull Pac4jOptions setSerializer(@NonNull Serializer serializer) {
    this.serializer = serializer;
    return this;
  }
}
