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
   *
   * @return True to save profile/user data into session. Default is true for indirect clients.
   */
  public @Nullable Boolean getSaveInSession() {
    return saveInSession;
  }

  /**
   * Set whenever profile/data must be save in HTTP session.
   *
   * @param saveInSession
   * @return
   */
  public Pac4jOptions setSaveInSession(Boolean saveInSession) {
    this.saveInSession = saveInSession;
    return this;
  }

  public Boolean getMultiProfile() {
    return multiProfile;
  }

  public Pac4jOptions setMultiProfile(Boolean multiProfile) {
    this.multiProfile = multiProfile;
    return this;
  }

  public Boolean getRenewSession() {
    return renewSession;
  }

  public Pac4jOptions setRenewSession(Boolean renewSession) {
    this.renewSession = renewSession;
    return this;
  }

  public String getDefaultClient() {
    return defaultClient;
  }

  public Pac4jOptions setDefaultClient(String defaultClient) {
    this.defaultClient = defaultClient;
    return this;
  }

  public boolean isMultiProfile() {
    return multiProfile;
  }

  public Pac4jOptions setMultiProfile(boolean multiProfile) {
    this.multiProfile = multiProfile;
    return this;
  }

  public String getCallbackPath() {
    return callbackPath;
  }

  public Pac4jOptions setCallbackPath(String callbackPath) {
    this.callbackPath = callbackPath;
    return this;
  }

  public String getLogoutPath() {
    return logoutPath;
  }

  public Pac4jOptions setLogoutPath(String logoutPath) {
    this.logoutPath = logoutPath;
    return this;
  }

  public boolean isLocalLogout() {
    return localLogout;
  }

  public Pac4jOptions setLocalLogout(boolean localLogout) {
    this.localLogout = localLogout;
    return this;
  }

  public boolean isCentralLogout() {
    return centralLogout;
  }

  public Pac4jOptions setCentralLogout(boolean centralLogout) {
    this.centralLogout = centralLogout;
    return this;
  }

  public boolean isDestroySession() {
    return destroySession;
  }

  public Pac4jOptions setDestroySession(boolean destroySession) {
    this.destroySession = destroySession;
    return this;
  }

  public boolean isTrustProxy() {
    return trustProxy;
  }

  public Pac4jOptions setTrustProxy(boolean trustProxy) {
    this.trustProxy = trustProxy;
    return this;
  }
}
