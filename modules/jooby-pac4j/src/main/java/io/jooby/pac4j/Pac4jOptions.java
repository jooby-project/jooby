/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pac4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Options to configure pac4j security, callback and logout actions.
 *
 * @author edgar
 * @since 2.4.0
 */
public class Pac4jOptions {

  private String defaultUrl;

  private Boolean saveInSession;

  private Boolean multiProfile;

  private Boolean renewSession;

  private String defaultClient;

  private String callbackPath = "/callback";

  private String logoutPath = "/logout";

  private boolean localLogout = true;

  private boolean destroySession = true;

  private boolean trustProxy;

  private boolean centralLogout;

  /**
   * Default url to redirect to after successful login.
   * Used by {@link org.pac4j.core.engine.CallbackLogic}.
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
  public @Nonnull Pac4jOptions setDefaultUrl(@Nullable String defaultUrl) {
    this.defaultUrl = defaultUrl;
    return this;
  }

  /**
   * True to save profile/user data into session. Default is true for indirect clients.
   * Used by {@link org.pac4j.core.engine.CallbackLogic}.
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
  public @Nonnull Pac4jOptions setSaveInSession(@Nullable Boolean saveInSession) {
    this.saveInSession = saveInSession;
    return this;
  }

  /**
   * Whether multi profiles are supported.
   * Used by {@link org.pac4j.core.engine.CallbackLogic}.
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
  public @Nonnull Pac4jOptions setMultiProfile(@Nullable Boolean multiProfile) {
    this.multiProfile = multiProfile;
    return this;
  }

  /**
   * Whether the session must be renewed.
   * Used by {@link org.pac4j.core.engine.CallbackLogic}.
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
  public @Nonnull Pac4jOptions setRenewSession(@Nullable Boolean renewSession) {
    this.renewSession = renewSession;
    return this;
  }

  /**
   * Default client to use.
   * Used by {@link org.pac4j.core.engine.CallbackLogic}.
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
  public @Nonnull Pac4jOptions setDefaultClient(@Nullable String defaultClient) {
    this.defaultClient = defaultClient;
    return this;
  }

  /**
   * Callback path, defaults to <code>/callback</code>.
   * Used by {@link org.pac4j.core.engine.CallbackLogic}.
   *
   * @return Callback path, defaults to <code>/callback</code>.
   */
  public @Nonnull String getCallbackPath() {
    return callbackPath;
  }

  /**
   * Set callback path.
   *
   * @param callbackPath Callback path.
   * @return This options.
   */
  public @Nonnull Pac4jOptions setCallbackPath(@Nonnull String callbackPath) {
    this.callbackPath = callbackPath;
    return this;
  }

  /**
   * Callback path, defaults to <code>/logout</code>.
   * Used by {@link org.pac4j.core.engine.LogoutLogic}.
   *
   * @return Logout path, defaults to <code>/logout</code>.
   */
  public @Nonnull String getLogoutPath() {
    return logoutPath;
  }

  /**
   * Set logout path.
   *
   * @param logoutPath Logout path.
   * @return This options.
   */
  public @Nonnull Pac4jOptions setLogoutPath(@Nonnull String logoutPath) {
    this.logoutPath = logoutPath;
    return this;
  }

  /**
   * Local logout option. Defaults: true.
   * Used by {@link org.pac4j.core.engine.LogoutLogic}.
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
  public @Nonnull Pac4jOptions setLocalLogout(boolean localLogout) {
    this.localLogout = localLogout;
    return this;
  }

  /**
   * Central logout option. Defaults: false.
   * Used by {@link org.pac4j.core.engine.LogoutLogic}.
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
  public @Nonnull Pac4jOptions setCentralLogout(boolean centralLogout) {
    this.centralLogout = centralLogout;
    return this;
  }

  /**
   * Whether to destroy session after logout. Defaults: true.
   * Used by {@link org.pac4j.core.engine.LogoutLogic}.
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
  public @Nonnull Pac4jOptions setDestroySession(boolean destroySession) {
    this.destroySession = destroySession;
    return this;
  }

  /**
   * True for using the <code>X-Forwarded-*</code> headers to generates URL. Defauls to: false.
   * See {@link io.jooby.Context#getRequestURL(boolean)}.
   *
   * @return Whether trust on <code>X-Forwarded-*</code> headers.
   */
  public boolean isTrustProxy() {
    return trustProxy;
  }

  /**
   * Set whether trust on <code>X-Forwarded-*</code> headers.
   *
   * @param trustProxy True or using <code>X-Forwarded-*</code> headers.
   * @return This options.
   */
  public @Nonnull Pac4jOptions setTrustProxy(boolean trustProxy) {
    this.trustProxy = trustProxy;
    return this;
  }
}
