package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.jooby.Asset;
import org.jooby.BodyFormatter;
import org.jooby.MediaType;
import org.jooby.MockUnit;
import org.junit.Test;

import com.google.common.base.Charsets;

public class AssetFormatterTest {

  @Test
  public void text() throws Exception {
    InputStream stream = new ByteArrayInputStream("function () {}".getBytes());

    new MockUnit(Asset.class, BodyFormatter.Context.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);
          expect(asset.type()).andReturn(MediaType.js);
          expect(asset.stream()).andReturn(stream);
        })
        .expect(unit -> {
          BodyFormatter.Context writer = unit.get(BodyFormatter.Context.class);

          expect(writer.charset()).andReturn(Charsets.UTF_8);

          writer.text(unit.capture(BodyFormatter.Context.Text.class));
        })
        .run(unit -> {
          new AssetFormatter().format(unit.get(Asset.class), unit.get(BodyFormatter.Context.class));
        }, unit -> {
          BodyFormatter.Context.Text text = unit.captured(BodyFormatter.Context.Text.class)
              .iterator().next();
          StringWriter writer = new StringWriter();
          text.write(writer);
          assertEquals("function () {}", writer.toString());
        });
  }

  @Test(expected = IOException.class)
  public void textErr() throws Exception {
    new MockUnit(Asset.class, BodyFormatter.Context.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);
          expect(asset.type()).andReturn(MediaType.js);
          expect(asset.stream()).andThrow(new IOException());
        })
        .expect(unit -> {
          BodyFormatter.Context writer = unit.get(BodyFormatter.Context.class);

          expect(writer.charset()).andReturn(Charsets.UTF_8);

          writer.text(unit.capture(BodyFormatter.Context.Text.class));
        })
        .run(unit -> {
          new AssetFormatter().format(unit.get(Asset.class), unit.get(BodyFormatter.Context.class));
        }, unit -> {
          BodyFormatter.Context.Text text = unit.captured(BodyFormatter.Context.Text.class)
              .iterator().next();
          StringWriter writer = new StringWriter();
          text.write(writer);
        });
  }

  @Test
  public void bytes() throws Exception {
    byte[] bytes = {7, 19, 35 };
    InputStream stream = new ByteArrayInputStream(bytes);

    new MockUnit(Asset.class, BodyFormatter.Context.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);
          expect(asset.type()).andReturn(MediaType.valueOf("image/png"));
          expect(asset.stream()).andReturn(stream);
        })
        .expect(unit -> {
          BodyFormatter.Context writer = unit.get(BodyFormatter.Context.class);

          writer.bytes(unit.capture(BodyFormatter.Context.Bytes.class));
        })
        .run(unit -> {
          new AssetFormatter().format(unit.get(Asset.class), unit.get(BodyFormatter.Context.class));
        }, unit -> {
          BodyFormatter.Context.Bytes bin = unit.captured(BodyFormatter.Context.Bytes.class).iterator().next();
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          bin.write(out);
          assertArrayEquals(bytes, out.toByteArray());
        });
  }

  @Test(expected = IOException.class)
  public void bytesErr() throws Exception {
    new MockUnit(Asset.class, BodyFormatter.Context.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);
          expect(asset.type()).andReturn(MediaType.valueOf("image/png"));
          expect(asset.stream()).andThrow(new IOException());
        })
        .expect(unit -> {
          BodyFormatter.Context writer = unit.get(BodyFormatter.Context.class);

          writer.bytes(unit.capture(BodyFormatter.Context.Bytes.class));
        })
        .run(unit -> {
          new AssetFormatter().format(unit.get(Asset.class), unit.get(BodyFormatter.Context.class));
        }, unit -> {
          BodyFormatter.Context.Bytes bin = unit.captured(BodyFormatter.Context.Bytes.class).iterator().next();
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          bin.write(out);
        });
  }

}
