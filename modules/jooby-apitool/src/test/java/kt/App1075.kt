package kt

import org.jooby.Kooby

class App1075 : Kooby({
    path("/v2"){
        path("/orders") {
            get {
                listOf("order-1", "order-2")
            }
        }
        path("/products") {
            get {
                listOf("product-1", "product-2")
            }
        }
    }
})