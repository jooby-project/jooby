/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pac4j;

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

  public String getDefaultUrl() {
    return defaultUrl;
  }

  public Pac4jOptions setDefaultUrl(String defaultUrl) {
    this.defaultUrl = defaultUrl;
    return this;
  }

  public Boolean getSaveInSession() {
    return saveInSession;
  }

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
