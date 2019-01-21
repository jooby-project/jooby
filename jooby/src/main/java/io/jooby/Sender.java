package io.jooby;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface Sender {

  interface Callback {
    void onComplete(Context ctx, Throwable cause);
  }

  default Sender sendString(@Nonnull String data, @Nonnull Callback callback) {
    return sendString(data, StandardCharsets.UTF_8, callback);
  }

  default Sender sendString(@Nonnull String data, Charset charset, @Nonnull Callback callback) {
    return sendBytes(data.getBytes(charset), callback);
  }

  Sender sendBytes(@Nonnull byte[] data, @Nonnull Callback callback);

  void close();
}
