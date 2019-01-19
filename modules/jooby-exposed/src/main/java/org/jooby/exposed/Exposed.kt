package org.jooby.exposed

import com.google.inject.Binder
import com.google.inject.Key
import com.google.inject.name.Names
import com.typesafe.config.Config
import org.jetbrains.exposed.sql.Database
import org.jooby.Env
import org.jooby.Jooby
import java.util.NoSuchElementException
import javax.sql.DataSource

/**
 * <h1>exposed</h1>
 * <p>
 *  <a href="https://github.com/JetBrains/Exposed">Exposed</a> is a prototype for a lightweight SQL
 *  library written over JDBC driver for Kotlin language
 * </p>
 *
 * <p>
 * This module depends on {@link org.jooby.jdbc.Jdbc} module, make sure you read the doc of the
 * {@link org.jooby.jdbc.Jdbc} module.
 * </p>
 *
 * <h2>exports</h2>
 * <ul>
 * <li>One or more Database objects</li>
 * </ul>
 *
 * <h2>usage</h2>
 * <pre>{@code
 * {
 *   use(new Jdbc());
 *
 *   use(new Exposed());
 *
 *   get("/db") {
 *     val db = require(Database::class)
 *     transaction (db) {
 *       // Work with db...
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h2>multiple databases</h2>
 *
 * <pre>{@code
 * {
 *   use(new Jdbc("db1"));
 *
 *   use(new Jdbc("db2"));
 *
 *   use(new Exposed("db1"));
 *
 *   use(new Exposed("db2"));
 *
 *   get("/db") {
 *     val db1 = require("db1", Database::class)
 *     // Work with db1...
 *
 *     val db2 = require("db2", Database::class)
 *     // Work with db2...
 *   }
 * }
 * }</pre>
 *
 * @author edgar
 */
class Exposed(val name: String = "db") : Jooby.Module {
  override fun configure(env: Env, conf: Config, binder: Binder) {
    val dskey = Key.get(DataSource::class.java, Names.named(name))
    val ds = env.get(dskey)
        .orElseThrow { NoSuchElementException("DataSource missing: $dskey") }
    val db = Database.connect(ds)
    env.serviceKey().generate(Database::class.java, name) { key -> binder.bind(key).toInstance(db) }
  }
}
