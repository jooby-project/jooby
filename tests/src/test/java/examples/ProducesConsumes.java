package examples;

import io.jooby.annotations.GET;

public class ProducesConsumes {

  @GET(path = "/produces", produces = {"application/json", "application/xml"})
  public Message produces() {
    return new Message("MVC");
  }

  @GET(path = "/consumes", consumes = {"application/json", "application/xml"})
  public String consumes(Message body) {
    return body.toString();
  }
}
