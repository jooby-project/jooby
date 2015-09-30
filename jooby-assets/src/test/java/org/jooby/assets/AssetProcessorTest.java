package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class AssetProcessorTest {

  @Test
  public void name() {
    assertEquals("compressor-test", new CompressorTest().name());
  }

  @SuppressWarnings("serial")
  @Test
  public void options() {
    assertEquals(ImmutableMap.of("str", "str", "bool", true, "map", new HashMap<String, Object>() {
      {
        put("k", null);
      }
    }), new CompressorTest().set("str", "str").set("bool", true)
        .set("map", new HashMap<String, Object>() {
          {
            put("k", null);
          }
        }).options());
  }

  @Test
  public void toStr() {
    assertEquals("compressor-test", new CompressorTest().toString());
  }

}
