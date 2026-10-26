/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rainbowgum;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import io.jstach.rainbowgum.LogProperties;

/** Reads native HOCON objects, including quoted logger names, lists and maps. */
final class ConfigProperties extends LogProperties.AbstractNested<ConfigValue> {
  private final Supplier<Config> config;

  ConfigProperties(Supplier<Config> config) {
    this.config = config;
  }

  @Override
  protected ConfigValue doRootNodeOrNull() {
    return config.get().root();
  }

  @Override
  protected @Nullable ConfigValue doMemberNodeOrNull(ConfigValue node, String name) {
    return node instanceof ConfigObject object ? object.get(name) : null;
  }

  @Override
  protected @Nullable String doValueNodeOrNull(ConfigValue node) {
    return switch (node.valueType()) {
      case STRING, NUMBER, BOOLEAN -> String.valueOf(node.unwrapped());
      default -> null;
    };
  }

  @Override
  protected @Nullable List<String> doListNodeOrNull(ConfigValue node) {
    if (!(node instanceof ConfigList list)) {
      return null;
    }
    return list.stream().map(this::scalar).toList();
  }

  @Override
  protected @Nullable Map<String, String> doMapNodeOrNull(ConfigValue node) {
    if (!(node instanceof ConfigObject object)) {
      return null;
    }
    Map<String, String> result = new LinkedHashMap<>();
    object.forEach(
        (key, value) -> {
          if (value.valueType() != ConfigValueType.NULL) {
            result.put(key, scalar(value));
          }
        });
    return result;
  }

  private String scalar(ConfigValue value) {
    var scalar = doValueNodeOrNull(value);
    if (scalar == null) {
      throw new IllegalArgumentException(
          "Expected a scalar logging value at " + value.origin().description());
    }
    return scalar;
  }

  @Override
  public int order() {
    return 300;
  }

  @Override
  public String description(String key) {
    return "Jooby configuration[" + key + "]";
  }
}
