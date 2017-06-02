package org.jooby.elasticsearch;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.elasticsearch.client.RestClient;
import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Elasticsearch.class, Multibinder.class})
@PowerMockIgnore("javax.net.ssl.*")
public class ElasticsearchTest {

  private MockUnit.Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(CheckedRunnable.class))).andReturn(env);
  };

  private MockUnit.Block nb = unit -> {
    RestClient client = unit.mock(RestClient.class);
    unit.registerMock(RestClient.class, client);
  };

  @SuppressWarnings("unchecked")
  private MockUnit.Block bindings = unit -> {
    AnnotatedBindingBuilder<RestClient> abbclient = unit.mock(AnnotatedBindingBuilder.class);
    abbclient.toInstance(unit.capture(RestClient.class));
    //abbclient.toInstance(anyObject(RestClient.class));

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(RestClient.class)).andReturn(abbclient);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
            .expect(nb)
            .expect(bindings)
            .expect(onStop)
            .run(unit -> {
              new Elasticsearch()
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            }, unit -> {
              List<RestClient> captured = unit.captured(RestClient.class);
              assert captured.size() == 1;

              List<CheckedRunnable> callbacks = unit.captured(CheckedRunnable.class);
              callbacks.get(0).run();
            });
  }

  @Test
  public void config() throws Exception {
    Config config = ConfigFactory.parseResources(Elasticsearch.class, "es.conf");
    assertEquals(config, new Elasticsearch().config());
  }

}
