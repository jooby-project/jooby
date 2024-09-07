/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
/**
 * Avaje Validator Module.
 */
module io.jooby.avaje.validator {
    exports io.jooby.avaje.validator;

    requires transitive io.jooby;
    requires static com.github.spotbugs.annotations;
    requires typesafe.config;
    requires transitive io.avaje.validation;
    requires transitive io.jooby.validation;
}
