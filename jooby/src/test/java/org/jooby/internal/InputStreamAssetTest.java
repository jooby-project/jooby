package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import org.jooby.MediaType;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class InputStreamAssetTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(InputStream.class)
        .run(unit -> {
          InputStreamAsset asset =
              new InputStreamAsset(
                  unit.get(InputStream.class),
                  "stream.bin",
                  MediaType.octetstream
              );
          assertEquals(-1, asset.lastModified());
          assertEquals(-1, asset.length());
          assertEquals("stream.bin", asset.name());
          assertEquals("stream.bin", asset.path());
          assertEquals(unit.get(InputStream.class), asset.stream());
          assertEquals(MediaType.octetstream, asset.type());
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void noResource() throws Exception {
    new MockUnit(InputStream.class)
        .run(unit -> {
          new InputStreamAsset(
              unit.get(InputStream.class),
              "stream.bin",
              MediaType.octetstream
          ).resource();
        });
  }

}
