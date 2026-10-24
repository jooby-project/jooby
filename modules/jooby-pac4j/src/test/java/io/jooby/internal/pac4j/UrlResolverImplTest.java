/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Context;
import io.jooby.pac4j.Pac4jContext;

@ExtendWith(MockitoExtension.class)
class UrlResolverImplTest {

  @Mock private Pac4jContext pac4jContext;
  @Mock private Context joobyContext;

  @Test
  void shouldPreserveAbsoluteExternalUrlWithQuery() {
    String url = "https://www.casserverpac4j.dev/login?locale=fr";
    String callbackUrl = "https://app.example.org/callback?client_name=CasClient";
    UrlResolverImpl resolver = new UrlResolverImpl();

    assertEquals(url, resolver.compute(url, pac4jContext));
    assertEquals(callbackUrl, resolver.compute(callbackUrl, pac4jContext));

    verifyNoInteractions(pac4jContext, joobyContext);
  }

  @Test
  void shouldResolveRelativeUrlUsingContext() {
    when(pac4jContext.getContext()).thenReturn(joobyContext);
    when(joobyContext.getRequestURL("/callback"))
        .thenReturn("https://example.org/callback?ignored=true");

    assertEquals(
        "https://example.org/callback",
        new UrlResolverImpl().compute("/callback", pac4jContext));
  }
}
