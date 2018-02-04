package org.jooby.internal.pac4j2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import org.jooby.Cookie;
import org.jooby.Err;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Status;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class Pac4jContextTest {

  @Test
  public void newContext() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .run(unit -> {
          new Pac4jContext(unit.get(Request.class), unit.get(Response.class),
              unit.get(SessionStore.class));
        });
  }

  @Test
  public void newContextIgnoreBrokenParameter() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(true))
        .run(unit -> {
          new Pac4jContext(unit.get(Request.class), unit.get(Response.class),
              unit.get(SessionStore.class));
        });
  }

  @Test
  public void sessionStore() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.first(SessionStore.class));

          assertEquals(unit.first(SessionStore.class), pac4j.getSessionStore());
        });
  }

  @Test
  public void requestParameters() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(localGet("pac4j.foo", null))
        .expect(localGet("pac4j.bar", "foo"))
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("bar", pac4j.getRequestParameter("foo"));

          assertEquals("foo", pac4j.getRequestParameter("bar"));

          Map<String, String[]> requestParameters = pac4j.getRequestParameters();
          assertNotNull(requestParameters);
          assertArrayEquals(new String[]{"bar"}, requestParameters.get("foo"));
        });
  }

  @Test
  public void requestAttribute() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(localGet("attr", "val"))
        .expect(localGet("missing", null))
        .expect(localSet("foo", "bar"))
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("val", pac4j.getRequestAttribute("attr"));
          assertEquals(null, pac4j.getRequestAttribute("missing"));
          pac4j.setRequestAttribute("foo", "bar");
        });
  }

  @Test
  public void requestHeader() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(headerGet("attr", "val"))
        .expect(headerSet("foo", "bar"))
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("val", pac4j.getRequestHeader("attr"));
          pac4j.setResponseHeader("foo", "bar");
        });
  }

  @Test
  public void sessionAttribute() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(sessionGet("attr", "val"))
        .expect(sessionSet("foo", "bar"))
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("val", pac4j.getSessionAttribute("attr"));
          pac4j.setSessionAttribute("foo", "bar");

          assertEquals(2, unit.captured(WebContext.class).size());
          unit.captured(WebContext.class).forEach(it -> assertEquals(pac4j, it));
        });
  }

  @Test
  public void sessionId() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          SessionStore session = unit.get(SessionStore.class);
          expect(session.getOrCreateSessionId(unit.capture(WebContext.class))).andReturn("sid");
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("sid", pac4j.getSessionIdentifier());

          assertEquals(pac4j, unit.captured(WebContext.class).get(0));
        });
  }

  @Test
  public void path() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.path()).andReturn("/some");
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("/some", pac4j.getPath());
        });
  }

  @Test
  public void requestContent() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          Mutant body = unit.mock(Mutant.class);
          expect(body.value()).andReturn("...");

          Request req = unit.get(Request.class);
          expect(req.body()).andReturn(body);
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("...", pac4j.getRequestContent());
        });
  }

  @Test
  public void requestMethod() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.method()).andReturn("METHOD");
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("METHOD", pac4j.getRequestMethod());
        });
  }

  @Test
  public void requestRemoteAddress() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.ip()).andReturn("1.1.1.1");
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("1.1.1.1", pac4j.getRemoteAddr());
        });
  }

  @Test
  public void writeResponse() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.send("...");
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          pac4j.writeResponseContent("...");
        });
  }

  @Test
  public void writeResponseStatus() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.status(1234)).andReturn(rsp);
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          pac4j.setResponseStatus(1234);
        });
  }

  @Test
  public void writeResponseContentType() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.type("text/html")).andReturn(rsp);
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          pac4j.setResponseContentType("text/html");
        });
  }

  @Test
  public void serverNameAndPort() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.hostname()).andReturn("server");
          expect(req.port()).andReturn(123);
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("server", pac4j.getServerName());
          assertEquals(123, pac4j.getServerPort());
        });
  }

  @Test
  public void scheme() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.secure()).andReturn(false);
          expect(req.secure()).andReturn(true);
          expect(req.secure()).andReturn(false);
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("http", pac4j.getScheme());
          assertEquals("https", pac4j.getScheme());
          assertEquals(false, pac4j.isSecure());
        });
  }

  @Test
  public void fullRequestURL() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.hostname()).andReturn("server");
          expect(req.port()).andReturn(8080);
          expect(req.secure()).andReturn(false);
          expect(req.contextPath()).andReturn("");
          expect(req.path()).andReturn("/resource");
          expect(req.queryString()).andReturn(Optional.empty());
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("http://server:8080/resource", pac4j.getFullRequestURL());
        });
  }

  @Test
  public void fullRequestURLWithContextPath() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.hostname()).andReturn("server");
          expect(req.port()).andReturn(8080);
          expect(req.secure()).andReturn(false);
          expect(req.contextPath()).andReturn("/myapp");
          expect(req.path()).andReturn("/resource");
          expect(req.queryString()).andReturn(Optional.empty());
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("http://server:8080/myapp/resource", pac4j.getFullRequestURL());
        });
  }

  @Test
  public void fullRequestURLWithContextPathAndQueryString() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class)
        .expect(params(false))
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.hostname()).andReturn("server");
          expect(req.port()).andReturn(8080);
          expect(req.secure()).andReturn(true);
          expect(req.contextPath()).andReturn("/myapp");
          expect(req.path()).andReturn("/resource");
          expect(req.queryString()).andReturn(Optional.of("q=foo"));
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          assertEquals("https://server:8080/myapp/resource?q=foo", pac4j.getFullRequestURL());
        });
  }

  @Test
  public void cookies() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class, Cookie.class)
        .expect(params(false))
        .expect(unit -> {
          Cookie cookie = unit.mock(Cookie.class);
          expect(cookie.name()).andReturn("sid");
          expect(cookie.value()).andReturn(Optional.of("value"));
          expect(cookie.domain()).andReturn(Optional.of("foo.com"));
          expect(cookie.path()).andReturn(Optional.of("/"));
          expect(cookie.secure()).andReturn(true);
          expect(cookie.httpOnly()).andReturn(false);

          Request req = unit.get(Request.class);
          expect(req.cookies()).andReturn(ImmutableList.of(cookie));
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          Collection<org.pac4j.core.context.Cookie> cookies = pac4j.getRequestCookies();
          org.pac4j.core.context.Cookie cookie = cookies.iterator().next();
          assertEquals("sid", cookie.getName());
          assertEquals("value", cookie.getValue());
          assertEquals("foo.com", cookie.getDomain());
          assertEquals("/", cookie.getPath());
          assertEquals(true, cookie.isSecure());
          assertEquals(false, cookie.isHttpOnly());
        });
  }

  @Test
  public void writeCookie() throws Exception {
    new MockUnit(Request.class, Response.class, SessionStore.class, Cookie.class)
        .expect(params(false))
        .expect(unit-> {
          Response rsp = unit.get(Response.class);
          expect(rsp.cookie(unit.capture(Cookie.Definition.class))).andReturn(rsp);
        })
        .run(unit -> {
          Pac4jContext pac4j = new Pac4jContext(unit.get(Request.class),
              unit.get(Response.class), unit.get(SessionStore.class));

          pac4j.addResponseCookie(new org.pac4j.core.context.Cookie("foo", "bar"));
        }, unit -> {
          Cookie.Definition cookie = unit.captured(Cookie.Definition.class).get(0);
          assertEquals("foo", cookie.name().get());
          assertEquals("bar", cookie.value().get());
        });
  }

  private MockUnit.Block sessionSet(String name, String value) {
    return unit -> {
      SessionStore session = unit.get(SessionStore.class);
      session.set(unit.capture(WebContext.class), eq(name), eq(value));
    };
  }

  private MockUnit.Block sessionGet(String name, String value) {
    return unit -> {
      SessionStore session = unit.get(SessionStore.class);
      expect(session.get(unit.capture(WebContext.class), eq(name))).andReturn(value);
    };
  }

  private MockUnit.Block headerSet(String name, String value) {
    return unit -> {
      Response rsp = unit.get(Response.class);
      expect(rsp.header(name, value)).andReturn(rsp);
    };
  }

  private MockUnit.Block headerGet(String name, String value) {
    return unit -> {
      Mutant mutant = unit.mock(Mutant.class);
      expect(mutant.toOptional()).andReturn(Optional.ofNullable(value));
      Request request = unit.get(Request.class);
      expect(request.header(name)).andReturn(mutant);
    };
  }

  private MockUnit.Block localSet(String name, String value) {
    return unit -> {
      Request req = unit.get(Request.class);
      expect(req.set(name, value)).andReturn(req);
    };
  }

  private MockUnit.Block localGet(String name, String value) {
    return unit -> {
      Request request = unit.get(Request.class);
      expect(request.ifGet(name)).andReturn(Optional.ofNullable(value));
    };
  }

  private MockUnit.Block params(boolean fail) {
    return unit -> {
      Mutant bar = unit.mock(Mutant.class);
      if (fail) {
        expect(bar.toList()).andThrow(new Err(Status.BAD_REQUEST));
      } else {
        expect(bar.toList()).andReturn(Arrays.asList("bar"));
      }
      Map<String, Mutant> hash = ImmutableMap.of("foo", bar);

      Mutant params = unit.mock(Mutant.class);
      expect(params.toMap()).andReturn(hash);

      Request req = unit.get(Request.class);
      expect(req.params()).andReturn(params);
    };
  }
}
