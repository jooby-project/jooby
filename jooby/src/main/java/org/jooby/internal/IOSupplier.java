package org.jooby.internal;

import java.io.IOException;


public interface IOSupplier<IO> {

  IO get() throws IOException;

}
