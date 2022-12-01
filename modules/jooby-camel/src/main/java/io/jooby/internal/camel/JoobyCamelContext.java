/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.camel;

import java.util.Map;
import java.util.Properties;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.Injector;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.DefaultRegistry;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import io.jooby.Jooby;

public class JoobyCamelContext extends DefaultCamelContext {

  private Jooby application;

  public JoobyCamelContext(Jooby application) {
    super(false);
    this.application = application;
  }

  @Override
  public DataFormat resolveDataFormat(String name) {
    return super.resolveDataFormat(name);
  }

  @Override
  protected Injector createInjector() {
    return new JoobyInjector(this, application);
  }

  @Override
  protected Registry createRegistry() {
    return new DefaultRegistry(new JoobyBeanRepository(application.getServices()));
  }

  @Override
  protected PropertiesComponent createPropertiesComponent() {
    PropertiesComponent properties =
        new org.apache.camel.component.properties.PropertiesComponent();

    // Dump all camel.*
    properties.setInitialProperties(configureProperties(application));

    // All others via lookup
    properties.addPropertiesSource(new JoobyPropertiesSource(application.getConfig()));

    return properties;
  }

  private static Properties configureProperties(Jooby application) {
    Properties properties = new Properties();
    properties.setProperty("camel.main.name", application.getName());
    properties.setProperty("camel.main.autoConfigurationLogSummary", "false");

    Config config = application.getConfig();
    if (config.hasPath("camel")) {
      for (Map.Entry<String, ConfigValue> prop : config.getConfig("camel").entrySet()) {
        properties.setProperty("camel." + prop.getKey(), prop.getValue().unwrapped().toString());
      }
    }

    return properties;
  }
}
