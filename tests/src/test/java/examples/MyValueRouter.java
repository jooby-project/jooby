package examples;

import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;
import io.jooby.test.MyValue;

public class MyValueRouter {

  @GET("/myvalue")
  public MyValue beanConverter(@QueryParam MyValue bean) {
    return bean;
  }
}
