/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import io.jooby.annotation.GET;

public class ArrayRoute {

  @GET("/mypath")
  public String[] controllerMethodStrings() {
    return new String[] {"/mypath1", "/mypath2"};
  }

  @GET("/mybeans")
  public Bean[] controllerMethodBeans() {
    return new Bean[] {new Bean(1), new Bean(2)};
  }

  @GET("/mymultibeans")
  public Bean[][] controllerMethodMultiBeans() {
    return new Bean[][] {{new Bean(1)}, {new Bean(2)}};
  }
}
