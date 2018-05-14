package org.jooby.servlet;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jooby.MediaType;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class ServletServletRequestTest {

  @Test
  public void defaults() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn("text/html");
          expect(req.getPathInfo()).andReturn("/");
          expect(req.getContextPath()).andReturn("");
        })
        .run(unit -> {
          new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir);
        });
  }

  @Test
  public void nullPathInfo() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn("text/html");
          expect(req.getPathInfo()).andReturn(null);
          expect(req.getContextPath()).andReturn("");
        })
        .run(unit -> {
          String path = new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir)
              .path();
          assertEquals("/", path);
        });
  }

  @Test
  public void withContextPath() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn("text/html");
          expect(req.getPathInfo()).andReturn(null);
          expect(req.getContextPath()).andReturn("/foo");
        })
        .run(unit -> {
          String path = new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir)
              .path();
          assertEquals("/foo/", path);
        });
  }

  @Test
  public void defaultsNullCT() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn(null);
          expect(req.getPathInfo()).andReturn("/");
          expect(req.getContextPath()).andReturn("");
        })
        .run(unit -> {
          new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir);
        });

  }

  @Test
  public void multipartDefaults() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn(MediaType.multipart.name());
          expect(req.getPathInfo()).andReturn("/");
          expect(req.getContextPath()).andReturn("");
        })
        .run(unit -> {
          new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir);
        });
  }

  @Test
  public void reqMethod() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn("text/html");
          expect(req.getPathInfo()).andReturn("/");
          expect(req.getMethod()).andReturn("GET");
          expect(req.getContextPath()).andReturn("");
        })
        .run(unit -> {
          assertEquals("GET", new ServletServletRequest(unit.get(HttpServletRequest.class),
              tmpdir).method());
        });

  }

  @Test
  public void path() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn("text/html");
          expect(req.getPathInfo()).andReturn("/spaces%20in%20it");
          expect(req.getContextPath()).andReturn("");
        })
        .run(unit -> {
          assertEquals("/spaces in it",
              new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir).path());
        });

  }

  @Test
  public void paramNames() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn("text/html");
          expect(req.getPathInfo()).andReturn("/");
          expect(req.getParameterNames()).andReturn(
              Iterators.asEnumeration(Lists.newArrayList("p1", "p2").iterator()));
          expect(req.getContextPath()).andReturn("");
        })
        .run(unit -> {
          assertEquals(Lists.newArrayList("p1", "p2"),
              new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir)
                  .paramNames());
        });

  }

  @Test
  public void params() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn("text/html");
          expect(req.getPathInfo()).andReturn("/");
          expect(req.getParameterValues("x")).andReturn(new String[]{"a", "b" });
          expect(req.getContextPath()).andReturn("");
        })
        .run(unit -> {
          assertEquals(Lists.newArrayList("a", "b"),
              new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir)
                  .params("x"));
        });

  }

  @Test
  public void noparams() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn("text/html");
          expect(req.getPathInfo()).andReturn("/");
          expect(req.getParameterValues("x")).andReturn(null);
          expect(req.getContextPath()).andReturn("");
        })
        .run(unit -> {
          assertEquals(Lists.newArrayList(),
              new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir)
                  .params("x"));
        });

  }

  @Test
  public void attributes() throws Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    final UUID serverAttribute = UUID.randomUUID();
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn("text/html");
          expect(req.getPathInfo()).andReturn("/");
          expect(req.getContextPath()).andReturn("");
          expect(req.getAttributeNames()).andReturn(
              Collections.enumeration(Collections.singletonList("server.attribute")));
          expect(req.getAttribute("server.attribute")).andReturn(serverAttribute);
        })
        .run(unit -> {
          assertEquals(ImmutableMap.of("server.attribute", serverAttribute),
              new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir)
                  .attributes());
        });

  }

  @Test
  public void emptyAttributes() throws Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn("text/html");
          expect(req.getPathInfo()).andReturn("/");
          expect(req.getContextPath()).andReturn("");
          expect(req.getAttributeNames()).andReturn(Collections.emptyEnumeration());
        })
        .run(unit -> {
          assertEquals(Collections.emptyMap(),
              new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir)
                  .attributes());
        });

  }

  @Test(expected = IOException.class)
  public void filesFailure() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn(MediaType.multipart.name());
          expect(req.getPathInfo()).andReturn("/");
          expect(req.getParts()).andThrow(new ServletException("intentional err"));
          expect(req.getContextPath()).andReturn("");
        })
        .run(unit -> {
          new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir)
              .files("x");
        });

  }

  @Test(expected = UnsupportedOperationException.class)
  public void noupgrade() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn(MediaType.multipart.name());
          expect(req.getPathInfo()).andReturn("/");
          expect(req.getContextPath()).andReturn("");
        })
        .run(unit -> {
          assertEquals(Lists.newArrayList(),
              new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir)
                  .upgrade(ServletServletRequest.class));
        });

  }

}
