/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.graphiql;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;

/**
 * GraphiQL module: https://github.com/graphql/graphiql.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * install(new GraphiQLModule());
 *
 * }</pre>
 *
 * Module install a GET route under <code>/graphql</code> path. Optionally, you can change the route
 * path by setting the <code>graphql.path</code> property in your application configuration file.
 *
 * @author edgar
 * @since 2.4.0
 */
public class GraphiQLModule implements Extension {

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    var contextPath = application.getContextPath();
    if (contextPath.equals("/")) {
      contextPath = "";
    }
    String graphqlPath = application.getEnvironment().getProperty("graphql.path", "/graphql");
    String index = GraphiqlPage.index(contextPath + graphqlPath);
    application.get(graphqlPath, ctx -> ctx.setResponseType(MediaType.html).send(index));
  }
}
