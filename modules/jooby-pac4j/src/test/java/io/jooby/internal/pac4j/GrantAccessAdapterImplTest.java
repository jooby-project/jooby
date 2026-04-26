/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.Pac4jConstants;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.pac4j.Pac4jOptions;

public class GrantAccessAdapterImplTest {

  private Context ctx;
  private Pac4jOptions options;
  private WebContext webContext;
  private SessionStore sessionStore;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    options = new Pac4jOptions();
    webContext = mock(WebContext.class);
    sessionStore = mock(SessionStore.class);
  }

  @Test
  void testAdaptWithUserProfiles() throws Exception {
    Route.Handler next = mock(Route.Handler.class);
    when(next.apply(ctx)).thenReturn("done");

    GrantAccessAdapterImpl adapter = new GrantAccessAdapterImpl(ctx, options, next);

    CommonProfile profile = new CommonProfile();
    profile.setId("user123");

    Object result = adapter.adapt(webContext, sessionStore, List.of(profile));

    assertEquals("done", result);
    verify(ctx).setUser(profile);
    verify(next).apply(ctx);
  }

  @Test
  void testAdaptWithoutUserProfiles() throws Exception {
    Route.Handler next = mock(Route.Handler.class);
    when(next.apply(ctx)).thenReturn("ok");

    GrantAccessAdapterImpl adapter = new GrantAccessAdapterImpl(ctx, options, next);

    // Empty profiles collection
    Object result = adapter.adapt(webContext, sessionStore, Collections.emptyList());

    assertEquals("ok", result);
    verify(ctx, never()).setUser(any());
  }

  @Test
  void testDefaultConstructorRedirectWithRequestedUrl() throws Exception {
    options.setDefaultUrl("/fallback");
    GrantAccessAdapterImpl adapter = new GrantAccessAdapterImpl(ctx, options);

    // Mock a saved redirection action in the session
    FoundAction action = new FoundAction("/saved-path");
    when(sessionStore.get(webContext, Pac4jConstants.REQUESTED_URL))
        .thenReturn(Optional.of(action));
    when(ctx.sendRedirect("/saved-path")).thenReturn(ctx);

    adapter.adapt(webContext, sessionStore, Collections.emptyList());

    verify(ctx).sendRedirect("/saved-path");
  }

  @Test
  void testDefaultConstructorRedirectFallback() throws Exception {
    options.setDefaultUrl("/fallback");
    GrantAccessAdapterImpl adapter = new GrantAccessAdapterImpl(ctx, options);

    // No requested URL in session
    when(sessionStore.get(webContext, Pac4jConstants.REQUESTED_URL)).thenReturn(Optional.empty());
    when(ctx.sendRedirect("/fallback")).thenReturn(ctx);

    adapter.adapt(webContext, sessionStore, Collections.emptyList());

    verify(ctx).sendRedirect("/fallback");
  }

  @Test
  void testDefaultConstructorRedirectInvalidTypeInSession() throws Exception {
    options.setDefaultUrl("/fallback");
    GrantAccessAdapterImpl adapter = new GrantAccessAdapterImpl(ctx, options);

    // Session has a string or something else not a WithLocationAction
    when(sessionStore.get(webContext, Pac4jConstants.REQUESTED_URL))
        .thenReturn(Optional.of("not-an-action"));
    when(ctx.sendRedirect("/fallback")).thenReturn(ctx);

    adapter.adapt(webContext, sessionStore, Collections.emptyList());

    verify(ctx).sendRedirect("/fallback");
  }
}
