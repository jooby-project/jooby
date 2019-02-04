package org.jooby.jedis;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jooby.Env;
import org.jooby.funzy.Throwing;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import redis.clients.jedis.JedisSentinelPool;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertNotNull;


@RunWith(PowerMockRunner.class)
@PrepareForTest({RedisSentinel.class, JedisSentinelPool.class, GenericObjectPoolConfig.class})
public class RedisSentinelTest {

    private MockUnit.Block onManaged = unit -> {
        Env env = unit.get(Env.class);
        expect(env.onStart(isA(Throwing.Runnable.class))).andReturn(env);
        expect(env.onStop(isA(Throwing.Runnable.class))).andReturn(env);
    };

    @SuppressWarnings("unchecked")
    @Test
    public void shouldGetJedisSentinelInstance() throws Exception {
        Config config = jedisConfig().withValue("db", ConfigValueFactory.fromAnyRef("redis://localhost:26379"))
                .withValue("jedis.sentinel.hosts", ConfigValueFactory.fromAnyRef(Collections.singletonList("localhost:26379")));
        new MockUnit(Env.class, Binder.class, JedisSentinelPool.class)
                .expect(unit -> {
                    Env env = unit.get(Env.class);
                    expect(env.serviceKey()).andReturn(new Env.ServiceKey());
                })
                .expect(unit -> {
                    GenericObjectPoolConfig poolConf = unit.mockConstructor(GenericObjectPoolConfig.class);
                    poolConf.setBlockWhenExhausted(true);
                    poolConf.setEvictionPolicyClassName("org.apache.commons.pool2.impl.DefaultEvictionPolicy");
                    poolConf.setJmxEnabled(false);
                    poolConf.setJmxNamePrefix("redis-pool");
                    poolConf.setLifo(true);
                    poolConf.setMaxIdle(10);
                    poolConf.setMaxTotal(128);
                    poolConf.setMaxWaitMillis(-1);
                    poolConf.setMinEvictableIdleTimeMillis(1800000L);
                    poolConf.setMinIdle(10);
                    poolConf.setNumTestsPerEvictionRun(3);
                    poolConf.setSoftMinEvictableIdleTimeMillis(1800000L);
                    poolConf.setTestOnBorrow(false);
                    poolConf.setTestOnReturn(false);
                    poolConf.setTestWhileIdle(false);
                    poolConf.setTimeBetweenEvictionRunsMillis(-1);

                    HashSet<String> hosts = new HashSet<>();
                    hosts.add("localhost:26379");

                    JedisSentinelPool pool = unit.mockConstructor(
                            JedisSentinelPool.class,
                            new Class[]{String.class, Set.class, GenericObjectPoolConfig.class, int.class},
                            "master", hosts, poolConf, 2000);

                    AnnotatedBindingBuilder<JedisSentinelPool> jpABB = unit.mock(AnnotatedBindingBuilder.class);
                    jpABB.toInstance(pool);
                    jpABB.toInstance(pool);

                    Binder binder = unit.get(Binder.class);
                    expect(binder.bind(Key.get(JedisSentinelPool.class))).andReturn(jpABB);
                    expect(binder.bind(Key.get(JedisSentinelPool.class, Names.named("db")))).andReturn(jpABB);
                })
                .expect(onManaged)
                .run(unit -> {
                    new RedisSentinel().configure(unit.get(Env.class), config, unit.get(Binder.class));
                }, unit -> {
                    assertNotNull(unit.get(JedisSentinelPool.class));
                });
    }

    private Config jedisConfig() {
        return ConfigFactory.parseResources(getClass(), "jedis.conf");
    }

}
