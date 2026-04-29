/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pac4j.core.context.Cookie;

import io.jooby.Context;
import io.jooby.Formdata;
import io.jooby.QueryString;
import io.jooby.SameSite;
import io.jooby.pac4j.Pac4jOptions;
import io.jooby.value.Value;

@ExtendWith(MockitoExtension.class)
class WebContextImplTest {

  @Mock private Context context;

  private WebContextImpl webContext;

  @BeforeEach
  void setUp() {
    webContext = new WebContextImpl(context);
  }

  @Test
  void testGetContext() {
    assertEquals(context, webContext.getContext());
  }

  @Test
  void testGetResponseHeader() {
    when(context.getResponseHeader("Test-Header")).thenReturn("HeaderValue");
    assertEquals(Optional.of("HeaderValue"), webContext.getResponseHeader("Test-Header"));

    when(context.getResponseHeader("Missing")).thenReturn(null);
    assertEquals(Optional.empty(), webContext.getResponseHeader("Missing"));
  }

  @Test
  void testGetRequestParameterFoundInForm() {
    Value missingValue = mock(Value.class);
    when(missingValue.isMissing()).thenReturn(true);

    Value foundValue = mock(Value.class);
    when(foundValue.isMissing()).thenReturn(false);
    when(foundValue.value()).thenReturn("paramValue");

    Value pathNode = mock(Value.class);
    var queryNode = mock(QueryString.class);
    var formNode = mock(Formdata.class);

    when(pathNode.get("myParam")).thenReturn(missingValue);
    when(queryNode.get("myParam")).thenReturn(missingValue);
    when(formNode.get("myParam")).thenReturn(foundValue);

    when(context.path()).thenReturn(pathNode);
    when(context.query()).thenReturn(queryNode);
    when(context.form()).thenReturn(formNode);

    assertEquals(Optional.of("paramValue"), webContext.getRequestParameter("myParam"));
  }

  @Test
  void testGetRequestParameterNotFound() {
    Value missingValue = mock(Value.class);
    when(missingValue.isMissing()).thenReturn(true);

    var pathNode = mock(Value.class);
    when(pathNode.get(anyString())).thenReturn(missingValue);

    var queryString = mock(QueryString.class);
    when(queryString.get(anyString())).thenReturn(missingValue);

    var formdata = mock(Formdata.class);
    when(formdata.get(anyString())).thenReturn(missingValue);

    when(context.path()).thenReturn(pathNode);
    when(context.query()).thenReturn(queryString);
    when(context.form()).thenReturn(formdata);

    assertEquals(Optional.empty(), webContext.getRequestParameter("missingParam"));
  }

  @Test
  void testGetRequestParametersWithMerging() {
    var pathNode = mock(Value.class);
    var queryNode = mock(QueryString.class);
    var formNode = mock(Formdata.class);

    when(pathNode.toMultimap()).thenReturn(Map.of("id", List.of("1")));
    when(queryNode.toMultimap()).thenReturn(Map.of("id", List.of("2"), "q", List.of("search")));
    when(formNode.toMultimap()).thenReturn(Map.of("id", List.of("3"), "f", List.of("data")));

    when(context.path()).thenReturn(pathNode);
    when(context.query()).thenReturn(queryNode);
    when(context.form()).thenReturn(formNode);

    Map<String, String[]> params = webContext.getRequestParameters();

    assertArrayEquals(new String[] {"1", "2", "3"}, params.get("id"));
    assertArrayEquals(new String[] {"search"}, params.get("q"));
    assertArrayEquals(new String[] {"data"}, params.get("f"));
  }

  @Test
  void testGetRequestAttribute() {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("attr1", "val1");
    when(context.getAttributes()).thenReturn(attributes);

    assertEquals(Optional.of("val1"), webContext.getRequestAttribute("attr1"));
    assertEquals(Optional.empty(), webContext.getRequestAttribute("missing"));
  }

  @Test
  void testSetRequestAttribute() {
    webContext.setRequestAttribute("attr1", "val1");
    verify(context).setAttribute("attr1", "val1");
  }

  @Test
  void testGetRequestHeader() {
    Value headerValue = mock(Value.class);
    when(headerValue.toOptional()).thenReturn(Optional.of("application/json"));
    when(context.header("Accept")).thenReturn(headerValue);

    assertEquals(Optional.of("application/json"), webContext.getRequestHeader("Accept"));
  }

  @Test
  void testBasicContextDelegations() {
    when(context.getMethod()).thenReturn("POST");
    assertEquals("POST", webContext.getRequestMethod());

    when(context.getRemoteAddress()).thenReturn("127.0.0.1");
    assertEquals("127.0.0.1", webContext.getRemoteAddr());

    when(context.getServerHost()).thenReturn("localhost");
    assertEquals("localhost", webContext.getServerName());

    when(context.getServerPort()).thenReturn(8080);
    assertEquals(8080, webContext.getServerPort());

    when(context.getScheme()).thenReturn("https");
    assertEquals("https", webContext.getScheme());

    when(context.isSecure()).thenReturn(true);
    assertTrue(webContext.isSecure());

    when(context.getRequestURL()).thenReturn("https://localhost:8080/api");
    assertEquals("https://localhost:8080/api", webContext.getFullRequestURL());

    when(context.getRequestPath()).thenReturn("/api");
    assertEquals("/api", webContext.getPath());
  }

  @Test
  void testSetResponseHeaderAndType() {
    webContext.setResponseHeader("X-Custom", "val");
    verify(context).setResponseHeader("X-Custom", "val");

    webContext.setResponseContentType("text/plain");
    verify(context).setResponseType("text/plain");
  }

  @Test
  void testGetRequestCookies() {
    when(context.cookieMap()).thenReturn(Map.of("session_id", "12345"));

    Collection<Cookie> cookies = webContext.getRequestCookies();
    assertEquals(1, cookies.size());
    Cookie pac4jCookie = cookies.iterator().next();
    assertEquals("session_id", pac4jCookie.getName());
    assertEquals("12345", pac4jCookie.getValue());
  }

  @Test
  void testAddResponseCookieWithSameSite() {
    Pac4jOptions options = new Pac4jOptions();
    options.setCookieSameSite(SameSite.NONE);
    when(context.require(Pac4jOptions.class)).thenReturn(options);

    Cookie cookie = new Cookie("my-cookie", "my-val");
    cookie.setDomain("example.com");
    cookie.setPath("/path");
    cookie.setHttpOnly(true);
    cookie.setMaxAge(3600);
    cookie.setSecure(false); // SameSite.NONE should enforce true

    webContext.addResponseCookie(cookie);

    ArgumentCaptor<io.jooby.Cookie> captor = ArgumentCaptor.forClass(io.jooby.Cookie.class);
    verify(context).setResponseCookie(captor.capture());

    io.jooby.Cookie captured = captor.getValue();
    assertEquals("my-cookie", captured.getName());
    assertEquals("my-val", captured.getValue());
    assertEquals("example.com", captured.getDomain());
    assertEquals("/path", captured.getPath());
    assertTrue(captured.isHttpOnly());
    assertEquals(3600, captured.getMaxAge());
    assertTrue(captured.isSecure()); // Enforced by SameSite.NONE
    assertEquals(SameSite.NONE, captured.getSameSite());
  }

  @Test
  void testAddResponseCookieWithoutSameSite() {
    Pac4jOptions options = new Pac4jOptions();
    when(context.require(Pac4jOptions.class)).thenReturn(options);

    Cookie cookie = new Cookie("simple", "val");

    webContext.addResponseCookie(cookie);

    ArgumentCaptor<io.jooby.Cookie> captor = ArgumentCaptor.forClass(io.jooby.Cookie.class);
    verify(context).setResponseCookie(captor.capture());

    io.jooby.Cookie captured = captor.getValue();
    assertEquals("simple", captured.getName());
    assertEquals("val", captured.getValue());
    assertNull(captured.getSameSite());
  }

  @Test
  void testGetSessionStore() {
    assertNotNull(webContext.getSessionStore());
    assertTrue(webContext.getSessionStore() instanceof SessionStoreImpl);
  }
}
