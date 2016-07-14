/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.couchbase;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.jooby.Env;
import org.jooby.Jooby.Module;
import org.jooby.Session;
import org.jooby.internal.couchbase.AsyncDatastoreImpl;
import org.jooby.internal.couchbase.DatastoreImpl;
import org.jooby.internal.couchbase.IdGenerator;
import org.jooby.internal.couchbase.JacksonMapper;
import org.jooby.internal.couchbase.SetConverterHack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.deps.io.netty.util.internal.MessagePassingQueue.Supplier;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.ConnectionString;
import com.couchbase.client.java.CouchbaseAsyncCluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.document.EntityDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.repository.AsyncRepository;
import com.couchbase.client.java.repository.Repository;
import com.couchbase.client.java.repository.annotation.Field;
import com.couchbase.client.java.repository.mapping.EntityConverter;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javaslang.Function3;
import javaslang.control.Try;
import rx.Observable;

/**
 * <h1>couchbase</h1>
 * <p>
 * <a href="http://www.couchbase.com">Couchbase</a> is a NoSQL document database with a distributed
 * architecture for performance, scalability, and availability. It enables developers to build
 * applications easier and faster by leveraging the power of SQL with the flexibility of JSON.
 * </p>
 * <p>
 * This module provides <a href="http://www.couchbase.com">couchbase</a> access via
 * <a href="https://github.com/couchbase/couchbase-java-client">Java SDK</a>
 * </p>
 *
 * <h2>exports</h2>
 * <ul>
 * <li>{@link CouchbaseEnvironment}</li>
 * <li>{@link CouchbaseCluster}</li>
 * <li>{@link AsyncBucket}</li>
 * <li>{@link Bucket}</li>
 * <li>{@link AsyncDatastore}</li>
 * <li>{@link Datastore}</li>
 * <li>{@link AsyncRepository}</li>
 * <li>{@link Repository}</li>
 * <li>Optionally a couchbase {@link Session.Store}</li>
 * </ul>
 *
 * <h2>usage</h2>
 * <p>
 * Via couchbase connection string:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Couchbase("couchbase://locahost/beers"));
 *
 *   get("/", req -> {
 *     Bucket beers = req.require(Bucket.class);
 *     // do with beer bucket
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Via property with a couchbase connection string:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Couchbase("db"));
 *
 *   get("/", req -> {
 *     Bucket beers = req.require(Bucket.class);
 *     // do with beer bucket
 *   });
 * }
 * }</pre>
 *
 * <p>
 * The <code>db</code> property is defined in the <code>application.conf</code> file as a couchbase
 * connection string.
 * </p>
 *
 * <h2>create, read, update and delete</h2>
 * <p>
 * Jooby provides a more flexible, easy to use and powerful CRUD operations via
 * {@link AsyncDatastore}/{@link Datastore} objects.
 * </p>
 *
 * <pre>{@code
 *
 * import org.jooby.couchbase.Couchbase;
 * import org.jooby.couchbase.N1Q;
 * ...
 * {
 *   use(new Couchbase("couchbase://localhost/beers"));
 *
 *   use("/api/beers")
 *     .get(req -> {
 *       Datastore ds = req.require(Datastore.class);
 *       return ds.query(N1Q.from(Beer.class));
 *     })
 *     .post(req -> {
 *       Datastore ds = req.require(Datastore.class);
 *       Beer beer = req.body().to(Beer.class);
 *       return ds.upsert(beer);
 *     })
 *     .get(":id", req -> {
 *       Datastore ds = req.require(Datastore.class);
 *       return ds.get(Beer.class, req.param("id").value());
 *     })
 *     .delete(":id", req -> {
 *       Datastore ds = req.require(Datastore.class);
 *       return ds.delete(Beer.class, req.param("id").value());
 *     });
 * }
 * }</pre>
 *
 * <p>
 * As you can see benefits over {@link AsyncRepository}/{@link Repository} are clear: you don't have
 * to deal with {@link EntityDocument} just send or retrieve POJOs.
 * </p>
 * <p>
 * Another good reason is that {@link AsyncDatastore}/{@link Datastore} supports query operations
 * with POJOs too.
 * </p>
 *
 * <h3>design and implementation choices</h3>
 * <p>
 * The {@link AsyncDatastore}/{@link Datastore} simplifies a lot the integration between Couchbase
 * and POJOs. This section describes how IDs are persisted and how mapping works.
 * </p>
 *
 * <p>
 * A document persisted by an {@link AsyncDatastore}/{@link Datastore} looks like:
 * </p>
 *
 * <pre>
 * {
 *   "model.Beer::1": {
 *     "name": "IPA",
 *     ...
 *     "id": 1,
 *     "_class": "model.Beer"
 *   }
 * }
 * </pre>
 *
 * <p>
 * The couchbase document ID contains the fully qualified name of the class, plus <code>::</code>
 * plus the entity/business ID: <code>mode.Beer::1</code>.
 * </p>
 * <p>
 * The business ID is part of the document, here the business ID is: <code>id:1</code>. The business
 * ID is required while creating POJO from couchbase queries.
 * </p>
 * <p>
 * Finally, a <code>_class</code> attribute is also part of the document. It contains the fully
 * qualified name of the class and its required while creating POJO from couchbase queries.
 * </p>
 *
 * <h3>mapping pojos</h3>
 * <p>
 * Mapping between document/POJOs is done internally with a custom {@link EntityConverter}. The
 * {@link EntityConverter} uses an internal copy of <code>ObjectMapper</code> object from
 * <code>Jackson</code>. So in <strong>theory</strong> anything that can be handle by
 * <code>Jackson</code> will work.
 * </p>
 *
 * <p>
 * In order to work with a POJO, you must defined an ID. There are two options:
 * </p>
 *
 * <p>
 * 1. Add an <code>id</code> field to your POJO:
 * </p>
 *
 * <pre>{@code
 * public class Beer {
 *   private String id;
 * }
 * }</pre>
 *
 * <p>
 * 2. Use a business name (not necessarily id) and add <code>Id</code> annotation:
 * </p>
 *
 * <pre>{@code
 *
 * import import com.couchbase.client.java.repository.annotation.Id;
 *
 * public class Beer {
 *   &#64;Id
 *   private String beerId;
 * }
 * }</pre>
 *
 * <p>
 * Auto-increment IDs are supported via {@link GeneratedValue}:
 * </p>
 *
 * <pre>{@code
 *
 * public class Beer {
 *   private Long id;
 * }
 * }</pre>
 *
 * <p>
 * Auto-increment IDs are generated using {@link Bucket#counter(String, long, long)} function
 * and they must be <code>Long</code>. We use the POJO fully qualified name as counter ID.
 * </p>
 *
 * <p>
 * Any other field will be mapped too, you don't need to annotate an attribute with {@link Field}.
 * If you don't want to persist an attribute, just ad the <code>transient</code> Java modifier:
 * </p>
 *
 * <pre>{@code
 * public class Beer {
 *   private String id;
 *
 *   private transient ignored;
 * }
 * }</pre>
 *
 * <p>
 * Keep in mind that if you annotated your POJO with <code>Jackson</code> annotations they will
 * be ignored... because we use an internal copy of <code>Jackson</code> that comes with
 * <code>Java Couchbase SDK</code>
 * </p>
 *
 * <h2>reactive usage</h2>
 * <p>
 * Couchbase SDK allows two programming model: <code>blocking</code> and <code>reactive</code>. We
 * already see how to use the blocking API, now is time to see how to use the <code>reactive</code>
 * API:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Couchbase("couchbase://localhost/beers"));
 *
 *   get("/", req -> {
 *     AsyncBucket bucket = req.require(AsyncBucket.class);
 *     // do with async bucket ;)
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Now, what to do with Observables? Do we have to block? Not necessarily if we use the
 * <code>Rx</code> module:
 * </p>
 *
 * <pre>{@code
 *
 * import org.jooby.rx.Rx;
 *
 * {
 *   // handle observable route responses
 *   use(new Rx());
 *
 *   use(new Couchbase("couchbase://localhost/beers"));
 *
 *   get("/api/beer/:id", req -> {
 *     AsyncDatastore ds = req.require(AsyncDatastore.class);
 *     String id = req.param("id").value();
 *     Observable<Beer> beer = ds.get(Beer.class, id);
 *     return beer;
 *   });
 * }
 * }</pre>
 *
 * <p>
 * The <code>Rx</code> module deal with observables so you can safely return {@link Observable} from
 * routes (Jooby rocks!).
 * </p>
 *
 * <h2>multiple buckets</h2>
 * <p>
 * If for any reason your application requires more than 1 bucket... then:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Couchbase("couchbase://localhost/beers")
 *     .buckets("extra-bucket"));
 *
 *   get("/", req -> {
 *     Bucket bucket = req.require("beers", Bucket.class);
 *     Bucket extra = req.require("extra-bucket", Bucket.class);
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Easy, right? Same principle apply for Async* objects
 * </p>
 *
 * <h2>multiple clusters</h2>
 * <p>
 * Again, if for any reason your application requires multiple clusters... then:
 * </p>
 *
 * <pre>{@code
 * {
 *   CouchbaseEnvironment env = ...;
 *
 *   use(new Couchbase("couchbase://192.168.56.1")
 *      .environment(env));
 *
 *   use(new Couchbase("couchbase://192.168.57.10")
 *      .environment(env));
 * }
 * }</pre>
 *
 * <p>
 * You must shared the {@link CouchbaseEnvironment} as documented <a href=
 * "http://developer.couchbase.com/documentation/server/4.0/sdks/java-2.2/managing-connections.html#story-h2-4">here</a>.
 * </p>
 *
 * <h2>options</h2>
 *
 * <h3>bucket password</h3>
 * <p>
 * You can set a global bucket password via: <code>couchbase.bucket.password</code> property, or
 * local bucket password (per bucket) via <code>couchbase.bucket.[name].password</code> property.
 * </p>
 *
 * <h3>environment configuration</h3>
 * <p>
 * Environment configuration is available via: <code>couchbase.env</code> namespace, here is an
 * example on how to setup <code>kvEndpoints</code>:
 * </p>
 *
 * <pre>
 * couchbase.env.kvEndpoints = 3
 * </pre>
 *
 * <h3>cluster manager</h3>
 * <p>
 * A {@link ClusterManager} service is available is you set an cluster username and password:
 * </p>
 *
 * <pre>
 * couchbase.cluster.username = foo
 * couchbase.cluster.password = bar
 * </pre>
 *
 * @author edgar
 * @since 1.0.0.CR7
 */
public class Couchbase implements Module {

  // FIXME: converter hack
  static final JacksonMapper CONVERTER = new JacksonMapper();

  static final AtomicInteger COUNTER = new AtomicInteger(0);

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Function<Config, CouchbaseEnvironment> env = c -> DefaultCouchbaseEnvironment.create();

  private String db;

  private Set<String> buckets = new LinkedHashSet<>();

  private Optional<String> sessionBucket = Optional.empty();

  /**
   * Creates a new {@link Couchbase} module.
   *
   * <pre>{@code
   * {
   *   use(new Couchbase("couchbase://localhost/bucket"));
   * }
   * }</pre>
   *
   * Or add a <code>db</code> property to your <code>.conf</code> file:
   *
   * <pre>{@code
   * {
   *   use(new Couchbase("db"));
   * }
   * }</pre>
   *
   * @param db A couchbase connection string or a property with a connection string.
   */
  public Couchbase(final String db) {
    this.db = requireNonNull(db, "Connection String/Database key required.");
  }

  /**
   * Creates a new {@link Couchbase} module. You must add a <code>db</code> property to your
   * <code>.conf</code> file.
   */
  public Couchbase() {
    this("db");
  }

  /**
   * Set a shared {@link CouchbaseEnvironment}. The environment will shutdown automatically.
   *
   * @param env Environment provider.
   * @return This module.
   */
  public Couchbase environment(final Function<Config, CouchbaseEnvironment> env) {
    this.env = env;
    return this;
  }

  /**
   * Set a shared {@link CouchbaseEnvironment}. The environment will shutdown automatically.
   *
   * @param env Environment provider.
   * @return This module.
   */
  public Couchbase environment(final Supplier<CouchbaseEnvironment> env) {
    return environment(c -> env.get());
  }

  /**
   * Set a shared {@link CouchbaseEnvironment}. The environment will shutdown automatically.
   *
   * @param env Environment.
   * @return This module.
   */
  public Couchbase environment(final CouchbaseEnvironment env) {
    return environment(() -> env);
  }

  /**
   * List of buckets to open on startup.
   *
   * @param names Bucket names.
   * @return This module.
   */
  public Couchbase buckets(final String... names) {
    buckets.addAll(Arrays.asList(names));
    return this;
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    String cstr = db.startsWith(ConnectionString.DEFAULT_SCHEME) ? db : conf.getString(db);
    String defbucket = defbucket(cstr);
    System.setProperty(N1Q.COUCHBASE_DEFBUCKET, defbucket);
    String dbname = cstr.equals(db) ? defbucket : db;

    // dump couchbase.env.* as system properties
    if (conf.hasPath("couchbase.env")) {
      conf.getConfig("couchbase.env").entrySet().forEach(e -> {
        System.setProperty("com.couchbase." + e.getKey(), e.getValue().unwrapped().toString());
      });
    }

    log.debug("Starting {}", cstr);

    Function3<Class, String, Object, Void> bind = (type, name, value) -> {
      binder.bind(Key.get(type, Names.named(name))).toInstance(value);
      if (COUNTER.get() == 0) {
        binder.bind(Key.get(type)).toInstance(value);
      }
      return null;
    };

    CouchbaseEnvironment cenv = this.env.apply(conf);
    if (COUNTER.get() == 0) {
      binder.bind(CouchbaseEnvironment.class).toInstance(cenv);
    }

    // FIXME: ConnectionString doesn't work with bucket in the url
    String cstrworkaround = cstr.replace("/" + defbucket, "");
    CouchbaseCluster cluster = CouchbaseCluster.fromConnectionString(cenv, cstrworkaround);
    bind.apply(CouchbaseCluster.class, dbname, cluster);

    // start cluster manager?
    if (conf.hasPath("couchbase.cluster.username")) {
      ClusterManager clusterManager = cluster
          .clusterManager(
              conf.getString("couchbase.cluster.username"),
              conf.getString("couchbase.cluster.password"));
      bind.apply(ClusterManager.class, dbname, clusterManager);
    }

    // configure buckets, repositories and datastores
    Set<String> buckets = Sets.newHashSet(defbucket);
    buckets.addAll(this.buckets);

    Function<String, String> password = name -> {
      return Arrays.asList(
          "couchbase.bucket." + name + ".password",
          "couchbase.bucket.password").stream()
          .filter(conf::hasPath)
          .map(conf::getString)
          .findFirst()
          .orElse(null);
    };
    buckets.forEach(name -> {
      Bucket bucket = cluster.openBucket(name, password.apply(name));
      log.debug("  bucket opened: {}", name);

      bind.apply(Bucket.class, name, bucket);
      AsyncBucket async = bucket.async();
      bind.apply(AsyncBucket.class, name, async);

      Repository repo = bucket.repository();
      AsyncRepository asyncrepo = repo.async();

      // FIXME: converter hack
      SetConverterHack.forceConverter(asyncrepo, CONVERTER);

      bind.apply(Repository.class, name, repo);
      bind.apply(AsyncRepository.class, name, asyncrepo);

      AsyncDatastoreImpl asyncds = new AsyncDatastoreImpl(async, asyncrepo, idGen(bucket),
          CONVERTER);
      bind.apply(AsyncDatastore.class, name, asyncds);
      bind.apply(Datastore.class, name, new DatastoreImpl(asyncds));

      buckets.add(name);

      COUNTER.incrementAndGet();
    });

    // special binding for session bucket: either the default bucket or custom
    this.sessionBucket.ifPresent(buckets::add);
    String session = this.sessionBucket.orElse(defbucket);
    bind.apply(Bucket.class, "session", cluster.openBucket(session, password.apply(session)));

    env.onStop(r -> {
      buckets.forEach(n -> {
        Try.of(() -> r.require(n, Bucket.class).close())
            .onFailure(x -> log.debug("bucket {} close operation resulted in exception", n, x))
            .getOrElse(false);
      });
      Try.run(cluster::disconnect)
          .onFailure(x -> log.debug("disconnect operation resulted in exception", x));

      Try.run(cenv::shutdown)
          .onFailure(x -> log.debug("environment shutdown resulted in exception", x));
    });
  }

  /**
   * Use a custom bucket for HTTP Session, see {@link CouchbaseSessionStore}.
   *
   * @param bucket Bucket to use for HTTP Session.
   * @return This module.
   */
  public Couchbase sessionBucket(final String bucket) {
    this.sessionBucket = Optional.of(requireNonNull(bucket, "Session bucket required."));
    return this;
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "couchbase.conf");
  }

  private static String defbucket(final String cstr) {
    List<String> segments = Splitter.on("/").trimResults().omitEmptyStrings()
        .splitToList(cstr.substring(0, Math.max(cstr.indexOf("?"), cstr.length()))
            .replace(ConnectionString.DEFAULT_SCHEME, ""));
    return segments.size() == 2 ? segments.get(1) : CouchbaseAsyncCluster.DEFAULT_BUCKET;
  }

  private static Function<Object, Object> idGen(final Bucket bucket) {
    return entity -> {
      return IdGenerator.getOrGenId(entity,
          () -> bucket.counter(entity.getClass().getName(), 1, 1).content());
    };
  }

}
