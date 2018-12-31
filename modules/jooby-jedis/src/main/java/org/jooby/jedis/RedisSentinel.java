package org.jooby.jedis;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jooby.Env;
import org.jooby.Jooby;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import javax.inject.Provider;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * Created by
 *
 * @Author: lis, Luganski Igor
 * @since 1.5.0
 */
public class RedisSentinel implements Jooby.Module {

    /**
     * Database name.
     */
    private String name = "db";

    private Set sentinels = new HashSet();

    /**
     * <p>
     * Creates a new {@link Redis} instance and connect to the provided database. Please note, the
     * name is a property in your <code>application.conf</code> file with Redis URI.
     * </p>
     *
     * <pre>
     * {
     *   use(new Redis("db1"));
     * }
     * </pre>
     *
     * application.conf
     *
     * <pre>
     * db1 = ""redis://localhost:6379""
     * </pre>
     *
     * Default database name is: <code>db</code>
     *
     * @param name A database name.
     */
    public RedisSentinel(final String name) {
        this.name = requireNonNull(name, "A db property is required.");
    }

    @Override
    public void configure(Env env, Config config, Binder binder) throws Throwable {
        /**
         * Pool
         */
        GenericObjectPoolConfig poolConfig = poolConfig(config, name);
        int timeout = (int) config.getDuration("jedis.timeout", TimeUnit.MILLISECONDS);
        URI uri = URI.create(config.getString(name));

        String MASTER_NAME = "";    //todo take from Config
        String REDIS_PASSWORD = ""; //todo take from Config
        //todo take from Config
//    sentinels.add("mymaster-0.servers.example.com:26379");
//        sentinels.add("mymaster-1.servers.example.com:26379");
//        sentinels.add("mymaster-2.servers.example.com:26379");

//        JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, poolConfig);
        JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, poolConfig, REDIS_PASSWORD);

        RedisProvider provider = new RedisProvider(pool, uri, poolConfig);
        pool.destroy();
        env.onStart(provider::start);
        env.onStop(provider::stop);

        Provider<Jedis> jedis = (Provider<Jedis>) () -> pool.getResource();

        Env.ServiceKey serviceKey = env.serviceKey();
        serviceKey.generate(JedisSentinelPool.class, name, k -> binder.bind(k).toInstance(pool));
        serviceKey.generate(Jedis.class, name,
                k -> binder.bind(k).toProvider(jedis));

        //Socket socket = jedis.getClient().getSocket();
//        printer("Connected to " + socket.getRemoteSocketAddress());
    }

    private GenericObjectPoolConfig poolConfig(final Config config, final String name) {
        Config poolConfig = config.getConfig("jedis.pool");
        String override = "jedis." + name;
        if (config.hasPath(override)) {
            poolConfig = config.getConfig(override).withFallback(poolConfig);
        }
        return poolConfig(poolConfig);
    }

    private GenericObjectPoolConfig poolConfig(final Config config) {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setBlockWhenExhausted(config.getBoolean("blockWhenExhausted"));
        poolConfig.setEvictionPolicyClassName(config.getString("evictionPolicyClassName"));
        poolConfig.setJmxEnabled(config.getBoolean("jmxEnabled"));
        poolConfig.setJmxNamePrefix(config.getString("jmxNamePrefix"));
        poolConfig.setLifo(config.getBoolean("lifo"));
        poolConfig.setMaxIdle(config.getInt("maxIdle"));
        poolConfig.setMaxTotal(config.getInt("maxTotal"));
        poolConfig.setMaxWaitMillis(config.getDuration("maxWait", TimeUnit.MILLISECONDS));
        poolConfig.setMinEvictableIdleTimeMillis(config.getDuration("minEvictableIdle",
                TimeUnit.MILLISECONDS));
        poolConfig.setMinIdle(config.getInt("minIdle"));
        poolConfig.setNumTestsPerEvictionRun(config.getInt("numTestsPerEvictionRun"));
        poolConfig.setSoftMinEvictableIdleTimeMillis(
                config.getDuration("softMinEvictableIdle", TimeUnit.MILLISECONDS));
        poolConfig.setTestOnBorrow(config.getBoolean("testOnBorrow"));
        poolConfig.setTestOnReturn(config.getBoolean("testOnReturn"));
        poolConfig.setTestWhileIdle(config.getBoolean("testWhileIdle"));
        poolConfig.setTimeBetweenEvictionRunsMillis(config.getDuration("timeBetweenEvictionRuns",
                TimeUnit.MILLISECONDS));

        return poolConfig;
    }

    @Override
    public Config config() {
        return ConfigFactory.parseResources(getClass(), "jedis.conf");
    }

}
