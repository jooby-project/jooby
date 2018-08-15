package io.jooby;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public interface Server {

  @Nonnull Server mode(@Nonnull Mode mode);

  @Nonnull Server port(int port);

  @Nonnull Server start(@Nonnull Router router);

  @Nonnull Server stop();

  @Nonnull Server tmpdir(@Nonnull Path tmpdir);
}
