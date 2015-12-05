package org.jooby.internal.pac4j;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jooby.Cookie;
import org.jooby.Err;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Session;
import org.jooby.Status;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class AuthContextTest {

  private Block params1 = unit -> {
    Mutant param = unit.get(Mutant.class);
    expect(param.toList()).andReturn(ImmutableList.of("v1"));
    expect(param.toList()).andThrow(new Err(Status.BAD_REQUEST));

    Map<String, Mutant> map = ImmutableMap.of("p1", param, "p2", param);

    Mutant params = unit.mock(Mutant.class);
    expect(params.toMap()).andReturn(map);

    Request req = unit.get(Request.class);
    expect(req.params()).andReturn(params);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .run(unit -> {
          new AuthContext(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void param() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals("v1", ctx.getRequestParameter("p1"));
          assertEquals(null, ctx.getRequestParameter("p2"));
        });
  }

  @Test
  public void params() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          Map<String, String[]> params = ctx.getRequestParameters();
          assertArrayEquals(new String[]{"v1" }, params.get("p1"));
        });
  }

  @Test
  public void header() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Mutant header = unit.get(Mutant.class);
          expect(header.toOptional()).andReturn(Optional.of("v1"));
          expect(header.toOptional()).andReturn(Optional.empty());

          Request req = unit.get(Request.class);
          expect(req.header("h1")).andReturn(header);
          expect(req.header("h2")).andReturn(header);
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals("v1", ctx.getRequestHeader("h1"));
          assertEquals(null, ctx.getRequestHeader("h2"));
        });
  }

  @Test
  public void setSessionAttr() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Session session = unit.mock(Session.class);
          expect(session.set("s", "v")).andReturn(session);
          expect(session.unset("u")).andReturn(unit.mock(Mutant.class));

          Request req = unit.get(Request.class);
          expect(req.session()).andReturn(session).times(2);
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          ctx.setSessionAttribute("s", "v");
          ctx.setSessionAttribute("u", null);
        });
  }

  @Test
  public void getSessionAttr() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Mutant attr = unit.mock(Mutant.class);
          expect(attr.toOptional()).andReturn(Optional.of("v"));

          Session session = unit.mock(Session.class);
          expect(session.get("s")).andReturn(attr);

          Request req = unit.get(Request.class);
          expect(req.session()).andReturn(session);
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals("v", ctx.getSessionAttribute("s"));
        });
  }

  @Test
  public void getReqAttr() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.get("r")).andReturn(Optional.of("v"));
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals("v", ctx.getRequestAttribute("r"));
        });

    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.get("r")).andReturn(Optional.empty());
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals(null, ctx.getRequestAttribute("r"));
        });
  }

  @Test
  public void setAttr() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.set("r", "v")).andReturn(req);
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          ctx.setRequestAttribute("r", "v");
        });

  }

  @Test
  public void sessionID() throws Exception {
    new MockUnit(Request.class, Response.class, Session.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Session session = unit.get(Session.class);
          expect(session.id()).andReturn("sid");

          Request req = unit.get(Request.class);
          expect(req.session()).andReturn(session);
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals("sid", ctx.getSessionIdentifier());
        });
  }

  @Test
  public void getRemmoteAddr() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.ip()).andReturn("0.0.0.0");
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals("0.0.0.0", ctx.getRemoteAddr());
        });
  }

  @Test
  public void setEncoding() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          ctx.setResponseCharacterEncoding("UTF-8");
        });
  }

  @Test
  public void setContentType() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.type("text/html")).andReturn(rsp);
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          ctx.setResponseContentType("text/html");
        });
  }

  @Test
  public void getCookies() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Cookie c = unit.mock(Cookie.class);
          expect(c.name()).andReturn("c");
          expect(c.value()).andReturn(Optional.of("v"));
          expect(c.domain()).andReturn(Optional.of("jooby.org"));
          expect(c.path()).andReturn(Optional.of("/"));
          expect(c.httpOnly()).andReturn(true);
          expect(c.secure()).andReturn(false);

          Request req = unit.get(Request.class);
          List<Cookie> cookies = Lists.newArrayList(c);
          expect(req.cookies()).andReturn(cookies);
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals(1, ctx.getRequestCookies().size());
        });
  }

  @Test
  public void addCookie() throws Exception {
    org.pac4j.core.context.Cookie cookie = new org.pac4j.core.context.Cookie("c", "v");
    cookie.setDomain("jooby.org");
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setSecure(false);
    cookie.setMaxAge(-1);

    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.cookie(isA(Cookie.Definition.class))).andReturn(rsp);
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          ctx.addResponseCookie(cookie);
        });
  }

  @Test
  public void method() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.method()).andReturn("GET");
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals("GET", ctx.getRequestMethod());
        });
  }

  @Test
  public void writeResponse() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.send("text");
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          ctx.writeResponseContent("text");
        });
  }

  @Test(expected = Err.class)
  public void writeResponseWithErr() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.send("text");
          expectLastCall().andThrow(new RuntimeException());
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          ctx.writeResponseContent("text");
        });
  }

  @Test
  public void setResponseStatus() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.status(200)).andReturn(rsp);
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          ctx.setResponseStatus(200);
        });
  }

  @Test
  public void setResponseHeader() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.header("h1", "v1")).andReturn(rsp);
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          ctx.setResponseHeader("h1", "v1");
        });
  }

  @Test
  public void serverName() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.hostname()).andReturn("localhost");
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals("localhost", ctx.getServerName());
        });
  }

  @Test
  public void serverPort() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.port()).andReturn(8080);
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals(8080, ctx.getServerPort());
        });
  }

  @Test
  public void scheme() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.secure()).andReturn(false);
          expect(req.secure()).andReturn(true);
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals("http", ctx.getScheme());
          assertEquals("https", ctx.getScheme());
        });
  }

  @Test
  public void fullRequestURL() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params1)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.secure()).andReturn(false);
          expect(req.hostname()).andReturn("localhost");
          expect(req.port()).andReturn(8080);
          expect(req.path()).andReturn("/login");
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals("http://localhost:8080/login", ctx.getFullRequestURL());
        });
  }

}
