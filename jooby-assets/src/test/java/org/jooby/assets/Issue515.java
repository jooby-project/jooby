package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue515 {

  @Test
  public void shouldGenerateValidFileSet() throws Exception {
    List<String> styles = new AssetCompiler(conf("issue515.conf", "dev")).styles("search");
    assertEquals(Arrays.asList("/css/bulma.min.css", "/css/font-awesome.min.css",
        "/css/header.scss", "/css/main.scss", "/css/sprite.css", "/css/search.scss"), styles);
  }

  private Config conf(final String path, final String env) {
    return ConfigFactory.parseResources(path)
        .withValue("assets.env", ConfigValueFactory.fromAnyRef(env))
        .withValue("assets.charset", ConfigValueFactory.fromAnyRef("UTF-8"));
  }
}
