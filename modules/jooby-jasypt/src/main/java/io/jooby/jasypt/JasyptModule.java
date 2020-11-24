/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jasypt;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimplePBEConfig;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.SneakyThrows;

/**
 * Jasypt module:  https://jooby.io/modules/jasypt.
 *
 * Usage:
 *
 * - Add jasypt dependency
 *
 * - Install them
 *
 * <pre>{@code
 * {
 *   install(new JasyptModule());
 * }
 * }</pre>
 *
 * The module looks for all properties starting with <code>enc</code> and decrypt them.
 *
 * @author edgar
 * @since 2.9.4
 */
public class JasyptModule implements Extension {

  private static final SneakyThrows.Function<Config, String> DEFAULT_PASSWORD_PROVIDER = config -> config
      .getString("jasypt.password");

  private SneakyThrows.Function<Config, String> passwordProvider;

  private String prefix = "enc";

  private PBEStringEncryptor encryptor;

  /**
   * Creates a new Jasypt module.
   *
   * @param encryptor Encryptor.
   */
  public JasyptModule(@Nonnull PBEStringEncryptor encryptor) {
    this.encryptor = encryptor;
  }

  /**
   * Creates a new Jasypt module.
   */
  public JasyptModule() {
    this(DEFAULT_PASSWORD_PROVIDER);
  }

  /**
   * Creates a Jasypt module with the password provider.
   *
   * @param passwordProvider Password provider.
   */
  public JasyptModule(@Nonnull SneakyThrows.Function<Config, String> passwordProvider) {
    this.passwordProvider = passwordProvider;
  }

  /**
   * Set encryption prefix. All properties using this prefix are going to be decrypted.
   * Defaults is <code>enc</code>.
   *
   * @param prefix Prefix of encrypted properties.
   * @return This module.
   */
  public JasyptModule setPrefix(@Nonnull String prefix) {
    this.prefix = prefix;
    return this;
  }

  @Override public void install(@Nonnull Jooby application) {
    PBEStringEncryptor encryptor = Optional.ofNullable(this.encryptor)
        .orElseGet(() -> create(application, passwordProvider));

    // Process properties
    Environment environment = application.getEnvironment();
    Map<String, String> encrypted = environment.getProperties(prefix, null);
    if (!encrypted.isEmpty()) {
      for (Map.Entry<String, String> entry : encrypted.entrySet()) {
        String encryptedValue = entry.getValue();
        String plainValue = encryptor.decrypt(encryptedValue);
        entry.setValue(plainValue);
      }
      // Override
      Config newConfig = ConfigFactory.parseMap(encrypted, "jasypt")
          .withFallback(environment.getConfig());
      environment.setConfig(newConfig);
    }

    ServiceRegistry services = application.getServices();
    services.put(PBEStringEncryptor.class, encryptor);
    Class encryptorType = encryptor.getClass();
    services.putIfAbsent(encryptorType, encryptor);
  }

  /**
   * Creates a PBE encryptor from application property files. The given key is used for reading
   * jasypt password and possible other customization. Please note the <code>key.password</code>
   * must be present in order to configure proper password setup.
   *
   * Properties:
   * <pre>
   *   # Required:
   *   jasypt.password = password uses to encrypt
   *
   *   # Optional:
   *   jasypt.algorithm = PBEWithMD5AndDES
   *   jasypt.keyObtentionIterations = 1000
   *   jasypt.poolSize = 5
   *   jasypt.ivGeneratorClassName = classname
   *   jasypt.saltGeneratorClassName = org.jasypt.salt.RandomSaltGenerator
   *   jasypt.providerName = SunJCE
   * </pre>
   *
   * Please note this method creates a {@link PooledPBEStringEncryptor} when poolSize is set.
   *
   * @param application Application.
   * @return A new encryptor.
   */
  public static PBEStringEncryptor create(@Nonnull Jooby application) {
    return create(application, DEFAULT_PASSWORD_PROVIDER);
  }

  private static PBEStringEncryptor create(@Nonnull Jooby application,
      @Nonnull SneakyThrows.Function<Config, String> passwordProvider) {
    Config config = application.getConfig();

    String password = passwordProvider.apply(config);

    SimplePBEConfig pbeConfig = new SimplePBEConfig();
    pbeConfig.setPassword(password);

    if (config.hasPath("jasypt")) {
      Config jasypt = config.getConfig("jasypt");

      withString(jasypt, "keyObtentionIterations", pbeConfig::setKeyObtentionIterations);
      withString(jasypt, "poolSize", pbeConfig::setPoolSize);
      withString(jasypt, "ivGeneratorClassName", pbeConfig::setIvGeneratorClassName);
      withString(jasypt, "saltGeneratorClassName", pbeConfig::setSaltGeneratorClassName);
      withString(jasypt, "providerName", pbeConfig::setProviderName);
    }

    if (pbeConfig.getPoolSize() == null) {
      StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
      encryptor.setConfig(pbeConfig);
      return encryptor;
    } else {
      PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
      encryptor.setConfig(pbeConfig);
      return encryptor;
    }
  }

  private static void withString(Config config, String key, Consumer<String> consumer) {
    if (config.hasPath(key)) {
      consumer.accept(config.getString(key));
    }
  }
}
