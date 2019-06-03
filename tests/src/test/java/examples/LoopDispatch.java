package examples;

import io.jooby.annotations.Dispatch;
import io.jooby.annotations.GET;

public class LoopDispatch {
  @GET("/")
  public String classlevel() {
    return Thread.currentThread().getName();
  }

  @GET("/method")
  @Dispatch("single")
  public String methodlevel() {
    return Thread.currentThread().getName();
  }
}
