/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represent a file attachment response.
 *
 * @author edgar
 * @since 2.0.0
 */
public class AttachedFile {

  private static final String CONTENT_DISPOSITION = "attachment;filename=\"%s\"";
  private static final String FILENAME_STAR = ";filename*=%s''%s";

  private static final String CHARSET = "UTF-8";

  private final long fileSize;

  private final MediaType contentType;

  private String fileName;

  private String contentDisposition;

  private InputStream content;

  /**
   * Creates a new file attachment.
   *
   * @param content File content.
   * @param fileName Filename.
   * @param fileSize File size or <code>-1</code> if unknown.
   */
  public AttachedFile(@Nonnull InputStream content, @Nonnull String fileName, long fileSize) {
    try {
      this.fileName = FilenameUtils.getName(fileName);
      this.contentType = MediaType.byFile(this.fileName);
      String filenameStar = URLEncoder.encode(this.fileName, CHARSET).replaceAll("\\+", "%20");
      if (this.fileName.equals(filenameStar)) {
        this.contentDisposition = String.format(CONTENT_DISPOSITION, this.fileName);
      } else {
        this.contentDisposition = String.format(CONTENT_DISPOSITION, this.fileName) + String
            .format(FILENAME_STAR, CHARSET, filenameStar);
      }
      this.content = content;
      this.fileSize = fileSize;
    } catch (UnsupportedEncodingException x) {
      throw Sneaky.propagate(x);
    }
  }

  /**
   * Creates a new file attachment.
   *
   * @param content File content.
   * @param fileName Filename.
   */
  public AttachedFile(@Nonnull InputStream content, @Nonnull String fileName) {
    this(content, fileName, -1);
  }

  /**
   * Creates a new file attachment.
   *
   * @param file File content.
   * @param fileName Filename.
   * @throws IOException For IO exception while reading file.
   */
  public AttachedFile(@Nonnull Path file, @Nonnull String fileName) throws IOException {
    this(new FileInputStream(file.toFile()), fileName, Files.size(file));
  }

  /**
   * Creates a new file attachment.
   *
   * @param file File content.
   * @throws IOException For IO exception while reading file.
   */
  public AttachedFile(@Nonnull Path file) throws IOException {
    this(file, file.getFileName().toString());
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

  @Override public String toString() {
    return fileName;
  }
}
