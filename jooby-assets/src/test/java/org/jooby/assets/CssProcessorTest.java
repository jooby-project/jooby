package org.jooby.assets;

import org.jooby.MediaType;

import com.typesafe.config.Config;

public class CssProcessorTest extends AssetProcessor {

  @Override
  public boolean matches(final MediaType type) {
    return type.matches(MediaType.css);
  }

  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    return name() + ":" + source;
  }

}
