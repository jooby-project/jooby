/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.graphql;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.Route;
import io.jooby.ServiceRegistry;
import io.jooby.SneakyThrows;
import io.jooby.internal.graphql.BlockingGraphQLHandler;
import io.jooby.internal.graphql.GraphQLHandler;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL module on top of https://www.graphql-java.com.
 *
 * Usage:
 *
 * <pre>{@code
 *
 *   install(new GrapQLModule(graphQL));
 *
 * }</pre>
 *
 * Module install a GET and POST route under <code>/graphql</code> path. Optionally, you can change
 * the route path by setting the <code>graphql.path</code> property in your application
 * configuration file.
 *
 * @author edgar
 * @since 2.4.0
 */
public class GraphQLModule implements Extension {

  private GraphQL graphQL;

  private boolean async = true;

  private boolean supportGetRequest;

  /**
   * Creates a new GraphQL module.
   *
   * @param graphQL GraphQL instance.
   */
  public GraphQLModule(@Nonnull GraphQL graphQL) {
    this.graphQL = graphQL;
  }

  /**
   * Creates a new GraphQL module.
   *
   * @param schema GraphQL schema.
   */
  public GraphQLModule(@Nonnull GraphQLSchema schema) {
    this(GraphQL.newGraphQL(schema).build());
  }

  /**
   * Creates a new GraphQL module.
   *
   * @param path Classpath location for schema file. Usually <code>schema.graphql</code>.
   * @param wiring Runtime wiring to build a GraphQL instance.
   */
  public GraphQLModule(@Nonnull String path, @Nonnull RuntimeWiring wiring) {
    this(newSchema(reader(GraphQLModule.class.getClassLoader(), path), wiring));
  }

  /**
   * Creates a new GraphQL module.
   *
   * @param path File system location for schema file. Usually <code>schema.graphqls</code>.
   * @param wiring Runtime wiring to build a GraphQL instance.
   */
  public GraphQLModule(@Nonnull Path path, @Nonnull RuntimeWiring wiring) {
    this(newSchema(fileReader(path), wiring));
  }

  /**
   * Creates a new GraphQL module. Load schema from default classpath location:
   * <code>schema.graphql</code>.
   *
   * @param wiring Runtime wiring to build a GraphQL instance.
   */
  public GraphQLModule(@Nonnull RuntimeWiring wiring) {
    this("schema.graphql", wiring);
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    String graphqlPath = application.getEnvironment().getProperty("graphql.path", "/graphql");

    GraphQLHandler handler = async
        ? new GraphQLHandler(graphQL)
        : new BlockingGraphQLHandler(graphQL);

    if (supportGetRequest) {
      executionMode(application.get(graphqlPath, handler), async);
    }

    executionMode(application.post(graphqlPath, handler), async);

    ServiceRegistry services = application.getServices();
    services.put(GraphQL.class, graphQL);
  }

  /**
   * Whether GraphQL query run in async mode or not. Default is <code>true</code>.
   *
   * @param async True to use async execution.
   * @return This module.
   */
  public GraphQLModule setAsync(boolean async) {
    this.async = async;
    return this;
  }

  /**
   * Whether to support HTTP GET request.
   *
   * @param supportGetRequest True to support HTTP GET request.
   * @return This module.
   */
  public GraphQLModule setSupportGetRequest(boolean supportGetRequest) {
    this.supportGetRequest = supportGetRequest;
    return this;
  }

  private void executionMode(Route route, boolean async) {
    if (async) {
      route.setReturnType(CompletableFuture.class);
    } else {
      route.setReturnType(Map.class);
    }
  }

  private static Reader fileReader(Path path) {
    try {
      return Files.newBufferedReader(path);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private static Reader reader(ClassLoader loader, @Nonnull String path) {
    try {
      return new InputStreamReader(
          loader.getResourceAsStream(path.startsWith("/") ? path.substring(0) : path),
          StandardCharsets.UTF_8);
    } catch (NullPointerException x) {
      throw SneakyThrows.propagate(new FileNotFoundException(path));
    }
  }

  private static GraphQLSchema newSchema(Reader input, RuntimeWiring wiring) {
    TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(input);
    SchemaGenerator schemaGenerator = new SchemaGenerator();
    return schemaGenerator.makeExecutableSchema(typeRegistry, wiring);
  }
}
