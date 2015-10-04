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

import java.math.BigInteger;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Set;

/**
 * The type of filesystem nodes.
 */

public interface CatalogNodeType
{
  /**
   * Match a node by type.
   *
   * @param m   The matcher
   * @param <A> The type of returned values
   * @param <E> The type of raised exceptions
   *
   * @return The value returned by the matcher
   *
   * @throws E Iff the matcher raises {@code E}
   */

  <A, E extends Exception> A matchNode(CatalogNodeMatcherType<A, E> m)
    throws E;

  /**
   * @return The node's POSIX file permissions
   */

  Set<PosixFilePermission> getPermissions();

  /**
   * @return The node's owner
   */

  String getOwner();

  /**
   * @return The node's group
   */

  String getGroup();

  /**
   * @return The node's filesystem inode value
   */

  BigInteger getINode();

  /**
   * @return The node's time of creation
   */

  Instant getCreationTime();

  /**
   * @return The node's time of most recent modification
   */

  Instant getModificationTime();

  /**
   * @return The node's time of most recent access
   */

  Instant getAccessTime();
}
