/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt

import examples.ABean
import jakarta.inject.Named
import jakarta.ws.rs.*
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.delay

class KtController {

  @GET
  fun doSomething(): String {
    return ""
  }

  @PUT
  @Path("/entity/{id}")
  fun replaceEntity(@PathParam("id") id: String): String {
    return id
  }

  @DELETE @Path("/unit") fun doUnit() {}

  @GET
  @Path("/doMap")
  fun doMap(): Map<String, Any> {
    return mapOf()
  }

  @Path("/doParams")
  fun params(
    @QueryParam("I") i: Int,
    @QueryParam("oI") oi: Int?,
    @QueryParam("q") q: String,
    @QueryParam("nullq") nullq: String?,
  ): ABean {
    println("i:$i oi: $oi q: $q nullq: $nullq")
    return ABean()
  }

  @GET
  @Path("/coroutine")
  suspend fun coroutine(): List<String> {
    delay(100)
    return listOf("...")
  }

  @GET
  @Path("/future")
  fun completableFuture(): CompletableFuture<String> {
    return CompletableFuture.completedFuture("...")
  }

  @GET
  @Path("/httpNames")
  fun httpNames(
    @HeaderParam("Last-Modified-Since") lastModifiedSince: String,
    @Named("x-search") @io.jooby.annotation.QueryParam q: String,
  ): String {
    println("lastModifiedSince:$lastModifiedSince q: $q")
    return lastModifiedSince
  }
}
