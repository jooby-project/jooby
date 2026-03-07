/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller class or a specific route method for tRPC TypeScript generation.
 *
 * <p>When applied to a class, it defines a namespace for the tRPC router. All tRPC-annotated
 * methods within the class will be grouped under this namespace in the generated TypeScript {@code
 * AppRouter}.
 *
 * <p><b>Defining Procedures:</b>
 *
 * <p>There are two ways to expose a method as a tRPC procedure:
 *
 * <ul>
 *   <li><b>Explicit tRPC Annotations:</b> Use {@link Trpc.Query} for read-only operations (mapped
 *       to HTTP GET) and {@link Trpc.Mutation} for state-changing operations (mapped to HTTP POST).
 *   <li><b>Hybrid HTTP Annotations:</b> Combine the base {@code @Trpc} annotation with standard
 *       HTTP annotations. A {@code @GET} annotation maps to a query, while {@code @POST},
 *       {@code @PUT}, {@code @PATCH}, and {@code @DELETE} map to a mutation.
 * </ul>
 *
 * <p><b>Network Payloads:</b>
 *
 * <p>Because tRPC natively supports only a single input payload, Java methods with multiple
 * parameters will automatically require a JSON array (Tuple) from the frontend client. Framework
 * parameters like {@code io.jooby.Context} are ignored during payload calculation.
 *
 * <p><b>Example:</b>
 *
 * <pre>{@code
 * @Trpc("movies") // Defines the 'movies' namespace
 * public class MovieService {
 *
 * @Trpc.Query // Becomes 'movies.list' query
 * public List<Movie> list() { ... }
 *
 * @Trpc // Hybrid approach: Becomes 'movies.delete' mutation
 * @DELETE
 * public void delete(int id) { ... }
 * }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Trpc {

  /**
   * Marks a method as a tRPC mutation.
   *
   * <p>Mutations are used for creating, updating, or deleting data. Under the hood, Jooby will
   * automatically expose this method as an HTTP POST route on the {@code /trpc} endpoint.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface Mutation {
    /**
     * Custom name for the tRPC mutation.
     *
     * <p>This overrides the generated procedure name in the TypeScript router.
     *
     * @return The custom procedure name. Empty by default, which means the generator will use the
     *     Java method name.
     */
    String value() default "";
  }

  /**
   * Marks a method as a tRPC query.
   *
   * <p>Queries are strictly used for fetching data. Under the hood, Jooby will automatically expose
   * this method as an HTTP GET route on the {@code /trpc} endpoint.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface Query {
    /**
     * Custom name for the tRPC query.
     *
     * <p>This overrides the generated procedure name in the TypeScript router.
     *
     * @return The custom procedure name. Empty by default, which means the generator will use the
     *     Java method name.
     */
    String value() default "";
  }

  /**
   * Custom name for the tRPC procedure or namespace.
   *
   * <p>If applied to a method, this overrides the generated procedure name. If applied to a class,
   * this overrides the generated namespace in the {@code AppRouter}.
   *
   * @return The custom procedure or namespace name. Empty by default, which means the generator
   *     will use the Java method or class name.
   */
  String value() default "";
}
