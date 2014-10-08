package jooby.internal.routes;

import java.util.Map.Entry;
import java.util.stream.Collectors;

import jooby.MediaType;
import jooby.Request;
import jooby.Response;
import jooby.Router;
import jooby.Variant;

public class TraceRouter implements Router {

  @Override
  public void handle(final Request req, final Response res) throws Exception {
    if (res.committed()) {
      return;
    }

    String CRLF = "\n";
    StringBuilder buffer = new StringBuilder("TRACE ").append(req.path())
        .append(" ").append(req.protocol());

    for (Entry<String, Variant> entry : req.headers().entrySet()) {
      buffer.append(CRLF).append(entry.getKey()).append(": ")
          .append(entry.getValue().toList(String.class).stream().collect(Collectors.joining(", ")));
    }

    buffer.append(CRLF);

    res.type(MediaType.valueOf("message/http"));
    res.length(buffer.length());
    res.send(buffer.toString());
  }

}
