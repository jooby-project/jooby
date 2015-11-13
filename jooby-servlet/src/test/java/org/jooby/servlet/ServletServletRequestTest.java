package org.jooby.servlet;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jooby.MediaType;
import org.jooby.test.MockUnit;
import org.junit.Test;

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
          expect(req.getRequestURI()).andReturn("/");
        })
        .run(unit -> {
          new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir);
        });
  }

  @Test
  public void defaultsNullCT() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn(null);
          expect(req.getRequestURI()).andReturn("/");
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
          expect(req.getRequestURI()).andReturn("/");
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
          expect(req.getRequestURI()).andReturn("/");
          expect(req.getMethod()).andReturn("GET");
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
          expect(req.getRequestURI()).andReturn("/spaces%20in%20it");
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
          expect(req.getRequestURI()).andReturn("/");
          expect(req.getParameterNames()).andReturn(
              Iterators.asEnumeration(Lists.newArrayList("p1", "p2").iterator()));
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
          expect(req.getRequestURI()).andReturn("/");
          expect(req.getParameterValues("x")).andReturn(new String[]{"a", "b" });
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
          expect(req.getRequestURI()).andReturn("/");
          expect(req.getParameterValues("x")).andReturn(null);
        })
        .run(unit -> {
          assertEquals(Lists.newArrayList(),
              new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir)
                  .params("x"));
        });

  }

  @Test(expected = IOException.class)
  public void filesFailure() throws IOException, Exception {
    String tmpdir = System.getProperty("java.io.tmpdir");
    new MockUnit(HttpServletRequest.class)
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          expect(req.getContentType()).andReturn(MediaType.multipart.name());
          expect(req.getRequestURI()).andReturn("/");
          expect(req.getParts()).andThrow(new ServletException("intentional err"));
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
          expect(req.getRequestURI()).andReturn("/");
        })
        .run(unit -> {
          assertEquals(Lists.newArrayList(),
              new ServletServletRequest(unit.get(HttpServletRequest.class), tmpdir)
                  .upgrade(ServletServletRequest.class));
        });

  }

}
