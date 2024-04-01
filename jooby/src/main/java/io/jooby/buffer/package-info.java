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
package io.jooby.buffer;
