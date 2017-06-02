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
package org.jooby.aws;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.internal.aws.AwsShutdownSupport;
import org.jooby.internal.aws.ConfigCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.inject.Binder;
import com.typesafe.config.Config;

/**
 * <h1>aws module</h1>
 * <p>
 * Small utility module that exposes {@link AmazonWebServiceClient} and give you access to aws
 * credentials (access and secret keys).
 * </p>
 *
 * <h2>exposes</h2>
 * <ul>
 * <li>One ore more {@link AmazonWebServiceClient}</li>
 * </ul>
 *
 * <h2>usage</h2>
 *
 * application.conf:
 *
 * <pre>
 * aws.accessKey = AKIAIOSFODNN7EXAMPLE
 * aws.secretKey =  wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
 * </pre>
 *
 * <pre>
 * {
 *   use(new Aws()
 *     .with(creds {@literal ->} new AmazonS3Client(creds))
 *     .with(creds {@literal ->} new AmazonSimpleEmailServiceClient(creds))
 *   );
 *
 *   get("/", req {@literal ->} {
 *     AmazonS3 s3 = req.require(AmazonS3.class);
 *     // work with s3
 *   });
 * }
 * </pre>
 *
 * <p>
 * Keep in mind, you will need the <code>s3 (ses, sqs,sns, etc..)</code> jar in your classpath.
 * </p>
 *
 * <p>
 * This module is small and simple. All it does is bind {@link AmazonWebServiceClient} instances in
 * Guice. It also helps to bind utility classes like <code>TransferManager</code>.
 * </p>
 *
 * <pre>
 * {
 *   use(new Aws()
 *     .with(creds {@literal ->} new AmazonS3Client(creds))
 *     .doWith((AmazonS3Client s3) {@literal ->} new TransferManager(s3))
 *   );
 *
 *   post("/", req {@literal ->} {
 *     TransferMananger tm = require(TransferManager.class);
 *   });
 * }
 * </pre>
 *
 * <h2>handling access and secret keys</h2>
 * <p>
 * Keys are defined in <code>.conf</code> file. It is possible to use global or per service keys.
 * </p>
 *
 * <p>
 * Here is an example of global keys:
 * </p>
 *
 * <pre>
 *  aws.accessKey = AKIAIOSFODNN7EXAMPLE
 *  aws.secretKey =  wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
 *
 *  aws.s3.accessKey = S3IOSFODNN7S3EXAMPLE
 *  aws.s3.secretKey = s3alrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
 * </pre>
 *
 * <pre>
 * {
 *   use(new Aws()
 *     .with(creds {@literal ->} new AmazonS3Client(creds)) // use aws.s3 keys
 *     .with(creds {@literal ->} new AmazonSimpleEmailServiceClient(creds)) // use global keys
 *   );
 * </pre>
 *
 * It uses the {@link AmazonWebServiceClient#getServiceName()} method in order to find per service
 * keys.
 *
 * @author edgar
 * @since 0.7.0
 */
@SuppressWarnings({"rawtypes", "unchecked" })
public class Aws implements Jooby.Module {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Builder<BiFunction<AWSCredentialsProvider, Config, AmazonWebServiceClient>> callbacks =
      ImmutableList.builder();

  private List<BiFunction> after = new ArrayList<>();

  /**
   * Bind an {@link AmazonWebServiceClient} instances as Guice service.
   *
   * <pre>
   * {
   *   use(new Aws()
   *     .with(creds {@literal ->} new AmazonS3Client(creds))
   *   );
   * </pre>
   *
   * @param callback A creation callback.
   * @return This module.
   */
  public Aws with(
      final BiFunction<AWSCredentialsProvider, Config, AmazonWebServiceClient> callback) {
    requireNonNull(callback, "Callback is required.");
    callbacks.add(callback);
    return this;
  }

  /**
   * Bind an {@link AmazonWebServiceClient} instances as Guice service.
   *
   * <pre>
   * {
   *   use(new Aws()
   *     .with((creds, conf) {@literal ->} {
   *       AmazonS3Client s3 = new AmazonS3Client(creds);
   *       s3.setXXX(conf.getString("XXXX"));
   *       return s3;
   *     })
   *   );
   * </pre>
   *
   * @param callback A creation callback.
   * @return This module.
   */
  public Aws with(final Function<AWSCredentialsProvider, AmazonWebServiceClient> callback) {
    return with((creds, conf) -> callback.apply(creds));
  }

  /**
   * Like {@link #with(BiFunction)} but it depends on a previously created service.
   *
   * <pre>
   * {
   *   use(new Aws()
   *     .with(creds {@literal ->} new AmazonS3Client(creds))
   *     .with(creds {@literal ->} new AmazonSQSClient(creds))
   *     .doWith((AmazonS3Client s3) {@literal ->} new TransferManager(s3))
   *   );
   * </pre>
   *
   * It will bind a <code>TransferManager</code> as a Guice service.
   *
   * @param callback A creation callback.
   * @param <T> Aws service type.
   * @return This module.
   */
  public <T extends AmazonWebServiceClient> Aws doWith(
      final BiFunction<T, Config, Object> callback) {
    requireNonNull(callback, "Callback is required.");
    after.add(callback);
    return this;
  }

  /**
   * Like {@link #with(Function)} but it depends on a previously created service.
   *
   * <pre>
   * {
   *   use(new Aws()
   *     .with(creds {@literal ->} new AmazonS3Client(creds))
   *     .with(creds {@literal ->} new AmazonSQSClient(creds))
   *     .doWith((AmazonS3Client s3) {@literal ->} new TransferManager(s3))
   *   );
   * </pre>
   *
   * It will bind a <code>TransferManager</code> as a Guice service.
   *
   * @param callback A creation callback.
   * @param <T> Aws service type.
   * @return This module.
   */
  public <T extends AmazonWebServiceClient> Aws doWith(final Function<T, Object> callback) {
    requireNonNull(callback, "Callback is required.");
    return doWith((s, c) -> callback.apply((T) s));
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {

    callbacks.build().forEach(it -> {
      ConfigCredentialsProvider creds = new ConfigCredentialsProvider(config);
      AmazonWebServiceClient service = it.apply(creds, config);
      creds.service(service.getServiceName());
      Class serviceType = service.getClass();
      Class[] interfaces = serviceType.getInterfaces();
      if (interfaces.length > 0) {
        // pick first
        binder.bind(interfaces[0]).toInstance(service);
      }
      binder.bind(serviceType).toInstance(service);
      env.onStop(new AwsShutdownSupport(service));
      after(env, binder, config, service);
    });
  }

  private void after(final Env env, final Binder binder, final Config config,
      final AmazonWebServiceClient service) {
    after.forEach(it -> {
      try {
        Object dep = it.apply(service, config);
        requireNonNull(dep, "A nonnull value is required.");
        Class type = dep.getClass();
        binder.bind(type).toInstance(dep);
        env.onStop(new AwsShutdownSupport(dep));
      } catch (ClassCastException ex) {
        log.debug("ignoring callback {}", it);
      }
    });
  }

}
