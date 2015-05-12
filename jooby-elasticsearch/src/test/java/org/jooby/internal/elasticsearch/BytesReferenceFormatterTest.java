package org.jooby.internal.elasticsearch;

import java.io.OutputStream;

import org.elasticsearch.common.bytes.BytesReference;
import org.jooby.MockUnit;
import org.jooby.Renderer;
import org.junit.Test;

public class BytesReferenceFormatterTest {

  @Test
  public void format() throws Exception {
    new MockUnit(BytesReference.class, Renderer.Context.class, OutputStream.class)
        .expect(unit -> {
          Renderer.Context ctx = unit.get(Renderer.Context.class);
          ctx.bytes(unit.capture(Renderer.Bytes.class));
        })
        .expect(unit -> {
          BytesReference bytes = unit.get(BytesReference.class);
          bytes.writeTo(unit.get(OutputStream.class));
        })
        .run(unit -> {
          new BytesReferenceFormatter().render(unit.get(BytesReference.class),
              unit.get(Renderer.Context.class));
        }, unit -> {
          unit.captured(Renderer.Bytes.class).iterator().next()
              .write(unit.get(OutputStream.class));
        });
  }

}
