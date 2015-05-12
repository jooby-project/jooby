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
package org.jooby.elasticsearch;

import static java.util.Objects.requireNonNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Renderer;
import org.jooby.Route;
import org.jooby.internal.elasticsearch.BytesReferenceFormatter;
import org.jooby.internal.elasticsearch.EmbeddedHandler;
import org.jooby.internal.elasticsearch.ManagedClient;
import org.jooby.internal.elasticsearch.ManagedNode;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * <p>
 * Enterprise full text search via <a href="https://github.com/elastic/elasticsearch">Elastic
 * Search</a>.
 * </p>
 * Provides a client/local API but also a RESTFul API.
 *
 * <h1>usage</h1>
 *
 * <pre>
 * {
 *   use(new ElasticSearch("/search"));
 * }
 * </pre>
 * <p>
 * Elastic search will listen under the <code>/search</code> path. Here are some examples:
 * </p>
 *
 * <p>
 * Creating a index:
 * </p>
 *
 * <pre>
 *  curl -XPUT 'localhost:8080/search/customer?pretty'
 * </pre>
 *
 * <p>
 * Indexing a doc:
 * </p>
 *
 * <pre>
 *  curl -XPUT 'localhost:8080/search/customer/external/1?pretty' -d '
 * {
 *  "name": "John Doe"
 * }'
 * </pre>
 *
 * <p>
 * Getting a doc:
 * </p>
 *
 * <pre>
 *  curl 'localhost:8080/search/customer/external/1?pretty'
 * </pre>
 *
 * <h1>client API</h1>
 * <p>
 * The module exposes a {@link Client} and {@link Node} instances
 * </p>
 *
 * <pre>
 *
 * post("/:id", req {@literal ->} {
 *   // index a document
 *   Client client = req.require(Client.class);
 *   Map&lt;String, Object&gt; json = new HashMap&lt;&gt;();
 *   json.put("user", "kimchy");
 *   json.put("message", "trying out Elasticsearch");
 *
 *   return client.prepareIndex("twitter", "tweet", req.param("id").value())
 *     .setSource(json)
 *     .execute()
 *     .actionGet();
 * });
 *
 * get("/:id", req {@literal ->} {
 *   // get a doc
 *   Client client = req.require(Client.class);
 *   return client.prepareGet("twitter", "tweet", req.param("id").value())
 *     .execute()
 *     .actionGet()
 *     .getSource();
 * });
 *
 * delete("/:id", req {@literal ->} {
 *   // delete a doc
 *   Client client = req.require(Client.class);
 *   return client.prepareDelete("twitter", "tweet", req.param("id").value())
 *     .execute()
 *     .actionGet();
 * });
 * </pre>
 *
 * <h1>configuration</h1>
 * <p>
 * If it possible to setup or configure <a href="https://github.com/elastic/elasticsearch">Elastic
 * Search</a> via <code>application.conf</code>, just make sure to prefix the property with
 * <code>elasticsearch</code>:
 * </p>
 *
 * <pre>
 *  elasticsearch.http.jsonp.enable = true
 * </pre>
 *
 * or programmatically:
 *
 * <pre>
 * {
 *   use(new ElasticSearch().doWith(settings {@literal ->} {
 *     settings.put(..., ...);
 *   });
 * }
 * </pre>
 *
 * <h2>http.enabled</h2>
 * <p>
 * HTTP is disabled and isn't possible to change this value. What does it mean? Jooby setup a custom
 * handler which makes it possible to use a Jooby server to serve Elastic Search requests and avoid
 * the need of starting up another server running in a different port.
 * </p>
 * <p>
 * Most of the <code>http.*</code> properties has no sense in Jooby.
 * </p>
 *
 * <h2>path.data</h2>
 * <p>
 * Path data is set to a temporary directory: <code>${application.tmpdir}/es/data</code>. It is
 * useful for development, but if you need want to make sure the index is persistent between server
 * restarts, please make sure to setup this path to something else.
 * </p>
 *
 * <h2>node.name</h2>
 * <p>
 * Node name is set to: <code>application.name</code>.
 * </p>
 *
 * <h2>cluster.name</h2>
 * <p>
 * Cluster name is set to: <code>${application.name}-cluster</code>.
 * </p>
 *
 * @author edgar
 * @since 0.6.0
 */
public class ElasticSearch implements Jooby.Module {

  private String path;

  private Consumer<Builder> scallback;

  private BiConsumer<Builder, Config> dcallback;

  /**
   * Mount {@link ElasticSearch} under the given path.
   *
   * @param path A path, like: "/search", "/es", "/etc".
   */
  public ElasticSearch(final String path) {
    this.path = requireNonNull(path, "Path is required.");
  }

  /**
   * Mount {@link ElasticSearch} in the default path: "/search".
   */
  public ElasticSearch() {
    this("/search");
  }

  /**
   * Configure settings:
   *
   * <pre>
   * {
   *   use(new ElasticSearch().doWith(settings {@literal ->} {
   *     settings.put(..., ...);
   *   });
   * }
   * </pre>
   *
   * @param callback A configuration callback.
   * @return This module.
   */
  public ElasticSearch doWith(final Consumer<ImmutableSettings.Builder> callback) {
    this.scallback = requireNonNull(callback, "Configurer callback is required.");
    return this;
  }

  /**
   * Configure settings:
   *
   * <pre>
   * {
   *   use(new ElasticSearch().doWith((settings, config) {@literal ->} {
   *     settings.put(..., ...);
   *   });
   * }
   * </pre>
   *
   * @param callback A configuration callback.
   * @return This module.
   */
  public ElasticSearch doWith(final BiConsumer<ImmutableSettings.Builder, Config> callback) {
    this.dcallback = requireNonNull(callback, "Configurer callback is required.");
    return this;
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    ImmutableSettings.Builder settings = ImmutableSettings.builder();
    config.getConfig("elasticsearch").entrySet()
        .forEach(e -> settings.put(e.getKey(), e.getValue().unwrapped()));

    if (scallback != null) {
      scallback.accept(settings);
    }

    if (dcallback != null) {
      dcallback.accept(settings, config);
    }

    // disabled HTTP, handle by jooby
    settings.put("http.enabled", "false");

    boolean detailedErrors = Boolean.valueOf(settings.get("http.detailed_errors.enabled"));

    NodeBuilder nb = NodeBuilder.nodeBuilder()
        .settings(settings);

    ManagedNode node = new ManagedNode(nb);
    binder.bind(Node.class).toProvider(node).asEagerSingleton();
    binder.bind(Client.class).toProvider(new ManagedClient(node)).asEagerSingleton();

    Multibinder.newSetBinder(binder, Renderer.class).addBinding()
        .toInstance(new BytesReferenceFormatter());

    EmbeddedHandler handler = new EmbeddedHandler(path, node, detailedErrors);
    Multibinder<Route.Definition> route = Multibinder.newSetBinder(binder, Route.Definition.class);
    route.addBinding()
        .toInstance(
            new Route.Definition("*", path + "/**", handler).name("elasticsearch")
        );
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(ElasticSearch.class, "es.conf");
  }
}
