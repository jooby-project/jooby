package org.jooby.elasticsearch;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.jooby.Env;
import org.jooby.Renderer;
import org.jooby.Route;
import org.jooby.internal.elasticsearch.BytesReferenceRenderer;
import org.jooby.internal.elasticsearch.EmbeddedHandler;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ElasticSearch.class, ImmutableSettings.class, NodeBuilder.class,
    Multibinder.class })
public class ElasticSearchTest {

  @SuppressWarnings("unchecked")
  private MockUnit.Block config = unit -> {
    ConfigValue opt1Value = unit.mock(ConfigValue.class);
    expect(opt1Value.unwrapped()).andReturn("0.0.0.0");

    ConfigValue opt2Value = unit.mock(ConfigValue.class);
    expect(opt2Value.unwrapped()).andReturn("8080");

    Entry<String, ConfigValue> opt1 = unit.mock(Entry.class);
    expect(opt1.getKey()).andReturn("http.host");
    expect(opt1.getValue()).andReturn(opt1Value);

    Entry<String, ConfigValue> opt2 = unit.mock(Entry.class);
    expect(opt2.getKey()).andReturn("http.port");
    expect(opt2.getValue()).andReturn(opt2Value);

    Set<Entry<String, ConfigValue>> options = Sets.newHashSet(opt1, opt2);
    Config es = unit.mock(Config.class);
    expect(es.entrySet()).andReturn(options);

    Config config = unit.get(Config.class);
    expect(config.getConfig("elasticsearch")).andReturn(es);
  };

  private MockUnit.Block onStart = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStart(unit.capture(CheckedRunnable.class))).andReturn(env);
  };

  private MockUnit.Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(CheckedRunnable.class))).andReturn(env);

    expect(env.onStop(unit.capture(CheckedRunnable.class))).andReturn(env);
  };

  private MockUnit.Block settings = unit -> {
    ImmutableSettings.Builder builder = unit.get(ImmutableSettings.Builder.class);
    expect(builder.put("http.port", (Object) "8080")).andReturn(builder);
    expect(builder.put("http.host", (Object) "0.0.0.0")).andReturn(builder);
    expect(builder.put("http.enabled", "false")).andReturn(builder);
    expect(builder.get("http.detailed_errors.enabled")).andReturn("true");

    unit.mockStatic(ImmutableSettings.class);
    expect(ImmutableSettings.builder()).andReturn(builder);
  };

  private MockUnit.Block nb = unit -> {
    Client client = unit.mock(Client.class);
    unit.registerMock(Client.class, client);

    Node node = unit.mock(Node.class);
    unit.registerMock(Node.class, node);
    expect(node.client()).andReturn(client);

    NodeBuilder nb = unit.get(NodeBuilder.class);
    expect(nb.settings(unit.get(ImmutableSettings.Builder.class))).andReturn(nb);
    expect(nb.build()).andReturn(node);

    unit.mockStatic(NodeBuilder.class);
    expect(NodeBuilder.nodeBuilder()).andReturn(nb);
  };

  @SuppressWarnings("unchecked")
  private MockUnit.Block bindings = unit -> {
    AnnotatedBindingBuilder<Node> abbnode = unit.mock(AnnotatedBindingBuilder.class);
    abbnode.toInstance(unit.get(Node.class));

    AnnotatedBindingBuilder<Client> abbclient = unit.mock(AnnotatedBindingBuilder.class);
    abbclient.toInstance(unit.get(Client.class));

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Node.class)).andReturn(abbnode);
    expect(binder.bind(Client.class)).andReturn(abbclient);

    BytesReferenceRenderer bytesRefFormatter = unit.mockConstructor(BytesReferenceRenderer.class);

    LinkedBindingBuilder<Renderer> lbbbf = unit.mock(LinkedBindingBuilder.class);
    lbbbf.toInstance(bytesRefFormatter);

    Multibinder<Renderer> formatters = unit.mock(Multibinder.class);
    expect(formatters.addBinding()).andReturn(lbbbf);

    EmbeddedHandler handler = unit.mockConstructor(EmbeddedHandler.class, new Class[]{String.class,
        Node.class, boolean.class }, eq("/es"), eq(unit.get(Node.class)), eq(true));

    Route.Definition rh = unit.mockConstructor(Route.Definition.class, new Class[]{String.class,
        String.class, Route.Handler.class }, "*", "/es/**", handler);
    expect(rh.name("elasticsearch")).andReturn(rh);

    LinkedBindingBuilder<Route.Definition> lbbr = unit.mock(LinkedBindingBuilder.class);
    lbbr.toInstance(rh);

    Multibinder<Route.Definition> routes = unit.mock(Multibinder.class);
    expect(routes.addBinding()).andReturn(lbbr);

    unit.mockStatic(Multibinder.class);
    expect(Multibinder.newSetBinder(binder, Renderer.class)).andReturn(formatters);
    expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(routes);

  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, ImmutableSettings.Builder.class,
        NodeBuilder.class)
            .expect(config)
            .expect(settings)
            .expect(nb)
            .expect(bindings)
            .expect(onStart)
            .expect(onStop)
            .expect(unit -> {
              Node node = unit.get(Node.class);
              expect(node.start()).andReturn(node);
              expect(node.stop()).andReturn(node);

              unit.get(Client.class).close();
            })
            .run(unit -> {
              new ElasticSearch("/es")
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            }, unit -> {
              List<CheckedRunnable> callbacks = unit.captured(CheckedRunnable.class);
              callbacks.get(0).run();
              callbacks.get(1).run();
              callbacks.get(2).run();
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void defaultsWithSettingsCallack() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, ImmutableSettings.Builder.class,
        NodeBuilder.class, Consumer.class)
            .expect(config)
            .expect(settings)
            .expect(nb)
            .expect(bindings)
            .expect(unit -> {
              unit.get(Consumer.class).accept(unit.get(ImmutableSettings.Builder.class));
            })
            .expect(onStart)
            .expect(onStop)
            .run(unit -> {
              new ElasticSearch("/es")
                  .doWith(unit.get(Consumer.class))
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void defaultsWithSettingsAndConfigCallack() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, ImmutableSettings.Builder.class,
        NodeBuilder.class, BiConsumer.class)
            .expect(config)
            .expect(settings)
            .expect(nb)
            .expect(bindings)
            .expect(unit -> {
              unit.get(BiConsumer.class).accept(unit.get(ImmutableSettings.Builder.class),
                  unit.get(Config.class));
            })
            .expect(onStart)
            .expect(onStop)
            .run(unit -> {
              new ElasticSearch("/es")
                  .doWith(unit.get(BiConsumer.class))
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            });
  }

  @Test
  public void config() throws Exception {
    Config config = ConfigFactory.parseResources(ElasticSearch.class, "es.conf");
    assertEquals(config, new ElasticSearch("/es").config());
  }

}
