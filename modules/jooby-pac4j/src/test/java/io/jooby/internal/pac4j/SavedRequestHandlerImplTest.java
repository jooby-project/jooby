/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.session.SessionStore;

import io.jooby.Context;
import io.jooby.pac4j.Pac4jContext;

public class SavedRequestHandlerImplTest {

  private Context joobyContext;
  private Pac4jContext pac4jContext;
  private SessionStore sessionStore;
  private CallContext callContext;

  @BeforeEach
  void setUp() {
    joobyContext = mock(Context.class);
    pac4jContext = mock(Pac4jContext.class);
    sessionStore = mock(SessionStore.class);

    // Wire up the Pac4jContext to return the Jooby Context
    when(pac4jContext.getContext()).thenReturn(joobyContext);

    // Create the CallContext used by pac4j logic
    callContext = new CallContext(pac4jContext, sessionStore);
  }

  @Test
  void testSavePathIncluded() {
    Set<String> excludes = Set.of("/favicon.ico");
    SavedRequestHandlerImpl handler = new SavedRequestHandlerImpl(excludes);

    // Path NOT in exclude list
    when(joobyContext.getRequestPath()).thenReturn("/login");
    // Mock session and context behavior for the internal super.save() call
    when(pac4jContext.getFullRequestURL()).thenReturn("http://localhost/login");
    when(pac4jContext.getRequestMethod()).thenReturn("GET");

    handler.save(callContext);

    // Verify that sessionStore was interacted with (indicating super.save() was called)
    verify(sessionStore).set(eq(pac4jContext), anyString(), any());
  }

  @Test
  void testSavePathExcluded() {
    Set<String> excludes = Set.of("/favicon.ico", "/robot.txt");
    SavedRequestHandlerImpl handler = new SavedRequestHandlerImpl(excludes);

    // Path IS in exclude list
    when(joobyContext.getRequestPath()).thenReturn("/favicon.ico");

    handler.save(callContext);

    // Verify that sessionStore was never touched (super.save() skipped)
    verifyNoInteractions(sessionStore);
  }
}
