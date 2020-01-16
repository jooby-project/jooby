/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.aws;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_SYSTEM_PROPERTY;
import static com.amazonaws.SDKGlobalConfiguration.SECRET_KEY_SYSTEM_PROPERTY;

public class ConfigCredentialsProvider implements AWSCredentialsProvider {

  private Config config;

  public ConfigCredentialsProvider(Config config) {
    this.config = config;
  }

  @Override public AWSCredentials getCredentials() {
    try {
      return new BasicAWSCredentials(config.getString(ACCESS_KEY_SYSTEM_PROPERTY),
          config.getString(SECRET_KEY_SYSTEM_PROPERTY));
    } catch (ConfigException.Missing x) {
      throw new SdkClientException(
          "Unable to load AWS credentials from application properties ("
              + ACCESS_KEY_SYSTEM_PROPERTY + " and " + SECRET_KEY_SYSTEM_PROPERTY + ")");
    }
  }

  @Override public void refresh() {

  }
}
