/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i3490

import io.jooby.Context
import io.jooby.annotation.*
import java.io.IOException
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.delay

@Path("/")
class C3490 {
  @GET
  suspend fun sayHi(@QueryParam n: String?, @QueryParam bit: String?): String {
    return Thread.currentThread().name + ": " + n + ": " + bit
  }

  @GET("/bye")
  suspend fun sayGoodBye(@QueryParam n: String): String {
    val from = Thread.currentThread().name
    delay(100)
    return from + ":" + Thread.currentThread().name + ": " + n
  }

  @GET("/completable")
  fun completable(): CompletableFuture<String> {
    val from = Thread.currentThread().name
    return CompletableFuture.supplyAsync { from + ": " + Thread.currentThread().name + ": Async" }
  }

  @Throws(IOException::class)
  @GET("/fo\"o")
  fun foo(ctx: Context) {
    ctx.send("fff")
  }

  @GET("/bean") fun bean(@BindParam bean: Bean3490) = bean

  @GET("/gen") fun gen(@BindParam bean: Bean3490): List<String> = listOf()

  @GET("/gene") fun <E> genE(@BindParam bean: Bean3490): List<E> = listOf()

  @GET("/genlist") fun <E> genE(@QueryParam bean: List<E>): List<E> = listOf()

  @GET("/context") fun contextAttr(@ContextParam attributes: Map<String, Any>) = attributes

  @GET("/box") fun box(@QueryParam box: Box3490<Int>) = box

  @GET("/list")
  fun box(@QueryParam id: Int?): Box3490<List<Bean3490>> =
    Box3490(listOf(Bean3490(id?.toString() ?: "none")))
}

data class Bean3490(val value: String) {
  override fun toString(): String {
    return value
  }

  companion object {
    @JvmStatic fun of(ctx: Context) = Bean3490(ctx.query("value").toString())
  }
}

data class Box3490<T>(val value: T)
