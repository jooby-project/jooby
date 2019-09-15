package examples;

import io.jooby.MyValue;
import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

public class MyValueRouter {

  @GET("/myvalue")
  public MyValue beanConverter(@QueryParam MyValue bean) {
    return bean;
  }
}
