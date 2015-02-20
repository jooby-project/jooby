package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.jooby.Cookie.Definition;
import org.junit.Test;

public class CookieDefinitionTest {

  @Test
  public void newCookieDefDefaults() {
    Definition def = new Cookie.Definition();
    assertEquals(Optional.empty(), def.name());
    assertEquals(Optional.empty(), def.comment());
    assertEquals(Optional.empty(), def.domain());
    assertEquals(Optional.empty(), def.httpOnly());
    assertEquals(Optional.empty(), def.maxAge());
    assertEquals(Optional.empty(), def.path());
    assertEquals(Optional.empty(), def.secure());
    assertEquals(Optional.empty(), def.value());
  }

  @Test
  public void newNamedCookieDef() {
    Definition def = new Cookie.Definition("name");
    assertEquals("name", def.name().get());
    assertEquals("name", def.toCookie().name());
    assertEquals(Optional.empty(), def.comment());
    assertEquals(Optional.empty(), def.domain());
    assertEquals(Optional.empty(), def.httpOnly());
    assertEquals(Optional.empty(), def.maxAge());
    assertEquals(Optional.empty(), def.path());
    assertEquals(Optional.empty(), def.secure());
    assertEquals(Optional.empty(), def.value());
  }

  @Test(expected = NullPointerException.class)
  public void newNullNameCookieDef() {
    new Cookie.Definition((String) null);
  }

  @Test
  public void newValuedCookieDef() {
    Definition def = new Cookie.Definition("name", "v");
    assertEquals("name", def.name().get());
    assertEquals("v", def.value().get());
    assertEquals("v", def.toCookie().value().get());
    assertEquals(Optional.empty(), def.comment());
    assertEquals(Optional.empty(), def.domain());
    assertEquals(Optional.empty(), def.httpOnly());
    assertEquals(Optional.empty(), def.maxAge());
    assertEquals(Optional.empty(), def.path());
    assertEquals(Optional.empty(), def.secure());
  }

  @Test(expected = NullPointerException.class)
  public void newNullValueCookieDef() {
    new Cookie.Definition("name", null);
  }

  @Test
  public void cookieWithComment() {
    Definition def = new Cookie.Definition("name");
    assertEquals(Optional.empty(), def.comment());
    assertEquals("a comment", def.comment("a comment").comment().get());
    assertEquals("a comment", def.toCookie().comment().get());
  }

  @Test
  public void cookieWithDomain() {
    Definition def = new Cookie.Definition("name");
    assertEquals(Optional.empty(), def.domain());
    assertEquals("jooby.org", def.domain("jooby.org").domain().get());
    assertEquals("jooby.org", def.toCookie().domain().get());
  }

  @Test
  public void cookieHttpOnly() {
    Definition def = new Cookie.Definition("name");
    assertEquals(Optional.empty(), def.httpOnly());
    assertEquals(true, def.httpOnly(true).httpOnly().get());
    assertEquals(true, def.toCookie().httpOnly());
  }

  @Test
  public void cookieMaxAge() {
    Definition def = new Cookie.Definition("name");
    assertEquals(Optional.empty(), def.maxAge());
    assertEquals(123L, (long) def.maxAge(123).maxAge().get());
    assertEquals(123, def.toCookie().maxAge());
  }

  @Test
  public void cookieVersion() {
    Definition def = new Cookie.Definition("name");
    assertEquals(1, def.toCookie().version());
  }

  @Test
  public void cookiePath() {
    Definition def = new Cookie.Definition("name");
    assertEquals(Optional.empty(), def.path());
    assertEquals("/", def.path("/").path().get());
    assertEquals("/", def.toCookie().path());
  }

  @Test
  public void cookieSecure() {
    Definition def = new Cookie.Definition("name");
    assertEquals(Optional.empty(), def.secure());
    assertEquals(true, def.secure(true).secure().get());
    assertEquals(true, def.toCookie().secure());
  }
}
