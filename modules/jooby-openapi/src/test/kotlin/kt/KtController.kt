package kt

import examples.ABean
import kotlinx.coroutines.delay
import java.util.concurrent.CompletableFuture
import javax.inject.Named
import javax.ws.rs.*

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

  @DELETE
  @Path("/unit")
  fun doUnit() {
  }

  @GET
  @Path("/doMap")
  fun doMap(): Map<String, Any> {
    return mapOf()
  }

  @Path("/doParams")
  fun params(@QueryParam("I") i: Int,
             @QueryParam("oI") oi: Int?,
             @QueryParam("q") q: String,
             @QueryParam("nullq") nullq: String?): ABean {
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
  fun httpNames(@HeaderParam("Last-Modified-Since") lastModifiedSince: String,
                @Named("x-search") @io.jooby.annotations.QueryParam q: String): String {
    return lastModifiedSince
  }
}
