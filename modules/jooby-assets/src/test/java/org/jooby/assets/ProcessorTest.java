package org.jooby.assets;

import org.jooby.MediaType;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;

public class ProcessorTest extends AssetProcessor {

  {
    set("sourceMap", ImmutableMap.of("type", "inline", "sources", true));
  }

  @Override
  public boolean matches(final MediaType type) {
    return type.matches(MediaType.js);
  }

  @Override
  public String process(final String filename, final String source, final Config conf,
      final ClassLoader loader) throws Exception {
    return name() + ":" + source;
  }

}
