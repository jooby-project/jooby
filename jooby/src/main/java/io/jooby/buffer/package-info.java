/*
 * This file is part of Jooby.
 *
 * It is derived from Spring Framework, originally available at: https://github.com/spring-projects/spring-framework
 *
 * Spring Framework is licensed under the Apache License, Version 2.0.
 *
 * Modifications:
 * - Code live inside of io.jooby.buffer package
 * - Added DataBuffer.duplicate
 * - Added DataBufferFactory.wrap(byte[] bytes, int offset, int length)
 *
 * Jooby is also licensed under the Apache License, Version 2.0.
 *
 * See the LICENSE file in the root of this repository for details.
 */

/**
 * Generic abstraction for working with byte buffer implementations.
 *
 * <p>Copy from
 * https://github.com/spring-projects/spring-framework/tree/main/spring-core/src/main/java/org/springframework/core/io/buffer.
 *
 * <ul>
 *   <li>Copy all package inside io.jooby.byffer
 *   <li>remove all Assert/ObjectUtils import references
 *   <li>remove Netty5DataBuffer references
 *   <li>replace reactive stream classes references by JDK reactive streams
 *   <li>DataBufferUtils is a limited version of original - remove Deprecated methods
 *   <li>Remove all deprecated since 6.0 from DataBuffer and DataBufferFactory
 * </ul>
 */
@edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault
package io.jooby.buffer;
