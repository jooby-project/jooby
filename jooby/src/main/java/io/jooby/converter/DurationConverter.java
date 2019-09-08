package io.jooby.converter;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;

public class DurationConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == Duration.class;
  }

  @Override public Object convert(Class type, String value) {
    try {
      return Duration.parse(value);
    } catch (DateTimeParseException x) {
      try {
        long duration = ConfigFactory.empty().withValue("d", ConfigValueFactory.fromAnyRef(value))
            .getDuration("d", TimeUnit.MILLISECONDS);
        return Duration.ofMillis(duration);
      } catch (ConfigException ignored) {
        throw x;
      }
    }
  }
}
