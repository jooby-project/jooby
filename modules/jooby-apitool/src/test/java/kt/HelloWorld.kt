package kt

import org.jooby.*
import parser.Foo

class HelloWorld : Kooby({
    /**
     * Get with default arg
     */
    get {
        "Hello Kotlin"
    }

    /**
     * Get on /kstr
     *
     * @param foo Foo param.
     */
    get("/kstr") {
        val bar = param<String>("foo")
        bar
    }

    get("/int") {
        val value = param<Int>("ivar")
        value
    }

    get("/foo") {
        val body = body<Foo>()
        body
    }

    get("/java") {
        val bar = body().to(Foo::class)
        bar
    }

    get("/defpara") {
        val bar = param("foo", "sql").value("bar")
        bar
    }

    /**
     * List of a, b,c
     *
     * @return [a, b, c]
     */
    get("/list") {
        val list = listOf("a", "b", "c")
        print(list)
        list
    }

    get("/data1") {
        MyData("foo")
    }

    post("/data2") {
        val data = body(MyData::class.java)
        data
    }

})

fun main(args: Array<String>) {
    run(::HelloWorld, *args)
}

data class MyData(val name: String)
