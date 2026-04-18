/**
 * JSON module using Gson: https://github.com/google/gson.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new GsonModule());
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
 * <p>You can retrieve the {@link com.google.gson.Gson} object via require call:
 *
 * <pre>{@code
 * {
 *
 *   Gson gson = require(Gson.class);
 *
 * }
 * }</pre>
 *
 * Complete documentation is available at: https://jooby.io/modules/gson.
 *
 * @author edgar
 * @since 2.7.2
 */
@org.jspecify.annotations.NullMarked
package io.jooby.gson;
