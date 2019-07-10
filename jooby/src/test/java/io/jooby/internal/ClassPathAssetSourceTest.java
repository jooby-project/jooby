package io.jooby.internal;

import io.jooby.Asset;
import io.jooby.AssetSource;
import io.jooby.MediaType;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ClassPathAssetSourceTest {

  @Test
  public void checkclasspathFiles() {
    assetSource("/META-INF/resources/webjars/vue/2.5.22", source -> {
      Asset vuejs = source.resolve("dist/vue.js");
      assertNotNull(vuejs);
      assertEquals(MediaType.js, vuejs.getContentType());

      Asset packagejson = source.resolve("package.json");
      assertNotNull(packagejson);
      assertEquals(MediaType.json, packagejson.getContentType());

      Asset root = source.resolve("");
      assertNull(root);
    });

    assetSource("/META-INF/resources/webjars/vue/2.5.22/dist", source -> {
      Asset vuejs = source.resolve("vue.js");
      assertNotNull(vuejs);
      assertEquals(MediaType.js, vuejs.getContentType());

      Asset root = source.resolve("");
      assertNull(root);
    });

    assetSource("/META-INF/resources/webjars/vue/2.5.22/dist/vue.js", source -> {
      Asset vuejs = source.resolve("vue.js");
      assertNotNull(vuejs);
      assertEquals(MediaType.js, vuejs.getContentType());
    });

    assetSource("/", source -> {
      Asset logback = source.resolve("logback.xml");
      assertNotNull(logback);
      assertEquals(MediaType.xml, logback.getContentType());
    });
  }

  private void assetSource(String location, Consumer<AssetSource> consumer) {
    AssetSource source = new ClassPathAssetSource(getClass().getClassLoader(), location);
    consumer.accept(source);
  }
}
