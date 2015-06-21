package org.jooby.internal.elasticsearch;

import static org.easymock.EasyMock.expect;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.jooby.Renderer;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class BytesReferenceRendererTest {

  @Test
  public void render() throws Exception {
    new MockUnit(BytesReference.class, Renderer.Context.class, StreamInput.class)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.send(unit.get(StreamInput.class));
        })
        .expect(unit -> {
          BytesReference bytes = unit.get(BytesReference.class);
          expect(bytes.streamInput()).andReturn(unit.get(StreamInput.class));
        })
        .run(unit -> {
          new BytesReferenceRenderer().render(unit.get(BytesReference.class),
              unit.get(Renderer.Context.class));
        });
  }

  @Test
  public void renderIgnored() throws Exception {
    new MockUnit(Renderer.Context.class)
        .run(unit -> {
          new BytesReferenceRenderer().render(new Object(),
              unit.get(Renderer.Context.class));
        });
  }

}
