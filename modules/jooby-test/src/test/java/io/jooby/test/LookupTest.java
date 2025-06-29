/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static io.jooby.ParamSource.COOKIE;
import static io.jooby.ParamSource.FLASH;
import static io.jooby.ParamSource.FORM;
import static io.jooby.ParamSource.HEADER;
import static io.jooby.ParamSource.PATH;
import static io.jooby.ParamSource.QUERY;
import static io.jooby.ParamSource.SESSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.*;
import io.jooby.exception.MissingValueException;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class LookupTest {

  private MockContext context;

  @BeforeEach
  public void makeContext() {
    context = mockContext();

    Map<String, String> pathMap = new HashMap<>();
    pathMap.put("foo", "path-bar");
    context.setPathMap(pathMap);

    context.setRequestHeader("foo", "header-bar");

    Map<String, String> cookieMap = new HashMap<>();
    cookieMap.put("foo", "cookie-bar");
    context.setCookieMap(cookieMap);

    context.setFlashAttribute("foo", "flash-bar");

    MockSession session = new MockSession();
    session.put("foo", "session-bar");
    context.setSession(session);

    context.setQueryString("?foo=query-bar");

    Formdata formdata = Value.formdata(context.getValueFactory());
    formdata.put("foo", "form-bar");
    context.setForm(formdata);
  }

  @Test
  public void testNoSources() {
    Throwable t = Assertions.assertThrows(IllegalArgumentException.class, this::test);
    assertEquals(t.getMessage(), "No parameter sources were specified.");
  }

  @Test
  public void testPriority() {
    test(PATH, HEADER, COOKIE, FLASH, SESSION, QUERY, FORM);
    test(HEADER, COOKIE, FLASH, SESSION, QUERY, FORM, PATH);
    test(COOKIE, FLASH, SESSION, QUERY, FORM, PATH, HEADER);
    test(FLASH, SESSION, QUERY, FORM, PATH, HEADER, COOKIE);
    test(SESSION, QUERY, FORM, PATH, HEADER, COOKIE, FLASH);
    test(QUERY, FORM, PATH, HEADER, COOKIE, FLASH, SESSION);
    test(FORM, PATH, HEADER, COOKIE, FLASH, SESSION, QUERY);
    test(FORM, PATH, HEADER, COOKIE, FLASH, SESSION, QUERY);

    test(PATH, l -> l.inPath().inHeader().inCookie().inFlash().inSession().inQuery().inForm());
    test(HEADER, l -> l.inHeader().inCookie().inFlash().inSession().inQuery().inForm().inPath());
    test(COOKIE, l -> l.inCookie().inFlash().inSession().inQuery().inForm().inPath().inHeader());
    test(FLASH, l -> l.inFlash().inSession().inQuery().inForm().inPath().inHeader().inCookie());
    test(SESSION, l -> l.inSession().inQuery().inForm().inPath().inHeader().inCookie().inFlash());
    test(QUERY, l -> l.inQuery().inForm().inPath().inHeader().inCookie().inFlash().inSession());
    test(FORM, l -> l.inForm().inPath().inHeader().inCookie().inFlash().inSession().inQuery());
  }

  @Test
  public void testMissingValue() {
    assertThrows(
        MissingValueException.class,
        () ->
            mockContext().lookup("foo", PATH, HEADER, COOKIE, FLASH, SESSION, QUERY, FORM).value());

    assertThrows(
        MissingValueException.class,
        () ->
            mockContext()
                .lookup()
                .inPath()
                .inHeader()
                .inCookie()
                .inFlash()
                .inSession()
                .inQuery()
                .inForm()
                .get("foo")
                .value());
  }

  private static MockContext mockContext() {
    var router = mock(Router.class);
    when(router.getValueFactory()).thenReturn(new ValueFactory());
    var ctx = new MockContext();
    ctx.setRouter(router);
    return ctx;
  }

  private void test(ParamSource... sources) {
    String value = context.lookup("foo", sources).value();
    assertEquals(sources[0].name().toLowerCase() + "-bar", value);
  }

  private void test(ParamSource firstSource, Function<ParamLookup, ParamLookup.Stage> lookup) {
    String value = lookup.apply(context.lookup()).get("foo").value();
    assertEquals(firstSource.name().toLowerCase() + "-bar", value);
  }
}
