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
package org.jooby.internal.aws;

import static java.util.Objects.requireNonNull;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.google.common.base.Joiner;
import com.typesafe.config.Config;

public class ConfigCredentialsProvider implements AWSCredentialsProvider {

  private static final String ACCESS_KEY = "accessKey";

  private static final String SECRET_KEY = "secretKey";

  private static final String SESSION_TOKEN = "sessionToken";

  private Config config;

  private String accessKey;

  private String secretKey;

  private String sessionToken;

  public ConfigCredentialsProvider(final Config config) {
    this.config = requireNonNull(config, "Config is required.");
  }

  public ConfigCredentialsProvider service(final String serviceName) {
    // global vs service specific key
    String accessKey = key("aws", ACCESS_KEY);
    String secretKey = key("aws", SECRET_KEY);
    String sessionToken = key("aws", SESSION_TOKEN);

    String serviceAccessKey = key("aws", serviceName, ACCESS_KEY);
    String serviceSecretKey = key("aws", serviceName, SECRET_KEY);
    String serviceSessionToken = key("aws", serviceName, SESSION_TOKEN);

    if (this.config.hasPath(serviceAccessKey)) {
      accessKey = serviceAccessKey;
    }
    if (this.config.hasPath(serviceSecretKey)) {
      secretKey = serviceSecretKey;
    }
    // override
    this.accessKey = this.config.getString(accessKey);
    this.secretKey = this.config.getString(secretKey);
    // session token
    if (this.config.hasPath(serviceSessionToken)) {
      this.sessionToken = this.config.getString(serviceSessionToken);
    } else if (this.config.hasPath(sessionToken)) {
      this.sessionToken = this.config.getString(sessionToken);
    }
    return this;
  }

  @Override
  public AWSCredentials getCredentials() {
    if (sessionToken != null) {
      return new BasicSessionCredentials(accessKey, secretKey, sessionToken);
    }
    return new BasicAWSCredentials(accessKey, secretKey);
  }

  @Override
  public void refresh() {
    // noop
  }

  private String key(final String... parts) {
    return Joiner.on(".").join(parts);
  }

}
