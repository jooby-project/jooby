package org.jooby;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class RequestForwardingTest {

  @Test
  public void unwrap() throws Exception {
    new MockUnit(Request.class)
        .run(unit -> {
          Request req = unit.get(Request.class);

          assertEquals(req, Request.Forwarding.unwrap(new Request.Forwarding(req)));

          // 2 level
        assertEquals(req,
            Request.Forwarding.unwrap(new Request.Forwarding(new Request.Forwarding(req))));

        // 3 level
        assertEquals(req, Request.Forwarding.unwrap(new Request.Forwarding(new Request.Forwarding(
            new Request.Forwarding(req)))));

      });
  }

  @Test
  public void path() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.path()).andReturn("/path");
        })
        .run(unit -> {
          assertEquals("/path", new Request.Forwarding(unit.get(Request.class)).path());
        });
  }

  @Test
  public void verb() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.method()).andReturn("HEAD");
        })
        .run(unit -> {
          assertEquals("HEAD", new Request.Forwarding(unit.get(Request.class)).method());
        });
  }

  @Test
  public void type() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.type()).andReturn(MediaType.json);
        })
        .run(unit -> {
          assertEquals(MediaType.json, new Request.Forwarding(unit.get(Request.class)).type());
        });
  }

  @Test
  public void accept() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.accept()).andReturn(MediaType.ALL);

          expect(req.accepts(MediaType.ALL)).andReturn(Optional.empty());

          expect(req.accepts(MediaType.json, MediaType.js)).andReturn(Optional.empty());

          expect(req.accepts("json", "js")).andReturn(Optional.empty());
        })
        .run(
            unit -> {
              assertEquals(MediaType.ALL, new Request.Forwarding(unit.get(Request.class)).accept());

              assertEquals(Optional.empty(),
                  new Request.Forwarding(unit.get(Request.class)).accepts(MediaType.ALL));

              assertEquals(Optional.empty(),
                  new Request.Forwarding(unit.get(Request.class)).accepts(MediaType.json,
                      MediaType.js));

              assertEquals(Optional.empty(),
                  new Request.Forwarding(unit.get(Request.class)).accepts("json", "js"));
            });
  }

  @Test
  public void params() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.params()).andReturn(Collections.emptyMap());
        })
        .run(unit -> {
          assertEquals(Collections.emptyMap(),
              new Request.Forwarding(unit.get(Request.class)).params());
        });
  }

  @Test
  public void param() throws Exception {
    new MockUnit(Request.class, Mutant.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.param("p")).andReturn(unit.get(Mutant.class));
        })
        .run(unit -> {
          assertEquals(unit.get(Mutant.class),
              new Request.Forwarding(unit.get(Request.class)).param("p"));
        });
  }

  @Test
  public void header() throws Exception {
    new MockUnit(Request.class, Mutant.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.header("h")).andReturn(unit.get(Mutant.class));
        })
        .run(unit -> {
          assertEquals(unit.get(Mutant.class),
              new Request.Forwarding(unit.get(Request.class)).header("h"));
        });
  }

  @Test
  public void headers() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.headers()).andReturn(Collections.emptyMap());
        })
        .run(unit -> {
          assertEquals(Collections.emptyMap(),
              new Request.Forwarding(unit.get(Request.class)).headers());
        });
  }

  @Test
  public void cookie() throws Exception {
    new MockUnit(Request.class, Cookie.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.cookie("c")).andReturn(Optional.of(unit.get(Cookie.class)));
        })
        .run(unit -> {
          assertEquals(Optional.of(unit.get(Cookie.class)),
              new Request.Forwarding(unit.get(Request.class)).cookie("c"));
        });
  }

  @Test
  public void cookies() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.cookies()).andReturn(Collections.emptyList());
        })
        .run(unit -> {
          assertEquals(Collections.emptyList(),
              new Request.Forwarding(unit.get(Request.class)).cookies());
        });
  }

  @Test
  public void body() throws Exception {
    TypeLiteral<Object> typeLiteral = TypeLiteral.get(Object.class);
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.body(typeLiteral)).andReturn(null);

          expect(req.body(Object.class)).andReturn(null);
        })
        .run(unit -> {
          assertEquals(null, new Request.Forwarding(unit.get(Request.class)).body(typeLiteral));

          assertEquals(null, new Request.Forwarding(unit.get(Request.class)).body(Object.class));
        });
  }

  @Test
  public void getInstance() throws Exception {
    Key<Object> key = Key.get(Object.class);
    TypeLiteral<Object> typeLiteral = TypeLiteral.get(Object.class);

    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.require(key)).andReturn(null);

          expect(req.require(typeLiteral)).andReturn(null);

          expect(req.require(Object.class)).andReturn(null);
        })
        .run(
            unit -> {
              assertEquals(null, new Request.Forwarding(unit.get(Request.class)).require(key));

              assertEquals(null,
                  new Request.Forwarding(unit.get(Request.class)).require(typeLiteral));

              assertEquals(null,
                  new Request.Forwarding(unit.get(Request.class)).require(Object.class));
            });
  }

  @Test
  public void charset() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.charset()).andReturn(Charsets.UTF_8);
        })
        .run(unit -> {
          assertEquals(Charsets.UTF_8, new Request.Forwarding(unit.get(Request.class)).charset());
        });
  }

  @Test
  public void length() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.length()).andReturn(10L);
        })
        .run(unit -> {
          assertEquals(10L, new Request.Forwarding(unit.get(Request.class)).length());
        });
  }

  @Test
  public void locale() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.locale()).andReturn(Locale.getDefault());
        })
        .run(
            unit -> {
              assertEquals(Locale.getDefault(),
                  new Request.Forwarding(unit.get(Request.class)).locale());
            });
  }

  @Test
  public void ip() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.ip()).andReturn("127.0.0.1");
        })
        .run(unit -> {
          assertEquals("127.0.0.1", new Request.Forwarding(unit.get(Request.class)).ip());
        });
  }

  @Test
  public void route() throws Exception {
    new MockUnit(Request.class, Route.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.route()).andReturn(unit.get(Route.class));
        })
        .run(
            unit -> {
              assertEquals(unit.get(Route.class),
                  new Request.Forwarding(unit.get(Request.class)).route());
            });
  }

  @Test
  public void session() throws Exception {
    new MockUnit(Request.class, Session.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.session()).andReturn(unit.get(Session.class));
        })
        .run(
            unit -> {
              assertEquals(unit.get(Session.class),
                  new Request.Forwarding(unit.get(Request.class)).session());
            });
  }

  @Test
  public void ifSession() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.ifSession()).andReturn(Optional.empty());
        })
        .run(
            unit -> {
              assertEquals(Optional.empty(),
                  new Request.Forwarding(unit.get(Request.class)).ifSession());
            });
  }

  @Test
  public void hostname() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.hostname()).andReturn("localhost");
        })
        .run(unit -> {
          assertEquals("localhost", new Request.Forwarding(unit.get(Request.class)).hostname());
        });
  }

  @Test
  public void protocol() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.protocol()).andReturn("https");
        })
        .run(unit -> {
          assertEquals("https", new Request.Forwarding(unit.get(Request.class)).protocol());
        });
  }

  @Test
  public void secure() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.secure()).andReturn(true);
        })
        .run(unit -> {
          assertEquals(true, new Request.Forwarding(unit.get(Request.class)).secure());
        });
  }

  @Test
  public void xhr() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.xhr()).andReturn(true);
        })
        .run(unit -> {
          assertEquals(true, new Request.Forwarding(unit.get(Request.class)).xhr());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void attributes() throws Exception {
    new MockUnit(Request.class, Map.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.attributes()).andReturn(unit.get(Map.class));
        })
        .run(unit -> {
          assertEquals(unit.get(Map.class),
              new Request.Forwarding(unit.get(Request.class)).attributes());
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.get("name")).andReturn(Optional.of("value"));
        })
        .run(unit -> {
          assertEquals(Optional.of("value"),
              new Request.Forwarding(unit.get(Request.class)).get("name"));
        });
  }

  @Test
  public void set() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.set("name", "value")).andReturn(req);
        })
        .run(unit -> {
          assertNotEquals(unit.get(Request.class),
              new Request.Forwarding(unit.get(Request.class)).set("name", "value"));
        });
  }

  @Test
  public void setWithKey() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.set(Key.get(String.class), "value")).andReturn(req);
        })
        .run(unit -> {
          assertNotEquals(unit.get(Request.class),
              new Request.Forwarding(unit.get(Request.class)).set(Key.get(String.class), "value"));
        });
  }

  @Test
  public void setWithClass() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.set(String.class, "value")).andReturn(req);
        })
        .run(unit -> {
          assertNotEquals(unit.get(Request.class),
              new Request.Forwarding(unit.get(Request.class)).set(String.class, "value"));
        });
  }

  @Test
  public void setWithTypeLiteral() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.set(TypeLiteral.get(String.class), "value")).andReturn(req);
        })
        .run(unit -> {
          assertNotEquals(unit.get(Request.class),
              new Request.Forwarding(unit.get(Request.class)).set(TypeLiteral.get(String.class), "value"));
        });
  }

  @Test
  public void unset() throws Exception {
    new MockUnit(Request.class, Map.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.unset("name")).andReturn(Optional.empty());
        })
        .run(unit -> {
              assertEquals(Optional.empty(),
                  new Request.Forwarding(unit.get(Request.class)).unset("name"));
            });
  }

  @Test
  public void toStringFwd() throws Exception {
    new MockUnit(Request.class, Map.class)
        .run(unit -> {
              assertEquals(unit.get(Request.class).toString(),
                  new Request.Forwarding(unit.get(Request.class)).toString());
            });
  }

}
