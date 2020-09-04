package io.jooby;

import io.jooby.exception.MissingValueException;
import io.jooby.internal.FormdataNode;
import io.jooby.internal.MultipartNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static io.jooby.ParamSource.COOKIE;
import static io.jooby.ParamSource.FLASH;
import static io.jooby.ParamSource.FORM;
import static io.jooby.ParamSource.HEADER;
import static io.jooby.ParamSource.MULTIPART;
import static io.jooby.ParamSource.PATH;
import static io.jooby.ParamSource.QUERY;
import static io.jooby.ParamSource.SESSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LookupTest {

  private MockContext context;

  @BeforeEach
  public void makeContext() {
    context = new MockContext();

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

    FormdataNode formdataNode = new FormdataNode(context);
    formdataNode.put("foo", "form-bar");
    context.setForm(formdataNode);

    Multipart multipart = new MultipartNode(context);
    multipart.put("foo", "multipart-bar");
    context.setMultipart(multipart);
  }

  @Test
  public void testNoSources() {
    Throwable t = Assertions.assertThrows(IllegalArgumentException.class, this::test);
    assertEquals(t.getMessage(), "No parameter sources were specified.");
  }

  @Test
  public void testPriority() {
    test(PATH, HEADER, COOKIE, FLASH, SESSION, QUERY, FORM, MULTIPART);
    test(HEADER, COOKIE, FLASH, SESSION, QUERY, FORM, MULTIPART, PATH);
    test(COOKIE, FLASH, SESSION, QUERY, FORM, MULTIPART, PATH, HEADER);
    test(FLASH, SESSION, QUERY, FORM, MULTIPART, PATH, HEADER, COOKIE);
    test(SESSION, QUERY, FORM, MULTIPART, PATH, HEADER, COOKIE, FLASH);
    test(QUERY, FORM, MULTIPART, PATH, HEADER, COOKIE, FLASH, SESSION);
    test(FORM, MULTIPART, PATH, HEADER, COOKIE, FLASH, SESSION, QUERY);
    test(MULTIPART, PATH, HEADER, COOKIE, FLASH, SESSION, QUERY, FORM);

    test(PATH, l -> l.inPath().inHeader().inCookie().inFlash().inSession().inQuery().inForm().inMultipart());
    test(HEADER, l -> l.inHeader().inCookie().inFlash().inSession().inQuery().inForm().inMultipart().inPath());
    test(COOKIE, l -> l.inCookie().inFlash().inSession().inQuery().inForm().inMultipart().inPath().inHeader());
    test(FLASH, l -> l.inFlash().inSession().inQuery().inForm().inMultipart().inPath().inHeader().inCookie());
    test(SESSION, l -> l.inSession().inQuery().inForm().inMultipart().inPath().inHeader().inCookie().inFlash());
    test(QUERY, l -> l.inQuery().inForm().inMultipart().inPath().inHeader().inCookie().inFlash().inSession());
    test(FORM, l -> l.inForm().inMultipart().inPath().inHeader().inCookie().inFlash().inSession().inQuery());
    test(MULTIPART, l -> l.inMultipart().inPath().inHeader().inCookie().inFlash().inSession().inQuery().inForm());
  }

  @Test
  public void testMissingValue() {
    assertThrows(MissingValueException.class,
        () -> new MockContext().lookup("foo", PATH, HEADER, COOKIE, FLASH, SESSION, QUERY, FORM, MULTIPART).value());

    assertThrows(MissingValueException.class,
        () -> new MockContext().lookup().inPath().inHeader().inCookie().inFlash().inSession().inQuery().inForm().inMultipart().get("foo").value());
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
