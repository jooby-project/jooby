/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.*;
import io.jooby.output.Output;

public class HeadContextTest {

  private Context delegate;
  private HeadContext head;

  @BeforeEach
  void setUp() {
    delegate = mock(Context.class);
    head = new HeadContext(delegate);
  }

  @Test
  void sendPath() throws IOException {
    Path path = Files.createTempFile("head-test", ".txt");
    Files.write(path, "hello".getBytes());
    try {
      head.send(path);
      verify(delegate).setResponseLength(5);
      verify(delegate).setResponseType(MediaType.text);
      verify(delegate).send(StatusCode.OK);
    } finally {
      Files.delete(path);
    }
  }

  @Test
  void sendPathError() throws IOException {
    Path path = mock(Path.class);
    FileSystem fs = mock(FileSystem.class);
    when(path.getFileSystem()).thenReturn(fs);

    assertThrows(RuntimeException.class, () -> head.send(path));
  }

  @Test
  void sendBytes() {
    byte[] data = new byte[10];
    head.send(data);
    verify(delegate).setResponseLength(10);
    verify(delegate).removeResponseHeader("Transfer-Encoding");
    verify(delegate).send(StatusCode.OK);
  }

  @Test
  void sendString() {
    head.send("hello");
    verify(delegate).setResponseLength(5);
    verify(delegate).send(StatusCode.OK);
  }

  @Test
  void sendByteBuffer() {
    ByteBuffer buffer = ByteBuffer.allocate(10);
    buffer.put(new byte[3]);
    buffer.flip();
    head.send(buffer);
    verify(delegate).setResponseLength(3);
    verify(delegate).send(StatusCode.OK);
  }

  @Test
  void sendOutput() {
    Output output = mock(Output.class);
    when(output.size()).thenReturn(100);
    head.send(output);
    verify(delegate).setResponseLength(100L);
    verify(delegate).send(StatusCode.OK);
  }

  @Test
  void sendFileChannel() throws IOException {
    FileChannel channel = mock(FileChannel.class);
    when(channel.size()).thenReturn(50L);
    head.send(channel);
    verify(delegate).setResponseLength(50L);
    verify(delegate).send(StatusCode.OK);
  }

  @Test
  void sendFileChannelError() throws IOException {
    FileChannel channel = mock(FileChannel.class);
    when(channel.size()).thenThrow(new IOException());
    assertThrows(IOException.class, () -> head.send(channel));
  }

  @Test
  void sendFileDownload() {
    FileDownload download = mock(FileDownload.class);
    when(download.getFileSize()).thenReturn(200L);
    when(download.getContentType()).thenReturn(MediaType.json);
    head.send(download);
    verify(delegate).setResponseLength(200L);
    verify(delegate).setResponseType(MediaType.json);
    verify(delegate).send(StatusCode.OK);
  }

  @Test
  void sendInputStream() {
    InputStream stream = mock(InputStream.class);
    when(delegate.getResponseLength()).thenReturn(-1L);
    head.send(stream);
    verify(delegate).setResponseHeader("Transfer-Encoding", "chunked");
    verify(delegate).send(StatusCode.OK);
  }

  @Test
  void sendStatusCode() {
    head.send(StatusCode.NOT_FOUND);
    verify(delegate).send(StatusCode.NOT_FOUND);
  }

  @Test
  void sendReadableByteChannel() {
    ReadableByteChannel channel = mock(ReadableByteChannel.class);
    when(delegate.getResponseLength()).thenReturn(-1L);
    head.send(channel);
    verify(delegate).setResponseHeader("Transfer-Encoding", "chunked");
    verify(delegate).send(StatusCode.OK);
  }

  @Test
  void render() throws Exception {
    Route route = mock(Route.class);
    MessageEncoder encoder = mock(MessageEncoder.class);
    when(delegate.getRoute()).thenReturn(route);
    when(route.getEncoder()).thenReturn(encoder);

    var output = mock(Output.class);
    Object value = new Object();
    when(encoder.encode(head, value)).thenReturn(output);

    head.render(value);
  }

  @Test
  void renderNull() throws Exception {
    Route route = mock(Route.class);
    MessageEncoder encoder = mock(MessageEncoder.class);
    when(delegate.getRoute()).thenReturn(route);
    when(route.getEncoder()).thenReturn(encoder);
    when(delegate.isResponseStarted()).thenReturn(false);

    assertThrows(IllegalStateException.class, () -> head.render(new Object()));
  }

  @Test
  void responseStreamsAndWriters() throws IOException {
    // Stream
    OutputStream os = head.responseStream();
    verify(delegate).send(StatusCode.OK);
    os.write(1);
    os.write(new byte[1]);
    os.write(new byte[1], 0, 1);

    // Writer
    PrintWriter writer = head.responseWriter();
    assertNotNull(writer);
    writer.write("test");

    // Sender
    Sender sender = head.responseSender();
    assertNotNull(sender);
    sender.write(new byte[0], (ws, cause) -> {});
    sender.write(mock(Output.class), (ws, cause) -> {});
    sender.close();
  }

  @Test
  void checkSizeHeadersLogic() {
    // Chunked
    when(delegate.getResponseLength()).thenReturn(-1L);
    head.send(mock(InputStream.class));
    verify(delegate).setResponseHeader("Transfer-Encoding", "chunked");

    // Fixed size
    when(delegate.getResponseLength()).thenReturn(100L);
    head.send(new byte[100]);
    verify(delegate).removeResponseHeader("Transfer-Encoding");
  }
}
