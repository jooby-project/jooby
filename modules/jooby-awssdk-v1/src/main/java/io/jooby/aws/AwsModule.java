/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.WebIdentityTokenCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.typesafe.config.Config;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.internal.aws.ConfigCredentialsProvider;
import io.jooby.internal.aws.ServiceShutdown;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Aws module for aws-java-sdk 1.x. This module:
 *
 * - Integrates AWS credentials within application properties.
 * - Register AWS services as application services (so they can be used by require calls or DI).
 * - Add graceful shutdown to AWS services.
 *
 * Usage:
 *
 * <pre>{@code
 * {
 *     install(
 *         new AwsModule()
 *             .setup(credentials -> {
 *               return TransferManagerBuilder.standard()
 *                   .withS3Client(
 *                       AmazonS3ClientBuilder.standard()
 *                           .withRegion(Regions.US_EAST_1)
 *                           .withCredentials(credentials)
 *                           .build()
 *                   ).build();
 *             })
 *     );
 * }
 * }</pre>
 *
 * <p>Previous example register AmazonS3Client and TransferManager services</p>
 *
 * <p>NOTE: You need to add the required service dependency to your project.</p>
 *
 * @author edgar
 */
public class AwsModule implements Extension {

  private final List<Function<AWSCredentialsProvider, Object>> factoryList = new ArrayList<>();

  /**
   * Setup a new AWS service. Supported outputs are:
   *
   * - Single amazon service
   * - Stream of amazon services
   * - Collection of amazon services
   *
   * Each of the services returned by this function are added to the application service registry
   * and shutdown at application shutdown time.
   *
   * @param provider Service provider/factory.
   * @return AWS service.
   */
  public @Nonnull AwsModule setup(@Nonnull Function<AWSCredentialsProvider, Object> provider) {
    factoryList.add(provider);
    return this;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    AWSCredentialsProvider credentialsProvider = newCredentialsProvider(application.getConfig());
    List<Object> serviceList = new ArrayList<>(factoryList.size());
    for (Function<AWSCredentialsProvider, Object> factory : factoryList) {
      Object value = factory.apply(credentialsProvider);
      if (value instanceof Stream) {
        ((Stream) value).forEach(serviceList::add);
      } else if (value instanceof Collection) {
        ((Collection) value).forEach(serviceList::add);
      } else {
        extractServices(value, serviceList::add);
      }
    }
    ServiceRegistry services = application.getServices();
    // for each service
    for (Object service : serviceList) {
      Stream.of(service.getClass(),
          Stream.of(service.getClass().getInterfaces()).findFirst().orElse(null))
          .filter(Objects::nonNull)
          .forEach(serviceType -> {
            services.putIfAbsent((Class) serviceType, service);
          });
    }
    serviceList.stream()
        .distinct()
        .forEach(service -> application.onStop(new ServiceShutdown(application.getLog(), service)));
    serviceList.clear();
    factoryList.clear();
  }

  /**
   * Creates a credentials provider, exactly like
   * {@link com.amazonaws.auth.DefaultAWSCredentialsProviderChain} appending the application
   * properties provider.
   *
   * @param config Application properties.
   * @return Credentials provider.
   */
  public static @Nonnull AWSCredentialsProvider newCredentialsProvider(@Nonnull Config config) {
    return new AWSCredentialsProviderChain(
        new EnvironmentVariableCredentialsProvider(),
        new SystemPropertiesCredentialsProvider(),
        WebIdentityTokenCredentialsProvider.create(),
        new ProfileCredentialsProvider(),
        new EC2ContainerCredentialsProviderWrapper(),
        // Application configuration
        new ConfigCredentialsProvider(config)
    );
  }

  private void extractServices(Object value, Consumer<Object> consumer) {
    transferManager(value, consumer);
    consumer.accept(value);
  }

  private void transferManager(Object value, Consumer<Object> consumer) {
    try {
      if (value instanceof TransferManager) {
        consumer.accept(((TransferManager) value).getAmazonS3Client());
      }
    } catch (NoClassDefFoundError x) {
      // s3 dependency is optional
    }
  }
}
