package org.jooby.assets;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jooby.test.ServerFeature;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public abstract class AssetsBase extends ServerFeature {

  public List<Object> list(final Object... args) {
    return Arrays.asList(args);
  }

  public Map<String, Object> map(final Object... args) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    for (int i = 0; i < args.length; i += 2) {
      builder.put(args[i].toString(), args[i + 1]);
    }
    return builder.build();
  }

  public Config assets(final String dist, final Object... args) {
    return ConfigFactory.empty()
        .withValue("assets", ConfigValueFactory.fromMap(map(args)));
  }
}
