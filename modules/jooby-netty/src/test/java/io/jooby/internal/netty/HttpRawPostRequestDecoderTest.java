/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.*;

@ExtendWith(MockitoExtension.class)
class HttpRawPostRequestDecoderTest {

  @Mock HttpRequest request;
  @Mock HttpDataFactory factory;
  @Mock Attribute data;

  private HttpRawPostRequestDecoder decoder;

  @BeforeEach
  void setup() {
    when(factory.createAttribute(request, "body")).thenReturn(data);
    decoder = new HttpRawPostRequestDecoder(factory, request);
  }

  @Test
  void testBasicPropertiesAndConfigurations() {
    assertFalse(decoder.isMultipart());

    // Test discard threshold (no-op setter and 0 getter)
    decoder.setDiscardThreshold(1024);
    assertEquals(0, decoder.getDiscardThreshold());
  }

  @Test
  void testGetBodyHttpDatas() {
    List<InterfaceHttpData> datas = decoder.getBodyHttpDatas();
    assertEquals(1, datas.size());
    assertEquals(data, datas.get(0));

    // getBodyHttpDatas(String) delegates directly to getBodyHttpDatas()
    List<InterfaceHttpData> namedDatas = decoder.getBodyHttpDatas("anyName");
    assertEquals(1, namedDatas.size());
    assertEquals(data, namedDatas.get(0));

    // Single item fetch
    assertEquals(data, decoder.getBodyHttpData("anyName"));
  }

  @Test
  void testIteration() {
    assertTrue(decoder.hasNext());
    assertEquals(data, decoder.next());
    assertEquals(data, decoder.currentPartialHttpData());
  }

  @Test
  void testNullDataBranch() {
    // Branch coverage: when factory returns null, lists should be empty and iterators false
    when(factory.createAttribute(request, "body")).thenReturn(null);
    HttpRawPostRequestDecoder nullDecoder = new HttpRawPostRequestDecoder(factory, request);

    assertTrue(nullDecoder.getBodyHttpDatas().isEmpty());
    assertTrue(nullDecoder.getBodyHttpDatas("anyName").isEmpty());
    assertNull(nullDecoder.getBodyHttpData("anyName"));
    assertFalse(nullDecoder.hasNext());
    assertNull(nullDecoder.next());
    assertNull(nullDecoder.currentPartialHttpData());
  }

  @Test
  void testOffer_StandardContent() throws Exception {
    ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {1, 2, 3});
    DefaultHttpContent content = new DefaultHttpContent(buf);

    InterfaceHttpPostRequestDecoder result = decoder.offer(content);

    assertEquals(decoder, result);
    // Verify it copies the buffer and detects it is NOT the last content
    verify(data).addContent(any(ByteBuf.class), eq(false));
  }

  @Test
  void testOffer_LastContent() throws Exception {
    ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {4, 5, 6});
    DefaultLastHttpContent content = new DefaultLastHttpContent(buf);

    InterfaceHttpPostRequestDecoder result = decoder.offer(content);

    assertEquals(decoder, result);
    // Verify it copies the buffer and detects it IS the last content
    verify(data).addContent(any(ByteBuf.class), eq(true));
  }

  @Test
  void testOffer_ThrowsIOException() throws Exception {
    ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {1});
    DefaultHttpContent content = new DefaultHttpContent(buf);

    doThrow(new IOException("Disk write failed"))
        .when(data)
        .addContent(any(ByteBuf.class), anyBoolean());

    // Verify the decoder wraps the checked IOException in the specific Netty RuntimeException
    assertThrows(
        HttpPostRequestDecoder.ErrorDataDecoderException.class, () -> decoder.offer(content));
  }

  @Test
  void testCleanFiles() {
    decoder.cleanFiles();
    verify(factory).cleanRequestHttpData(request);
  }

  @Test
  void testRemoveHttpDataFromClean() {
    InterfaceHttpData mockData = mock(InterfaceHttpData.class);
    decoder.removeHttpDataFromClean(mockData);

    verify(factory).removeHttpDataFromClean(request, mockData);
  }

  @Test
  void testDestroy() {
    decoder.destroy();

    // Verify it delegates to the factory for cleanup and deletes the actual data item
    verify(factory).cleanRequestHttpData(request);
    verify(factory).removeHttpDataFromClean(request, data);
    verify(data).delete();
  }
}
