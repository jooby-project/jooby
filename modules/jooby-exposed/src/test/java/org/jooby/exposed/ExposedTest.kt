package org.jooby.exposed

import com.google.inject.Binder
import com.google.inject.Key
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.name.Names
import com.typesafe.config.Config
import org.easymock.EasyMock
import org.jetbrains.exposed.sql.Database
import org.jooby.Env
import org.jooby.test.MockUnit
import org.junit.Test
import java.util.*
import java.util.function.Consumer
import javax.sql.DataSource

class ExposedTest {

  @Test
  fun defaults() {
    val dbkey = Key.get(Database::class.java, Names.named("db"))
    val blocks = arrayOf(MockUnit.Block { unit ->
      Exposed().configure(unit.get(Env::class.java), unit.get(Config::class.java), unit.get(Binder::class.java))
    }, MockUnit.Block { unit ->
      val binder = unit.captured(Consumer::class.java)[0] as Consumer<Key<Database>>
      binder.accept(dbkey)
    })

    MockUnit(Env::class.java, Binder::class.java, Config::class.java, DataSource::class.java)
        .expect(dataSource("db"))
        .expect(serviceKey("db"))
        .expect(bind(dbkey))
        .run(*blocks)
  }

  @Test(expected = NoSuchElementException::class)
  fun noDataSource() {
    val blocks = arrayOf(MockUnit.Block { unit ->
      Exposed().configure(unit.get(Env::class.java), unit.get(Config::class.java), unit.get(Binder::class.java))
    })

    MockUnit(Env::class.java, Binder::class.java, Config::class.java, DataSource::class.java)
        .expect { unit ->
          val env = unit.get(Env::class.java)
          EasyMock.expect(env.get(Key.get(DataSource::class.java, Names.named("db")))).andReturn(Optional.empty())
        }
        .run(*blocks)
  }

  private fun bind(key: Key<Database>): MockUnit.Block {
    return MockUnit.Block { unit ->
      val lbb = unit.mock(LinkedBindingBuilder::class.java) as LinkedBindingBuilder<Database>
      lbb.toInstance(EasyMock.isA(Database::class.java))

      val binder = unit.get(Binder::class.java)
      EasyMock.expect(binder.bind(key)).andReturn(lbb)
    }
  }

  private fun serviceKey(name: String): MockUnit.Block {
    return MockUnit.Block { unit ->
      val skey = unit.mock(Env.ServiceKey::class.java)
      skey.generate(EasyMock.eq(Database::class.java), EasyMock.eq(name), unit.capture(Consumer::class.java) as Consumer<Key<Database>>?);

      val env = unit.get(Env::class.java)
      EasyMock.expect(env.serviceKey()).andReturn(skey)
    }
  }

  private fun dataSource(db: String?): MockUnit.Block {
    return MockUnit.Block { unit ->
      val ds = unit.get(DataSource::class.java)
      val env = unit.get(Env::class.java)
      val ods = if (db == null) Optional.empty() else Optional.of(ds)
      EasyMock.expect(env.get(Key.get(DataSource::class.java, Names.named(db)))).andReturn(ods)
    }
  }
}
