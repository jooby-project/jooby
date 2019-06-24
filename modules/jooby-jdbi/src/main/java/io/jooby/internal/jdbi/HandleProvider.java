package io.jooby.internal.jdbi;

import io.jooby.RequestScope;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import javax.inject.Provider;

public class HandleProvider implements Provider<Handle> {
  private Jdbi jdbi;

  public HandleProvider(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override public Handle get() {
    Handle handle = RequestScope.get(jdbi);
    if (handle == null) {
      handle = jdbi.open();
    }
    return handle;
  }
}
