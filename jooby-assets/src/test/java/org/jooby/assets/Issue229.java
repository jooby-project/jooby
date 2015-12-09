package org.jooby.assets;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class Issue229 {

  @Test
  public void wrongVars() throws Exception {
    AssetCompiler compiler = new AssetCompiler(ConfigFactory.parseResources("issue.229.conf"));
    compiler.keySet().forEach(asset -> {
      System.out.println(asset);
    });
  }
}
