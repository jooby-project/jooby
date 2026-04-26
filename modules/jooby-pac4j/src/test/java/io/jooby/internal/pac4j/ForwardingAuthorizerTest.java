/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.UserProfile;

import io.jooby.Registry;

public class ForwardingAuthorizerTest {

  @Test
  public void testIsAuthorized() {
    // 1. Prepare mocks
    Registry registry = mock(Registry.class);
    Authorizer delegate = mock(Authorizer.class);
    WebContext webContext = mock(WebContext.class);
    SessionStore sessionStore = mock(SessionStore.class);
    List<UserProfile> profiles = Collections.emptyList();

    // 2. Setup behavior: Registry returns our mock authorizer
    when(registry.require(Authorizer.class)).thenReturn(delegate);
    when(delegate.isAuthorized(webContext, sessionStore, profiles)).thenReturn(true);

    // 3. Initialize ForwardingAuthorizer
    ForwardingAuthorizer forwardingAuthorizer = new ForwardingAuthorizer(Authorizer.class);
    forwardingAuthorizer.setRegistry(registry);

    // 4. Execute
    boolean result = forwardingAuthorizer.isAuthorized(webContext, sessionStore, profiles);

    // 5. Verify results
    assertTrue(result);
    verify(registry).require(Authorizer.class);
    verify(delegate).isAuthorized(webContext, sessionStore, profiles);
  }
}
