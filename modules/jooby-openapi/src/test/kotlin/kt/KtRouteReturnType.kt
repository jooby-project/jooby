package kt

import examples.ABean
import examples.BBean
import examples.Bean
import io.jooby.Kooby
import io.jooby.StatusCode
import io.jooby.exception.StatusCodeException
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture


class KtRouteReturnType : Kooby({
//  fun something(): String {
//    throw StatusCodeException(StatusCode.UNAUTHORIZED, "test")
//  }
//
//  fun someGeneric(): List<String> {
//    return ArrayList()
//  }
//
//  @Throws(IllegalAccessException::class)
//  fun throwsIAE(): String {
//    throw IllegalAccessException("no-access")
//  }
//
//  path("/literal") {
//    get("/1") {
//      "str"
//    }
//    get("/2") { 57 }
//    get("/3") {}
//    get("/4") { true }
//  }
//  path("/call") {
//    get("/1") { KtRouteReturnType() }
//    get("/2") { Statics.computeStatic() }
//    get("/3") {
//      val instance = Instance()
//      instance.newInstance(0, "c")
//    }
//    get("/4") { something() }
//    get("/5") { throwsIAE() }
//    get("/6") { someGeneric() }
//  }
  path("/generic") {
    get("/1") {
      CompletableFuture.supplyAsync { ctx.query("n").intValue(1) }
          .thenApply { x -> x * 2 }
          .whenComplete { v, x -> ctx.render(v) }
    }
//    get("/2") {
//      val future = CompletableFuture.completedFuture(0)
//          .thenApply { x -> x * 2 }
//          .thenApply { x -> x * 3 }
//      future
//    }
//    get("/3") {
//      CompletableFuture
//          .supplyAsync { "foo" }
//    }
//    get("/4") {
//      val callable = Callable { Byte.MIN_VALUE }
//      callable
//    }
  }
//  path("/localvar") {
//    get("/1") {
//      val q = ctx.query("q").value()
//      q
//    }
//    get("/2") {
//      val q = ctx.query("q").intValue()
//      q
//    }
//    get("/3") {
//      val values = ctx.path("v").toList().toTypedArray()
//      values
//    }
//    get("/4") {
//      val values = floatArrayOf(ctx.query("f1").floatValue(), ctx.query("f2").floatValue())
//      values
//    }
//  }
//  path("/complexType") {
//    get("/1") { ctx.query("q").toList() }
//    get("/2") {
//      val q = ctx.query("q").toList()
//      q
//    }
//    get("/3") {
//      val values: List<List<String>> = ArrayList()
//      values.stream().filter { obj: List<String>? -> Objects.nonNull(obj) }.toArray()
//      values
//    }
//  }
//  path("/multipleTypes") {
//    get("/1") {
//      if (ctx.isInIoThread) {
//        return@get ArrayList<String>()
//      } else {
//        return@get LinkedList<String>()
//      }
//    }
//    get("/2") {
//      if (ctx.isInIoThread) {
//        return@get ABean()
//      } else {
//        return@get BBean()
//      }
//    }
//    get("/3") {
//      val user: Bean
//      if (ctx.isInIoThread) {
//        user = ABean()
//        return@get user
//      } else {
//        user = BBean()
//        return@get user
//      }
//    }
//  }
//  path("/array") {
//    get("/1") { booleanArrayOf(true, false, false) }
//    get("/2") {
//      val list = listOf<KtRouteReturnType>()
//      list
//    }
//    get("/3") { IntArray(0) }
//    get("/4") { arrayOf("foo", "bar") }
//  }
}) {
//  internal object Statics {
//    fun computeStatic(): String {
//      return "static"
//    }
//  }
//
//  internal class Instance {
//    fun newInstance(x: Int, v: String?): String {
//      return "static"
//    }
//  }


}
