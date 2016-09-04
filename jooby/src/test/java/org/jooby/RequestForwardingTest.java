package org.jooby;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.jooby.Request.Forwarding;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
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
  public void port() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.port()).andReturn(80);
        })
        .run(unit -> {
          assertEquals(80, new Request.Forwarding(unit.get(Request.class)).port());
        });
  }

  @Test
  public void matches() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.matches("/x")).andReturn(true);
        })
        .run(unit -> {
          assertEquals(true, new Request.Forwarding(unit.get(Request.class)).matches("/x"));
        });
  }

  @Test
  public void cpath() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.contextPath()).andReturn("");
        })
        .run(unit -> {
          assertEquals("", new Request.Forwarding(unit.get(Request.class)).contextPath());
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
  public void is() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);

          expect(req.is(MediaType.ALL)).andReturn(true);

          expect(req.is(MediaType.json, MediaType.js)).andReturn(true);

          expect(req.is("json", "js")).andReturn(true);
        })
        .run(unit -> {
          assertEquals(true,
              new Request.Forwarding(unit.get(Request.class)).is(MediaType.ALL));

          assertEquals(true,
              new Request.Forwarding(unit.get(Request.class)).is(MediaType.json, MediaType.js));

          assertEquals(true,
              new Request.Forwarding(unit.get(Request.class)).is("json", "js"));
        });
  }

  @Test
  public void isSet() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);

          expect(req.isSet("x")).andReturn(true);
        })
        .run(unit -> {
          assertEquals(true,
              new Request.Forwarding(unit.get(Request.class)).isSet("x"));
        });
  }

  @Test
  public void params() throws Exception {
    new MockUnit(Request.class, Mutant.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.params()).andReturn(unit.get(Mutant.class));
        })
        .run(unit -> {
          assertEquals(unit.get(Mutant.class),
              new Request.Forwarding(unit.get(Request.class)).params());
        });
  }

  @Test
  public void beanParam() throws Exception {
    Object bean = new Object();
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          Mutant params = unit.mock(Mutant.class);
          expect(params.to(Object.class)).andReturn(bean);
          expect(params.to(TypeLiteral.get(Object.class))).andReturn(bean);

          expect(req.params()).andReturn(params).times(2);
        })
        .run(
            unit -> {
              assertEquals(bean,
                  new Request.Forwarding(unit.get(Request.class)).params().to(Object.class));

              assertEquals(
                  bean,
                  new Request.Forwarding(unit.get(Request.class)).params().to(
                      TypeLiteral.get(Object.class)));
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
    new MockUnit(Request.class, Mutant.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.cookie("c")).andReturn(unit.get(Mutant.class));
        })
        .run(unit -> {
          assertEquals(unit.get(Mutant.class),
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
          Mutant body = unit.mock(Mutant.class);
          expect(body.to(typeLiteral)).andReturn(null);
          expect(body.to(Object.class)).andReturn(null);

          expect(req.body()).andReturn(body).times(2);
        })
        .run(
            unit -> {
              assertEquals(null,
                  new Request.Forwarding(unit.get(Request.class)).body().to(typeLiteral));

              assertEquals(null,
                  new Request.Forwarding(unit.get(Request.class)).body().to(Object.class));
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
  public void file() throws Exception {
    new MockUnit(Request.class, Upload.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.file("f")).andReturn(unit.get(Upload.class));
        })
        .run(unit -> {
          assertEquals(unit.get(Upload.class),
              new Request.Forwarding(unit.get(Request.class)).file("f"));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void files() throws Exception {
    new MockUnit(Request.class, List.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.files("f")).andReturn(unit.get(List.class));
        })
        .run(unit -> {
          assertEquals(unit.get(List.class),
              new Request.Forwarding(unit.get(Request.class)).files("f"));
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
  public void localeLookup() throws Exception {
    BiFunction<List<Locale.LanguageRange>, List<Locale>, Locale> lookup = Locale::lookup;
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.locale(lookup)).andReturn(Locale.getDefault());
        })
        .run(
            unit -> {
              assertEquals(Locale.getDefault(),
                  new Request.Forwarding(unit.get(Request.class)).locale(lookup));
            });
  }

  @Test
  public void locales() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.locales()).andReturn(Arrays.asList(Locale.getDefault()));
        })
        .run(
            unit -> {
              assertEquals(Arrays.asList(Locale.getDefault()),
                  new Request.Forwarding(unit.get(Request.class)).locales());
            });
  }

  @Test
  public void localesFilter() throws Exception {
    BiFunction<List<Locale.LanguageRange>, List<Locale>, List<Locale>> lookup = Locale::filter;
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.locales(lookup)).andReturn(Arrays.asList(Locale.getDefault()));
        })
        .run(unit -> {
          assertEquals(Arrays.asList(Locale.getDefault()),
              new Request.Forwarding(unit.get(Request.class)).locales(lookup));
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
  public void ifGet() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.ifGet("name")).andReturn(Optional.of("value"));
        })
        .run(unit -> {
          assertEquals(Optional.of("value"),
              new Request.Forwarding(unit.get(Request.class)).ifGet("name"));
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.get("name")).andReturn("value");
        })
        .run(unit -> {
          assertEquals("value",
              new Request.Forwarding(unit.get(Request.class)).get("name"));
        });
  }

  @Test
  public void push() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.push("/path")).andReturn(req);
        })
        .run(unit -> {
          Forwarding req = new Request.Forwarding(unit.get(Request.class));
          assertEquals(req, req.push("/path"));
        });

    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.push("/path", ImmutableMap.of("k", "v"))).andReturn(req);
        })
        .run(unit -> {
          Forwarding req = new Request.Forwarding(unit.get(Request.class));
          assertEquals(req, req.push("/path", ImmutableMap.of("k", "v")));
        });
  }

  @Test
  public void getdef() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.get("name", "v")).andReturn("value");
        })
        .run(unit -> {
          assertEquals("value",
              new Request.Forwarding(unit.get(Request.class)).get("name", "v"));
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
        .run(
            unit -> {
              assertNotEquals(unit.get(Request.class),
                  new Request.Forwarding(unit.get(Request.class)).set(
                      TypeLiteral.get(String.class), "value"));
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
  public void flash() throws Exception {
    new MockUnit(Request.class, Map.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.flash()).andReturn(Collections.emptyMap());
        })
        .run(unit -> {
          new Request.Forwarding(unit.get(Request.class)).flash();
        });
  }

  @Test
  public void setFlashAttr() throws Exception {
    new MockUnit(Request.class, Map.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.flash("foo", "bar")).andReturn(req);
        })
        .run(unit -> {
          assertNotEquals(unit.get(Request.class),
              new Request.Forwarding(unit.get(Request.class)).flash("foo", "bar"));
        });
  }

  @Test
  public void getFlashAttr() throws Exception {
    new MockUnit(Request.class, Map.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.flash("foo")).andReturn("bar");
        })
        .run(unit -> {
          assertEquals("bar",
              new Request.Forwarding(unit.get(Request.class)).flash("foo"));
        });
  }

  @Test
  public void getIfFlashAttr() throws Exception {
    new MockUnit(Request.class, Map.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.ifFlash("foo")).andReturn(Optional.of("bar"));
        })
        .run(unit -> {
          assertEquals("bar",
              new Request.Forwarding(unit.get(Request.class)).ifFlash("foo").get());
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

  @Test
  public void form() throws Exception {
    RequestForwardingTest v = new RequestForwardingTest();
    new MockUnit(Request.class, Map.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          Mutant params = unit.mock(Mutant.class);
          expect(params.to(RequestForwardingTest.class)).andReturn(v);

          expect(req.params()).andReturn(params);
        })
        .run(
            unit -> {
              assertEquals(
                  v,
                  new Request.Forwarding(unit.get(Request.class)).params().to(
                      RequestForwardingTest.class));
            });
  }

  @Test
  public void bodyWithType() throws Exception {
    RequestForwardingTest v = new RequestForwardingTest();
    new MockUnit(Request.class, Map.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);

          expect(req.body(RequestForwardingTest.class)).andReturn(v);
        })
        .run(unit -> {
          assertEquals(
              v,
              new Request.Forwarding(unit.get(Request.class)).body(
                  RequestForwardingTest.class));
        });
  }

  @Test
  public void paramsWithType() throws Exception {
    RequestForwardingTest v = new RequestForwardingTest();
    new MockUnit(Request.class, Map.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);

          expect(req.params(RequestForwardingTest.class)).andReturn(v);
        })
        .run(unit -> {
          assertEquals(
              v,
              new Request.Forwarding(unit.get(Request.class)).params(
                  RequestForwardingTest.class));
        });
  }
}
