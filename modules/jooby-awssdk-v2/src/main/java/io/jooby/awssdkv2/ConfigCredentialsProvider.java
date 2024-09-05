/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.awssdkv2;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.StringUtils;

public class ConfigCredentialsProvider implements AwsCredentialsProvider {
  private final Config config;

  public ConfigCredentialsProvider(@NonNull Config config) {
    this.config = config;
  }

  @Override
  public AwsCredentials resolveCredentials() {
    String accessKey =
        config.hasPath(SdkSystemSetting.AWS_ACCESS_KEY_ID.property())
            ? config.getString(SdkSystemSetting.AWS_ACCESS_KEY_ID.property())
            : null;
    String secretKey =
        config.hasPath(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property())
            ? config.getString(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property())
            : null;
    String sessionToken =
        config.hasPath(SdkSystemSetting.AWS_SESSION_TOKEN.property())
            ? config.getString(SdkSystemSetting.AWS_SESSION_TOKEN.property())
            : null;

    if (accessKey == null) {
      throw SdkClientException.builder()
          .message(
              String.format(
                  "Unable to load credentials from system settings. Access key must be"
                      + " specified either via environment variable (%s) or system property (%s).",
                  SdkSystemSetting.AWS_ACCESS_KEY_ID.environmentVariable(),
                  SdkSystemSetting.AWS_ACCESS_KEY_ID.property()))
          .build();
    }

    if (secretKey == null) {
      throw SdkClientException.builder()
          .message(
              String.format(
                  "Unable to load credentials from system settings. Secret key must be"
                      + " specified either via environment variable (%s) or system property (%s).",
                  SdkSystemSetting.AWS_SECRET_ACCESS_KEY.environmentVariable(),
                  SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property()))
          .build();
    }

    return StringUtils.isBlank(sessionToken)
        ? AwsBasicCredentials.create(accessKey, secretKey)
        : AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
  }
}
