/**
 * JSON module using Jackson: https://jooby.io/modules/jackson2.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new Jackson2Module());
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
 * <p>You can retrieve the {@link com.fasterxml.jackson.databind.ObjectMapper} via require call:
 *
 * <pre>{@code
 * {
 *
 *   ObjectMapper mapper = require(ObjectMapper.class);
 *
 * }
 * }</pre>
 *
 * Complete documentation is available at: https://jooby.io/modules/jackson2.
 *
 * @author edgar
 * @since 2.0.0
 */
@org.jspecify.annotations.NullMarked
package io.jooby.jackson;
