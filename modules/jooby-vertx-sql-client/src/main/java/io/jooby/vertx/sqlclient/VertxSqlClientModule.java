/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx.sqlclient;

import com.typesafe.config.ConfigValueType;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.*;

public abstract class VertxSqlClientModule implements Extension {
  private final String name;

  public VertxSqlClientModule(@NonNull String name) {
    this.name = name;
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    var registry = application.getServices();
    var config = application.getConfig();
    var configOptions = config.getValue(name);
    SqlConnectOptions options;
    if (configOptions.valueType() == ConfigValueType.STRING) {
      options = fromUri(config.getString(name));
    } else {
      options = fromMap(new JsonObject(config.getObject(name).unwrapped()));
    }

    var client = newBuilder().connectingTo(options).using(registry.require(Vertx.class)).build();

    registry.put(ServiceKey.key(SqlClient.class, name), client);
    registry.putIfAbsent(SqlClient.class, client);
    if (client instanceof Pool pool) {
      registry.put(ServiceKey.key(Pool.class, name), pool);
      registry.putIfAbsent(Pool.class, pool);
    }
    // Shutdown
    application.onStop(() -> client.close().await());
  }

  protected abstract SqlConnectOptions fromMap(JsonObject config);

  protected abstract SqlConnectOptions fromUri(String uri);

  protected abstract ClientBuilder<? extends SqlClient> newBuilder();
}
