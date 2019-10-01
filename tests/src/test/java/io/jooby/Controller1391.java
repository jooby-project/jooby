package io.jooby;

import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

import java.util.List;

@Path("/1391")
public class Controller1391 {

  public static class Data1391 {
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @POST
  public List<Data1391> post(List<Data1391> data) {
    return data;
  }
}
