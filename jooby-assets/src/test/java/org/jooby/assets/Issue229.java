package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.typesafe.config.ConfigFactory;

public class Issue229 {

  @Test
  public void wrongVars() throws Exception {
    AssetCompiler compiler = new AssetCompiler(ConfigFactory.parseResources("issue.229.conf"));
    Set<String> vars = new HashSet<>();
    compiler.keySet().forEach(asset -> {
      vars.add(asset);
    });

    assertEquals(Sets.newHashSet("form", "base"), vars);
  }
}
