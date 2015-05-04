package org.jooby.internal.elasticsearch;

import static org.junit.Assert.assertEquals;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.elasticsearch.common.bytes.BytesReference;
import org.jooby.BodyFormatter;
import org.jooby.MediaType;
import org.jooby.MockUnit;
import org.junit.Test;

public class BytesReferenceFormatterTest {

  @Test
  public void types() {
    assertEquals(MediaType.ALL, new BytesReferenceFormatter().types());
  }

  @Test
  public void canFormat() {
    assertEquals(true, new BytesReferenceFormatter().canFormat(BytesReference.class));
    assertEquals(false, new BytesReferenceFormatter().canFormat(byte[].class));
    assertEquals(false, new BytesReferenceFormatter().canFormat(ByteBuffer.class));
  }

  @Test
  public void format() throws Exception {
    new MockUnit(BytesReference.class, BodyFormatter.Context.class, OutputStream.class)
        .expect(unit -> {
          BodyFormatter.Context ctx = unit.get(BodyFormatter.Context.class);
          ctx.bytes(unit.capture(BodyFormatter.Context.Bytes.class));
        })
        .expect(unit -> {
          BytesReference bytes = unit.get(BytesReference.class);
          bytes.writeTo(unit.get(OutputStream.class));
        })
        .run(unit -> {
          new BytesReferenceFormatter().format(unit.get(BytesReference.class),
              unit.get(BodyFormatter.Context.class));
        }, unit -> {
          unit.captured(BodyFormatter.Context.Bytes.class).iterator().next()
              .write(unit.get(OutputStream.class));
        });
  }

}
