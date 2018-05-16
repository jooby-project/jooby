package kt

import org.jooby.Kooby

class App1070 : Kooby({
    /** Top */
    path("/v2/currencies") {
        /** SubTop1 */
        path("/rates") {
            /**
             * yadayada.
             *
             * @return OK
             */
            get {
                // do something
                ""
            }

            /**
             * yadayada2.
             *
             * @return ```200``` OK
             */
            delete {
                // do something
                ""
            }
        }
        /** SubTop2 */
        path("/:isoCode") {
            /**
             * Gets the currency for a given ISO code.
             *
             * @return ....
             */
            get {
                // do something
                ""
            }
        }
    }
})
