/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Represents a file download.
 *
 * @author edgar
 * @author imeszaros
 * @since 2.9.0
 */
public class FileDownload {

  /** Download mode. */
  public enum Mode {

    /** Value indicating that the file can be displayed inside the Web page, or as the Web page. */
    INLINE("inline"),

    /**
     * Value indicating that the file should be downloaded; most browsers present a 'Save as'
     * dialog.
     */
    ATTACHMENT("attachment");

    final String value;

    Mode(String value) {
      this.value = value;
    }
  }

  private static final String CONTENT_DISPOSITION = "%s;filename=\"%s\"";
  private static final String FILENAME_STAR = ";filename*=%s''%s";

  private static final String CHARSET = "UTF-8";

  private final long fileSize;

  private final MediaType contentType;

  private final String fileName;

  private final String contentDisposition;

  private final InputStream content;

  private boolean deleteOnComplete;

  private Path file;

  /**
   * Creates a new file attachment.
   *
   * @param mode Download mode.
   * @param content File content.
   * @param fileName Filename.
   * @param fileSize File size or <code>-1</code> if unknown.
   */
  public FileDownload(
      Mode mode, @NonNull InputStream content, @NonNull String fileName, long fileSize) {
    try {
      this.fileName = Paths.get(fileName).getFileName().toString();
      this.contentType = MediaType.byFile(this.fileName);
      String filenameStar = URLEncoder.encode(this.fileName, CHARSET).replaceAll("\\+", "%20");
      if (this.fileName.equals(filenameStar)) {
        this.contentDisposition = String.format(CONTENT_DISPOSITION, mode.value, this.fileName);
      } else {
        this.contentDisposition =
            String.format(CONTENT_DISPOSITION, mode.value, this.fileName)
                + String.format(FILENAME_STAR, CHARSET, filenameStar);
      }
      this.content = content;
      this.fileSize = fileSize;
    } catch (UnsupportedEncodingException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  /**
   * Creates a new file attachment.
   *
   * @param mode Download mode.
   * @param content File content.
   * @param fileName Filename.
   */
  public FileDownload(Mode mode, @NonNull InputStream content, @NonNull String fileName) {
    this(mode, content, fileName, -1);
  }

  /**
   * Creates a new file attachment.
   *
   * @param mode Download mode.
   * @param content File content.
   * @param fileName Filename.
   */
  public FileDownload(Mode mode, @NonNull byte[] content, @NonNull String fileName) {
    this(mode, new ByteArrayInputStream(content), fileName, content.length);
  }

  /**
   * Creates a new file attachment.
   *
   * @param mode Download mode.
   * @param file File content.
   * @param fileName Filename.
   * @throws IOException For IO exception while reading file.
   */
  public FileDownload(Mode mode, @NonNull Path file, @NonNull String fileName) throws IOException {
    this(mode, new FileInputStream(file.toFile()), fileName, Files.size(file));
    this.file = file;
  }

  /**
   * Creates a new file attachment.
   *
   * @param mode Download mode.
   * @param file File content.
   * @throws IOException For IO exception while reading file.
   */
  public FileDownload(Mode mode, @NonNull Path file) throws IOException {
    this(mode, file, file.getFileName().toString());
    this.file = file;
  }

  /**
   * True if the file will be deleted after sending to the client.
   *
   * @return True if the file will be deleted after sending to the client.
   */
  public boolean deleteOnComplete() {
    return deleteOnComplete;
  }

  /**
   * Get the underlying file or <code>null</code>.
   *
   * @return Get the underlying file or <code>null</code>.
   */
  public @Nullable Path getFile() {
    return file;
  }

  /**
   * File size or <code>-1</code> if unknown.
   *
   * @return File size or <code>-1</code> if unknown.
   */
  public long getFileSize() {
    return fileSize;
  }

  /**
   * File content type.
   *
   * @return File content type.
   */
  public MediaType getContentType() {
    return contentType;
  }

  /**
   * File name.
   *
   * @return File name.
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Content disposition header.
   *
   * @return Content disposition header.
   */
  public String getContentDisposition() {
    return contentDisposition;
  }

  /**
   * File content.
   *
   * @return File content.
   */
  public InputStream stream() {
    return content;
  }

  @Override
  public String toString() {
    return fileName;
  }

  /** Allows creating a {@link FileDownload} with the specified {@link Mode}. */
  public interface Builder {

    /**
     * Creates a {@link FileDownload} with the specified {@link Mode}.
     *
     * @param mode the {@link Mode}
     * @return a {@link FileDownload} with the specified mode
     */
    FileDownload build(Mode mode);

    /**
     * Creates an attached {@link FileDownload}.
     *
     * @return a {@link FileDownload} with {@link Mode#ATTACHMENT}
     */
    default FileDownload attachment() {
      return build(Mode.ATTACHMENT);
    }

    /**
     * Creates an inline {@link FileDownload}.
     *
     * @return a {@link FileDownload} with {@link Mode#INLINE}
     */
    default FileDownload inline() {
      return build(Mode.INLINE);
    }
  }

  /**
   * Add support for delete file on complete when created from {@link #build(Path, String)} or
   * {@link #build(Path)}.
   */
  public interface BuilderExt extends Builder {

    /**
     * Mark the file to be deleted once it send to the client. This options works on file created
     * within a {@link Path} instance.
     *
     * @return This builder.
     */
    Builder deleteOnComplete();
  }

  /**
   * Creates a builder with the specified content which can be used to create a {@link FileDownload}
   * with any {@link Mode}.
   *
   * @param content File content.
   * @param fileName Filename.
   * @param fileSize File size or <code>-1</code> if unknown.
   * @return a {@link Builder} with the specified content
   */
  public static Builder build(
      @NonNull InputStream content, @NonNull String fileName, long fileSize) {
    return mode -> new FileDownload(mode, content, fileName, fileSize);
  }

  /**
   * Creates a builder with the specified content which can be used to create a {@link FileDownload}
   * with any {@link Mode}.
   *
   * @param content File content.
   * @param fileName Filename.
   * @return a {@link Builder} with the specified content
   */
  public static Builder build(@NonNull InputStream content, @NonNull String fileName) {
    return mode -> new FileDownload(mode, content, fileName);
  }

  /**
   * Creates a builder with the specified content which can be used to create a {@link FileDownload}
   * with any {@link Mode}.
   *
   * @param content File content.
   * @param fileName Filename.
   * @return a {@link Builder} with the specified content
   */
  public static Builder build(@NonNull byte[] content, @NonNull String fileName) {
    return mode -> new FileDownload(mode, content, fileName);
  }

  /**
   * Creates a builder with the specified content which can be used to create a {@link FileDownload}
   * with any {@link Mode}.
   *
   * @param file File content.
   * @param fileName Filename.
   * @return a {@link Builder} with the specified content
   */
  public static BuilderExt build(@NonNull Path file, @NonNull String fileName) {
    return new BuilderExt() {
      private boolean deleteOnComplete;

      @Override
      public FileDownload build(Mode mode) {
        try {
          var output = new FileDownload(mode, file, fileName);
          output.deleteOnComplete = deleteOnComplete;
          return output;
        } catch (IOException e) {
          throw SneakyThrows.propagate(e);
        }
      }

      @Override
      public Builder deleteOnComplete() {
        this.deleteOnComplete = true;
        return this;
      }
    };
  }

  /**
   * Creates a builder with the specified content which can be used to create a {@link FileDownload}
   * with any {@link Mode}.
   *
   * @param file File content.
   * @return a {@link Builder} with the specified content
   */
  public static BuilderExt build(@NonNull Path file) {
    return build(file, file.getFileName().toString());
  }
}
