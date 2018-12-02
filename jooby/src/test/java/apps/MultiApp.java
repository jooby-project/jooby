package apps;

import io.jooby.App;
import io.jooby.Context;

public class MultiApp {

  public static class Foo extends App {
    {
      get("/foo", Context::path);
    }
  }

  public static class Bar extends App {
    {
      basePath("/some");
      get("/bar", Context::path);
    }
  }

  public static void main(String[] args) {
//    Server server = new Netty();
//
//    server.deploy(new Foo());
//    server.deploy(new Bar());
//
//    server.start();
  }
}
