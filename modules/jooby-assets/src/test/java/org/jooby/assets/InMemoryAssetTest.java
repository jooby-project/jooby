package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jooby.Asset;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.io.ByteStreams;

public class InMemoryAssetTest {

  @Test
  public void length() throws Exception {
    new MockUnit(Asset.class)
        .run(unit -> {
          assertEquals(5L,
              new InMemoryAsset(unit.get(Asset.class), "bytes".getBytes(StandardCharsets.UTF_8))
                  .length());
        });
  }

  @Test
  public void stream() throws Exception {
    new MockUnit(Asset.class)
        .run(unit -> {
          InputStream stream = new InMemoryAsset(unit.get(Asset.class),
              "bytes".getBytes(StandardCharsets.UTF_8)).stream();
          assertEquals("bytes", new String(ByteStreams.toByteArray(stream), "UTF-8"));
        });
  }

}
