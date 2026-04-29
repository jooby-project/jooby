/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.UnauthorizedAction;

import io.jooby.Context;
import io.jooby.StatusCode;
import io.jooby.pac4j.Pac4jContext;

@ExtendWith(MockitoExtension.class)
class ActionAdapterImplTest {

  @Mock private Pac4jContext pac4jContext;
  @Mock private Context joobyContext;

  private ActionAdapterImpl adapter;

  @BeforeEach
  void setUp() {
    adapter = new ActionAdapterImpl();
  }

  @Test
  void shouldThrowTechnicalExceptionWhenActionIsNull() {
    TechnicalException thrown =
        assertThrows(TechnicalException.class, () -> adapter.adapt(null, pac4jContext));

    assertEquals("No action provided", thrown.getMessage());
  }

  @Test
  void shouldDelegateToSendRedirectWhenActionIsWithLocation() {
    when(pac4jContext.getContext()).thenReturn(joobyContext);

    // FoundAction implements WithLocationAction (HTTP 302)
    FoundAction action = new FoundAction("/login");

    Context expectedReturnContext = mock(Context.class);
    when(joobyContext.sendRedirect(StatusCode.FOUND, "/login")).thenReturn(expectedReturnContext);

    Object result = adapter.adapt(action, pac4jContext);

    assertEquals(expectedReturnContext, result);
    verify(joobyContext).sendRedirect(StatusCode.FOUND, "/login");
  }

  @Test
  void shouldSetResponseCodeAndThrowWhenActionIsStandard() {
    when(pac4jContext.getContext()).thenReturn(joobyContext);

    // UnauthorizedAction is a standard HttpAction (HTTP 401)
    UnauthorizedAction action = new UnauthorizedAction();

    HttpAction thrown = assertThrows(HttpAction.class, () -> adapter.adapt(action, pac4jContext));

    assertEquals(action, thrown);
    verify(joobyContext).setResponseCode(StatusCode.UNAUTHORIZED);
  }
}
