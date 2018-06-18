package org.jooby.flyway;

import static com.google.common.collect.ImmutableMap.of;
import com.typesafe.config.ConfigFactory;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.Map;

public class Issue1124 {

  @Test
  public void checkValidFlywayProperty() {
    Map conf = of("placeholders", of("tables", "foo"));
    Map<String, Object> expected = of("placeholders", of("tables", "foo"));
    assertEquals(expected, Flywaydb.flyway(ConfigFactory.parseMap(conf)).root().unwrapped());
  }

  @Test
  public void filterResidualProperties() {
    Map conf = of("placeholders", of("tables", "foo"), "maxPoolSize", 1);
    Map<String, Object> expected = of("placeholders", of("tables", "foo"));
    assertEquals(expected, Flywaydb.flyway(ConfigFactory.parseMap(conf)).root().unwrapped());
  }
}
