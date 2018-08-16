package io.jooby;

import javax.annotation.Nonnull;

public interface Parser {

  Parser NOT_ACCEPTABLE = new Parser() {
    @Override public <T> T parse(Context ctx, Reified<T> type) {
      throw new Err(StatusCode.NOT_ACCEPTABLE);
    }
  };

  default String contentType() {
   return "text/plain";
  }

  <T> T parse(Context ctx, Reified<T> type) throws Exception;
}
