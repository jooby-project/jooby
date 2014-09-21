package jooby;

public interface Filter {

  void handle(Request request, Response response, RouteChain chain) throws Exception;

}
