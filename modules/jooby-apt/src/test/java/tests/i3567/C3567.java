/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3567;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class C3567 {

  private final Config config;

  @Inject
  public C3567(Config config) {
    this.config = config;
  }

  @GET("/3567")
  public String webMethod(@QueryParam String property) {
    return config.getString(property);
  }
}
