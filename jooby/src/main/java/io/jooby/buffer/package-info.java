/**
 * Generic abstraction for working with byte buffer implementations. Copy from
 * https://github.com/spring-projects/spring-framework/tree/main/spring-core/src/main/java/org/springframework/core/io/buffer.
 * - Copy all package inside io.jooby.byffer - remove all Assert/ObjectUtils import references -
 * remove Netty5DataBuffer references. - replace reactive stream classes references by JDK reactive
 * streams. - DataBufferUtils is a limited version of original - remove Deprecated methods
 */
package io.jooby.buffer;
