package org.jooby.assets;

import java.util.Arrays;
import java.util.List;

import com.typesafe.config.Config;

public class AggregatorTest extends AssetAggregator {

  @Override
  public List<String> fileset() {
    return Arrays.asList(get("output").toString());
  }

  @Override
  public void run(final Config conf) throws Exception {
  }


}
