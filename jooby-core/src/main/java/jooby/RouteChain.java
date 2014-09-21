package jooby;

public interface RouteChain {

  void next(Request request, Response response) throws Exception;

}
