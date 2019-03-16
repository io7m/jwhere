/*
 * Copyright © 2015 <code@io7m.com> http://io7m.com
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

import java.util.Objects;
import com.io7m.junreachable.UnreachableCodeException;
import net.jcip.annotations.Immutable;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A hash value.
 */

@Immutable public final class CatalogFileHash
{
  private final String algorithm;
  private final String value;

  /**
   * Construct a hash value.
   *
   * @param in_algorithm The algorithm used (eg 'SHA2-512')
   * @param in_value     The hash value
   */

  public CatalogFileHash(
    final String in_algorithm,
    final String in_value)
  {
    this.value = Objects.requireNonNull(in_value, "in_value");
    this.algorithm = Objects.requireNonNull(in_algorithm, "in_algorithm");
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
      return CatalogFileHash.fromFileWithDigest(
        MessageDigest.getInstance(
          "SHA-256"), file);
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

    try (final InputStream is = Files.newInputStream(file)) {
      while (true) {
        final int r = is.read(data);
        if (r == -1) {
          break;
        }
        md.update(data, 0, r);
      }
    }

    final String hex = Hex.encodeHexString(md.digest());
    return new CatalogFileHash(md.getAlgorithm(), hex);
  }

  @Override public String toString()
  {
    return String.format("%s:%s", this.algorithm, this.value);
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final CatalogFileHash that = (CatalogFileHash) o;
    return this.getAlgorithm().equals(that.getAlgorithm()) && this.getValue()
      .equals(that.getValue());
  }

  @Override public int hashCode()
  {
    int result = this.getAlgorithm().hashCode();
    result = 31 * result + this.getValue().hashCode();
    return result;
  }

  /**
   * @return The hash algorithm
   */

  public String getAlgorithm()
  {
    return this.algorithm;
  }

  /**
   * @return The hash value
   */

  public String getValue()
  {
    return this.value;
  }
}
