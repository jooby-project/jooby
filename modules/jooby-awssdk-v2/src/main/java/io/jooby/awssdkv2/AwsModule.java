/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.awssdkv2;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Aws module for aws-java-sdk 2.x. This module:
 *
 * <p>- Integrates AWS credentials within application properties.
 *
 * <p>- Register AWS services as application services (so they can be used by require calls or DI).
 *
 * <p>- Add graceful shutdown to any {@link SdkAutoCloseable} instance.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *     install(
 *         new AwsModule()
 *             .setup(credentials -> {
 *               var s3 = S3Client.builder().region(Region.US_EAST_1).build();
 *               var s3transfer = S3TransferManager.builder().s3Client(s3).build()
 *               return Stream.of(s3, s3transfer);
 *             })
 *     );
 * }
 * }</pre>
 *
 * <p>Previous example register AmazonS3Client and TransferManager services
 *
 * <p>NOTE: You need to add the service dependencies to your project.
 *
 * @author edgar
 * @since 3.3.1
 */
public class AwsModule implements Extension {
  private final AwsCredentialsProvider credentialsProvider;
  private final List<Function<AwsCredentialsProvider, Object>> factoryList = new ArrayList<>();

  public AwsModule(@NonNull AwsCredentialsProvider credentialsProvider) {
    this.credentialsProvider = credentialsProvider;
  }

  public AwsModule() {
    this.credentialsProvider = null;
  }

  /**
   * Setup a new AWS service. Supported outputs are:
   *
   * <p>- Single amazon service - Stream of amazon services - Collection of amazon services
   *
   * <p>Each of the services returned by this function are added to the application service registry
   * and shutdown at application shutdown time.
   *
   * @param provider Service provider/factory.
   * @return AWS service.
   */
  public @NonNull AwsModule setup(@NonNull Function<AwsCredentialsProvider, Object> provider) {
    factoryList.add(provider);
    return this;
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    var config = application.getConfig();
    var credentialsProvider =
        Optional.ofNullable(this.credentialsProvider)
            .orElseGet(() -> newCredentialsProvider(config));

    var serviceList = new ArrayList<>(factoryList.size());
    for (var factory : factoryList) {
      Object value = factory.apply(credentialsProvider);
      if (value instanceof Stream values) {
        values.forEach(serviceList::add);
      } else if (value instanceof Iterable values) {
        values.forEach(serviceList::add);
      } else {
        serviceList.add(value);
      }
    }
    ServiceRegistry services = application.getServices();
    // for each service
    for (Object service : serviceList) {
      Stream.of(
              service.getClass(),
              Stream.of(service.getClass().getInterfaces()).findFirst().orElse(null))
          .filter(Objects::nonNull)
          .forEach(serviceType -> services.putIfAbsent((Class) serviceType, service));
    }
    serviceList.stream()
        .filter(SdkAutoCloseable.class::isInstance)
        .map(SdkAutoCloseable.class::cast)
        .forEach(application::onStop);
    serviceList.clear();
    factoryList.clear();
  }

  /**
   * Creates a credentials provider, exactly like {@link DefaultCredentialsProvider#create()}
   * appending the application properties provider.
   *
   * @param config Application properties.
   * @return Credentials provider.
   */
  public static @NonNull AwsCredentialsProvider newCredentialsProvider(@NonNull Config config) {
    return AwsCredentialsProviderChain.of(
        DefaultCredentialsProvider.create(), new ConfigCredentialsProvider(config));
  }
}
