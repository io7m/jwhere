/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.jwhere.core;

import com.io7m.junreachable.UnreachableCodeException;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Functions to construct file hashes.
 */

public final class CatalogFileHashes
{
  private CatalogFileHashes()
  {

  }

  /**
   * Produce a hash from the given file.
   *
   * @param file The file
   *
   * @return A hash value
   *
   * @throws IOException On I/O errors
   */

  public static CatalogFileHash fromFile(final Path file)
    throws IOException
  {
    try {
      return fromFileWithDigest(MessageDigest.getInstance("SHA-256"), file);
    } catch (final NoSuchAlgorithmException e) {
      throw new UnreachableCodeException(e);
    }
  }

  /**
   * Produce a hash from the given file.
   *
   * @param file The file
   * @param md   The message digest
   *
   * @return A hash value
   *
   * @throws IOException On I/O errors
   */

  public static CatalogFileHash fromFileWithDigest(
    final MessageDigest md,
    final Path file)
    throws IOException
  {
    final byte[] data = new byte[8192];

    try (InputStream is = Files.newInputStream(file)) {
      while (true) {
        final int r = is.read(data);
        if (r == -1) {
          break;
        }
        md.update(data, 0, r);
      }
    }

    final String hex = Hex.encodeHexString(md.digest()).toUpperCase();
    return CatalogFileHash.builder()
      .setAlgorithm(md.getAlgorithm())
      .setValue(hex)
      .build();
  }
}
