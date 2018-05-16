package org.jooby.assets;

import com.google.common.collect.ImmutableMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import java.util.HashMap;

public class AssetProcessorTest {

  @Test
  public void name() {
    assertEquals("compressor-test", new CompressorTest().name());
  }

  @SuppressWarnings("serial")
  @Test
  public void options() throws Exception {
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
  public void engineFactory() throws Exception {
    TestEngineFactory engineFactory = new TestEngineFactory();
    AssetProcessor processor = new CompressorTest()
        .set(engineFactory);
    assertNotNull(processor.engine(TestEngine.class));
  }

  @Test(expected = IllegalStateException.class)
  public void noEngine() throws Exception {
    AssetProcessor processor = new CompressorTest();
    assertNotNull(processor.engine(TestEngine.class));
  }

  @Test
  public void toStr() {
    assertEquals("compressor-test", new CompressorTest().toString());
  }

}
