/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3508;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;

import java.util.Map;

public class App3508 extends Jooby {
  public App3508(Extension validator, boolean withProblemDetails) {

    if (withProblemDetails) {
      Config problemDetailsConfig = ConfigFactory.parseMap(
          Map.of("problem.details.enable", true,
              "problem.details.log4xxErrors", true)
      );

      getEnvironment()
          .setConfig(problemDetailsConfig.withFallback(getConfig()));
    }

    install(new JacksonModule());
    install(validator);

    mvc(new Controller3508_());
  }
}
