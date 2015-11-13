package org.jooby.internal.elasticsearch;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Optional;

import org.elasticsearch.rest.RestRequest.Method;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EmbeddedHttpRequest.class })
public class EmbeddedHttpRequestTest {

  private byte[] bytes = "{}".getBytes();

  private MockUnit.Block newInstance = unit -> {
    Mutant body = unit.mock(Mutant.class);
    expect(body.to(byte[].class)).andReturn(bytes);
    Request request = unit.get(Request.class);
    expect(request.path()).andReturn("/search/customer?pretty");
    expect(request.length()).andReturn((long) bytes.length);
    expect(request.body()).andReturn(body);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Request.class)
        .expect(newInstance)
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals("", req.param("pretty"));
          assertEquals(true, req.hasContent());
          assertEquals(2, req.content().length());
        });
  }

  @Test
  public void defaultsNoParams() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);
          Mutant body = unit.mock(Mutant.class);
          expect(body.to(byte[].class)).andReturn(bytes);

          expect(request.path()).andReturn("/search/customer");
          expect(request.length()).andReturn((long) bytes.length);
          expect(request.body()).andReturn(body);
        })
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals(0, req.params().size());
          assertEquals(true, req.hasContent());
          assertEquals(2, req.content().length());
        });
  }

  @Test
  public void defaultsNoBody() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);
          expect(request.path()).andReturn("/search/customer");
          expect(request.length()).andReturn(0L);
        })
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals(0, req.params().size());
          assertEquals(false, req.hasContent());
          assertEquals(0, req.content().length());
        });
  }

  @Test
  public void content() throws Exception {
    new MockUnit(Request.class)
        .expect(newInstance)
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals(true, req.hasContent());
          assertEquals(bytes, req.content().array());
        });
  }

  @Test
  public void hasParam() throws Exception {
    new MockUnit(Request.class)
        .expect(newInstance)
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals(true, req.hasParam("pretty"));
          assertEquals(false, req.hasParam("nopretty"));
        });
  }

  @Test
  public void header() throws Exception {
    new MockUnit(Request.class)
        .expect(newInstance)
        .expect(unit -> {
          Mutant header = unit.mock(Mutant.class);
          expect(header.toOptional()).andReturn(Optional.of("header"));

          expect(header.toOptional()).andReturn(Optional.empty());

          Request req = unit.get(Request.class);
          expect(req.header("header")).andReturn(header);

          expect(req.header("noheader")).andReturn(header);
        })
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals("header", req.header("header"));
          assertEquals(null, req.header("noheader"));
        });
  }

  @Test
  public void headers() throws Exception {
    new MockUnit(Request.class)
        .expect(newInstance)
        .expect(unit -> {
          Mutant header = unit.mock(Mutant.class);
          expect(header.value()).andReturn("v1");

          expect(header.value()).andReturn("v2");

          Map<String, Mutant> headers = ImmutableMap.<String, Mutant> builder()
              .put("h1", header)
              .put("h2", header)
              .build();

          Request req = unit.get(Request.class);
          expect(req.headers()).andReturn(headers);
        })
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          Map<String, String> headers = ImmutableMap.<String, String> builder()
              .put("h1", "v1")
              .put("h2", "v2")
              .build();
          assertEquals(ImmutableList.copyOf(headers.entrySet()), req.headers());
        });
  }

  @Test
  public void method() throws Exception {
    new MockUnit(Request.class)
        .expect(newInstance)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.method()).andReturn("GET");
        })
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals(Method.GET, req.method());
        });
  }

  @Test
  public void param() throws Exception {
    new MockUnit(Request.class)
        .expect(newInstance)
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals("", req.param("pretty"));
        });
  }

  @Test
  public void paramWithDefaultValue() throws Exception {
    new MockUnit(Request.class)
        .expect(newInstance)
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals("v1", req.param("p", "v1"));
        });
  }

  @Test
  public void params() throws Exception {
    new MockUnit(Request.class)
        .expect(newInstance)
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals(ImmutableMap.of("pretty", ""), req.params());
        });
  }

  @Test
  public void rawPath() throws Exception {
    new MockUnit(Request.class)
        .expect(newInstance)
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals("/customer?pretty", req.rawPath());
        });
  }

  @Test
  public void uri() throws Exception {
    new MockUnit(Request.class)
        .expect(newInstance)
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals("/customer?pretty", req.uri());
        });
  }

  @Test
  public void contentUnsafe() throws Exception {
    new MockUnit(Request.class)
        .expect(newInstance)
        .run(unit -> {
          EmbeddedHttpRequest req = new EmbeddedHttpRequest("/search", unit.get(Request.class));
          assertEquals(false, req.contentUnsafe());
        });
  }

}
