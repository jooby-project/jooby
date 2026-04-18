/**
 * GraphQL module on top of https://www.graphql-java.com.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * // required:
 * install(new Jackson2Module()); // or Jackson3Module, or AvajeJsonBModule, etc.
 *
 * install(new GrapQLModule(graphQL));
 *
 * }</pre>
 *
 * Module install a GET and POST route under <code>/graphql</code> path. Optionally, you can change
 * the route path by setting the <code>graphql.path</code> property in your application
 * configuration file.
 *
 * <p>NOTE: From 4.5.0 You must install a json module.
 *
 * @author edgar
 * @since 2.4.0
 */
@org.jspecify.annotations.NullMarked
package io.jooby.graphql;
