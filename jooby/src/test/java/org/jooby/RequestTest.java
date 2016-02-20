package org.jooby;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class RequestTest {
  public class RequestMock implements Request {

    @Override
    public MediaType type() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean matches(final String pattern) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<MediaType> accept() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String contextPath() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<MediaType> accepts(final List<MediaType> types) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Mutant params() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Mutant param(final String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Mutant header(final String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Mutant> headers() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Mutant cookie(final String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Cookie> cookies() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Mutant body() throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T require(final Key<T> key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Charset charset() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Locale locale() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Locale locale(final Iterable<Locale> locales) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Locale locale(final Locale... locales) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Locale> locales() {
      throw new UnsupportedOperationException();
    }

    @Override
    public long length() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String ip() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Route route() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String hostname() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Session session() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Session> ifSession() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String protocol() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean secure() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> attributes() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> Optional<T> ifGet(final String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Request set(final String name, final Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Request set(final Key<?> key, final Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Request set(final Class<?> type, final Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Request set(final TypeLiteral<?> type, final Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> Optional<T> unset(final String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSet(final String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int port() {
      throw new UnsupportedOperationException();
    }

  }

  @Test
  public void accepts() throws Exception {
    LinkedList<MediaType> dataList = new LinkedList<>();
    new RequestMock() {
      @Override
      public Optional<MediaType> accepts(final List<MediaType> types) {
        dataList.addAll(types);
        return null;
      }
    }.accepts(MediaType.json);
    assertEquals(Arrays.asList(MediaType.json), dataList);
  }

  @Test
  public void acceptsStr() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    new RequestMock() {
      @Override
      public Optional<MediaType> accepts(final List<MediaType> types) {
        dataList.addAll(types);
        return null;
      }
    }.accepts("json");
    assertEquals(Arrays.asList(MediaType.json), dataList);
  }

  @Test
  public void getInstance() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    new RequestMock() {
      @Override
      public <T> T require(final Key<T> key) {
        dataList.add(key);
        return null;
      }
    }.require(Object.class);
    assertEquals(Arrays.asList(Key.get(Object.class)), dataList);
  }

  @Test
  public void getTypeLiteralInstance() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    new RequestMock() {
      @Override
      public <T> T require(final Key<T> key) {
        dataList.add(key);
        return null;
      }
    }.require(TypeLiteral.get(Object.class));
    assertEquals(Arrays.asList(Key.get(Object.class)), dataList);
  }

  @Test
  public void xhr() throws Exception {
    new MockUnit(Mutant.class)
        .expect(unit -> {
          Mutant xRequestedWith = unit.get(Mutant.class);
          expect(xRequestedWith.toOptional(String.class)).andReturn(Optional.of("XMLHttpRequest"));

          expect(xRequestedWith.toOptional(String.class)).andReturn(Optional.empty());
        })
        .run(unit -> {
          assertEquals(true, new RequestMock() {
            @Override
            public Mutant header(final String name) {
              assertEquals("X-Requested-With", name);
              return unit.get(Mutant.class);
            }
          }.xhr());

          assertEquals(false, new RequestMock() {
            @Override
            public Mutant header(final String name) {
              assertEquals("X-Requested-With", name);
              return unit.get(Mutant.class);
            }
          }.xhr());
        });
  }

  @Test
  public void path() throws Exception {
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.path()).andReturn("/path");
        })
        .run(unit -> {
          assertEquals("/path", new RequestMock() {
            @Override
            public Route route() {
              return unit.get(Route.class);
            }
          }.path());
        });
  }

  @Test
  public void verb() throws Exception {
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.method()).andReturn("PATCH");
        })
        .run(unit -> {
          assertEquals("PATCH", new RequestMock() {
            @Override
            public Route route() {
              return unit.get(Route.class);
            }
          }.method());
        });
  }

}
