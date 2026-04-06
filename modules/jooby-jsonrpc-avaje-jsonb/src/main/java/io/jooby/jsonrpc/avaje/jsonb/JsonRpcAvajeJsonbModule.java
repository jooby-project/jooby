/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc.avaje.jsonb;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.avaje.jsonb.Jsonb;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.internal.jsonrpc.avaje.jsonb.AvajeJsonRpcParser;
import io.jooby.jsonrpc.JsonRpcParser;

public class JsonRpcAvajeJsonbModule implements Extension {
  @Override
  public void install(@NonNull Jooby application) throws Exception {
    // JSON-RPC
    application
        .getServices()
        .put(JsonRpcParser.class, new AvajeJsonRpcParser(application.require(Jsonb.class)));
  }
}
