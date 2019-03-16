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

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jaffirm.core.Preconditions;
import org.immutables.value.Value;

import java.math.BigInteger;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * A node representing a file in the filesystem.
 */

@ImmutablesStyleType
@Value.Immutable
public interface CatalogFileNodeType extends CatalogNodeType
{
  /**
   * @return The size of the file in bytes
   */

  BigInteger size();

  /**
   * @return The last access time of the directory
   */

  @Override
  Instant accessTime();

  /**
   * @return The creation time of the directory
   */

  @Override
  Instant creationTime();

  /**
   * @return The group
   */

  @Override
  String group();

  /**
   * @return The filesystem ID value
   */

  @Override
  BigInteger id();

  /**
   * @return The last modification time of the directory
   */

  @Override
  Instant modificationTime();

  /**
   * @return The owner
   */

  @Override
  String owner();

  /**
   * @return The directory POSIX permissions
   */

  @Override
  Set<PosixFilePermission> permissions();

  /**
   * @return The file hash
   */

  Optional<CatalogFileHash> hash();

  @Override
  default <A, E extends Exception> A matchNode(
    final CatalogNodeMatcherType<A, E> m)
    throws E
  {
    return m.onFile(this);
  }

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    Preconditions.checkPrecondition(
      this.group(),
      !this.group().isEmpty(),
      g -> "Group name cannot be empty");
    Preconditions.checkPrecondition(
      this.owner(),
      !this.owner().isEmpty(),
      g -> "Owner name cannot be empty");
  }
}
