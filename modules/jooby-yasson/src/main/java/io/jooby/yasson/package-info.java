/**
 * JSON module using JSON-B: https://github.com/eclipse-ee4j/jsonb-api.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new YassonModule());
 *
 *   get("/", ctx -> {
 *     MyObject myObject = ...;
 *     // send json
 *     return myObject;
 *   });
 *
 *   post("/", ctx -> {
 *     // read json
 *     MyObject myObject = ctx.body(MyObject.class);
 *     // send json
 *     return myObject;
 *   });
 * }
 * }</pre>
 *
 * For body decoding the client must specify the <code>Content-Type</code> header set to <code>
 * application/json</code>.
 *
 * <p>You can retrieve the {@link jakarta.json.bind.Jsonb} object via require call:
 *
 * <pre>{@code
 * {
 *
 *   Jsonb jsonb = require(Jsonb.class);
 *
 * }
 * }</pre>
 *
 * Complete documentation is available at: https://jooby.io/modules/jsonb.
 */
@org.jspecify.annotations.NullMarked
package io.jooby.yasson;
