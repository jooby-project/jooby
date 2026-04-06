/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3864;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.annotation.jsonrpc.JsonRpc;

@JsonRpc
public class NullSupport {

  public String nullableInt(Integer nullInt) {
    return "1";
  }

  public String requiredInt(int nonnullInt) {
    return "1";
  }

  public String requiredString(@NonNull String nonnullStr) {
    return "1";
  }
}
