/**
 * Aws module for aws-java-sdk 2.x. This module:
 *
 * <p>- Integrates AWS credentials within application properties.
 *
 * <p>- Register AWS services as application services (so they can be used by require calls or DI).
 *
 * <p>- Add graceful shutdown to any {@link software.amazon.awssdk.utils.SdkAutoCloseable} instance.
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
@ReturnValuesAreNonnullByDefault
package io.jooby.awssdkv2;

import edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault;
