package jooby;

import java.util.Optional;

public interface AfterRoute {

  void handle(Request request, Response response, Optional<Exception> cause) throws Exception;

}
