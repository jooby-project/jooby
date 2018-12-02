package io.jooby;

import javax.annotation.Nonnull;

public interface Parser {

  Parser NOT_ACCEPTABLE = new Parser() {
    @Override public <T> T parse(Context ctx, Reified<T> type) {
      throw new Err(StatusCode.NOT_ACCEPTABLE);
    }
  };

  @Nonnull default String contentType() {
   return MediaType.TEXT;
  }

  @Nonnull <T> T parse(@Nonnull Context ctx, @Nonnull Reified<T> type) throws Exception;
}
