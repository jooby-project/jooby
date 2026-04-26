/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.http.url.UrlResolver;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class DevLoginFormTest {

  private Config pac4j;
  private UrlResolver urlResolver;
  private Context ctx;
  private DevLoginForm loginForm;
  private String callbackPath = "/callback";

  @BeforeEach
  void setUp() {
    pac4j = mock(Config.class);
    Clients clients = mock(Clients.class);
    urlResolver = mock(UrlResolver.class);
    ctx = mock(Context.class);

    when(pac4j.getClients()).thenReturn(clients);
    when(clients.getUrlResolver()).thenReturn(urlResolver);

    loginForm = new DevLoginForm(pac4j, callbackPath);
  }

  @Test
  void testApplyWithQueryData() throws Exception {
    // 1. Mock query parameters
    Value errorValue = Value.value(new ValueFactory(), "error", "Invalid Credentials");
    Value userValue = Value.value(new ValueFactory(), "username", "joobyUser");

    when(ctx.query("error")).thenReturn(errorValue);
    when(ctx.query("username")).thenReturn(userValue);

    // 2. Mock URL resolution logic
    when(urlResolver.compute(eq(callbackPath), any())).thenReturn("http://localhost/callback");

    // 3. Mock fluent context behavior
    when(ctx.setResponseType(MediaType.html)).thenReturn(ctx);

    // 4. Execute
    loginForm.apply(ctx);

    // 5. Verifications
    // Check attributes
    verify(ctx).setAttribute("username", "joobyUser");
    verify(ctx).setAttribute("error", "Invalid Credentials");

    // Check response type and content
    verify(ctx).setResponseType(MediaType.html);
    verify(ctx)
        .send(
            argThat(
                (String html) ->
                    html.contains("Invalid Credentials")
                        && html.contains("http://localhost/callback?client_name=FormClient")
                        && html.contains("value=\"joobyUser\"")));
  }

  @Test
  void testApplyWithMissingQueryData() throws Exception {
    // 1. Mock empty/missing query parameters
    when(ctx.query("error")).thenReturn(Value.missing(new ValueFactory(), "error"));
    when(ctx.query("username")).thenReturn(Value.missing(new ValueFactory(), "username"));

    when(urlResolver.compute(eq(callbackPath), any())).thenReturn("/callback");
    when(ctx.setResponseType(MediaType.html)).thenReturn(ctx);

    // 2. Execute
    loginForm.apply(ctx);

    // 3. Verifications
    verify(ctx).setAttribute("username", "");
    verify(ctx).setAttribute("error", "");

    // Verify HTML doesn't contain nulls or weird values for error/username
    verify(ctx)
        .send(
            argThat(
                (String html) ->
                    html.contains("<h3>Login</h3>")
                        && html.contains("action=\"/callback?client_name=FormClient\"")
                        && html.contains("value=\"\"")));
  }
}
