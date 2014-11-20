package org.jooby;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import org.junit.Test;

import com.google.common.base.Charsets;

public class ResponseForwardingTest {

  @Test
  public void unwrap() throws Exception {
    new MockUnit(Response.class)
        .run(unit -> {
          Response rsp = unit.get(Response.class);

          assertEquals(rsp, Response.Forwarding.unwrap(new Response.Forwarding(rsp)));

          // 2 level
        assertEquals(rsp,
            Response.Forwarding.unwrap(new Response.Forwarding(new Response.Forwarding(rsp))));

        // 3 level
        assertEquals(rsp,
            Response.Forwarding.unwrap(new Response.Forwarding(new Response.Forwarding(
                new Response.Forwarding(rsp)))));

      });
  }

  @Test
  public void type() throws Exception {
    new MockUnit(Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);

          expect(rsp.type()).andReturn(Optional.empty());

          expect(rsp.type("json")).andReturn(rsp);
          expect(rsp.type(MediaType.js)).andReturn(rsp);
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));

          assertEquals(Optional.empty(), rsp.type());
          assertEquals(rsp, rsp.type("json"));
          assertEquals(rsp, rsp.type(MediaType.js));
        });
  }

  @Test
  public void header() throws Exception {
    new MockUnit(Response.class, Mutant.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.header("h")).andReturn(unit.get(Mutant.class));
        })
        .run(unit -> {
          assertEquals(unit.get(Mutant.class),
              new Response.Forwarding(unit.get(Response.class)).header("h"));
        });
  }

  @Test
  public void setheader() throws Exception {
    Date now = new Date();
    new MockUnit(Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.header("b", (byte) 1)).andReturn(null);
          expect(rsp.header("c", 'c')).andReturn(null);
          expect(rsp.header("s", "s")).andReturn(null);
          expect(rsp.header("d", now)).andReturn(null);
          expect(rsp.header("d", 3d)).andReturn(null);
          expect(rsp.header("f", 4f)).andReturn(null);
          expect(rsp.header("i", 8)).andReturn(null);
          expect(rsp.header("l", 9l)).andReturn(null);
          expect(rsp.header("s", (short) 2)).andReturn(null);
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));
          assertEquals(rsp, rsp.header("b", (byte) 1));
          assertEquals(rsp, rsp.header("c", 'c'));
          assertEquals(rsp, rsp.header("s", "s"));
          assertEquals(rsp, rsp.header("d", now));
          assertEquals(rsp, rsp.header("d", 3d));
          assertEquals(rsp, rsp.header("f", 4f));
          assertEquals(rsp, rsp.header("i", 8));
          assertEquals(rsp, rsp.header("l", 9l));
          assertEquals(rsp, rsp.header("s", (short) 2));
        });
  }

  @Test
  public void cookie() throws Exception {
    new MockUnit(Response.class, Cookie.class, Cookie.Definition.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.cookie(unit.get(Cookie.class))).andReturn(null);

          expect(rsp.cookie(unit.get(Cookie.Definition.class))).andReturn(null);

          expect(rsp.cookie("name", "value")).andReturn(null);
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));
          assertEquals(rsp, rsp.cookie(unit.get(Cookie.class)));
          assertEquals(rsp, rsp.cookie(unit.get(Cookie.Definition.class)));
          assertEquals(rsp, rsp.cookie("name", "value"));
        });
  }

  @Test
  public void download() throws Exception {
    File file = new File("file.ppt");
    new MockUnit(Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);

          rsp.download(file);
          rsp.download("alias", file);

          rsp.download("file.pdf");
          rsp.download("alias", "file.pdf");

          rsp.download(eq("file.pdf"), isA(InputStream.class));

          rsp.download(eq("file.pdf"), isA(Reader.class));
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));

          rsp.download(file);

          rsp.download("alias", file);

          rsp.download("file.pdf");

          rsp.download("alias", "file.pdf");

          rsp.download("file.pdf", new ByteArrayInputStream(new byte[0]));

          rsp.download("file.pdf", new StringReader(""));
        });
  }

  @Test
  public void format() throws Exception {
    new MockUnit(Response.class, Response.Formatter.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);

          expect(rsp.format()).andReturn(unit.get(Response.Formatter.class));

        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));

          assertEquals(unit.get(Response.Formatter.class), rsp.format());
        });
  }

  @Test
  public void charset() throws Exception {
    new MockUnit(Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.charset()).andReturn(Charsets.UTF_8);

          expect(rsp.charset(Charsets.US_ASCII)).andReturn(null);
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));
          assertEquals(Charsets.UTF_8, rsp.charset());

          assertEquals(rsp, rsp.charset(Charsets.US_ASCII));
        });
  }

  @Test
  public void clearCookie() throws Exception {
    new MockUnit(Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.clearCookie("cookie")).andReturn(null);
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));
          assertEquals(rsp, rsp.clearCookie("cookie"));
        });
  }

  @Test
  public void committed() throws Exception {
    new MockUnit(Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.committed()).andReturn(true);
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));
          assertEquals(true, rsp.committed());
        });
  }

  @Test
  public void length() throws Exception {
    new MockUnit(Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.length(10)).andReturn(null);
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));
          assertEquals(rsp, rsp.length(10));
        });
  }

  @Test
  public void local() throws Exception {
    new MockUnit(Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.local("name")).andReturn("str");

          expect(rsp.local("name", "val")).andReturn(rsp);
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));
          assertEquals("str", rsp.local("name"));

          assertEquals(rsp, rsp.local("name", "val"));
        });
  }

  @Test
  public void locals() throws Exception {
    new MockUnit(Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.locals()).andReturn(Collections.emptyMap());
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));
          assertEquals(Collections.emptyMap(), rsp.locals());
        });
  }

  @Test
  public void redirect() throws Exception {
    new MockUnit(Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.redirect("/location");

          rsp.redirect(Status.MOVED_PERMANENTLY, "/location");
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));
          rsp.redirect("/location");

          rsp.redirect(Status.MOVED_PERMANENTLY, "/location");
        });
  }

  @Test
  public void send() throws Exception {
    Body body = Body.ok();
    Object obody = new Object();
    new MockUnit(Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);

          rsp.send(body);

          rsp.send(obody);
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));

          rsp.send(body);

          rsp.send(obody);
        });
  }

  @Test
  public void status() throws Exception {
    new MockUnit(Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);

          expect(rsp.status()).andReturn(Optional.empty());

          expect(rsp.status(200)).andReturn(rsp);
          expect(rsp.status(Status.BAD_REQUEST)).andReturn(rsp);
        })
        .run(unit -> {
          Response rsp = new Response.Forwarding(unit.get(Response.class));

          assertEquals(Optional.empty(), rsp.status());
          assertEquals(rsp, rsp.status(200));
          assertEquals(rsp, rsp.status(Status.BAD_REQUEST));
        });
  }

  @Test
  public void toStr() throws Exception {

    Response rsp = new Response.Forwarding(new ResponseTest.ResponseMock() {
      @Override
      public String toString() {
        return "something something dark";
      }
    });

    assertEquals("something something dark", rsp.toString());
  }

}
