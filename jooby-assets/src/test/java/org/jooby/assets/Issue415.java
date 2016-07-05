package org.jooby.assets;

import java.io.File;
import java.nio.file.Paths;

import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue415 {

  @Test
  public void shouldNotFailOnEmptyCss() throws Exception {
    File dir = Paths.get("target", "public").toFile();
    new AssetCompiler(conf("issue415.conf", "dev")).build("dev", dir);
  }

  private Config conf(final String path, final String env) {
    return ConfigFactory.parseResources(path)
        .withValue("assets.env", ConfigValueFactory.fromAnyRef(env))
        .withValue("assets.charset", ConfigValueFactory.fromAnyRef("UTF-8"));
  }
}
