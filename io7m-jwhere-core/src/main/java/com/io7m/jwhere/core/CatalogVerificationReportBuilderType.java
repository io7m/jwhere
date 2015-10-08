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

import java.nio.file.Path;

/**
 * The type of mutable builders for verification reports.
 */

public interface CatalogVerificationReportBuilderType
{
  /**
   * Mark an item as being verified.
   *
   * @param path The path
   */

  void addItemOK(Path path);

  /**
   * @return A verification report based on all data given so far
   */

  CatalogVerificationReport build();

  /**
   * @param path The path
   *
   * @return {@code true} iff a path has been referenced
   */

  boolean pathIsReferenced(Path path);

  /**
   * Mark an item as being uncatalogued. That is, the item has appeared more
   * recently than the catalog was created.
   *
   * @param path The path
   */

  void addItemUncatalogued(Path path);

  /**
   * Mark an item as having changed type (from file to directory, or vice
   * versa).
   *
   * @param path     The path
   * @param node     The node as it appeared in the catalog
   * @param node_now The node as it appears now
   */

  void addItemChangedType(
    Path path,
    CatalogNodeType node,
    CatalogNodeType node_now);

  /**
   * Mark an item as having changed in some manner.
   *
   * @param path       The path
   * @param field      The metadata field that changed
   * @param value_then The field value as it appeared in the catalog
   * @param value_now  The field value as it appears now
   */

  void addItemMetadataChanged(
    Path path,
    CatalogVerificationChangedMetadata.Field field,
    String value_then,
    String value_now);

  /**
   * Mark an item as having a different hash value to than the one recorded in
   * the catalog.
   *
   * @param path      The path
   * @param hash_then The hash value as it appeared in the catalog
   * @param hash_now  The hash value as it appears now
   */

  void addItemHashChanged(
    Path path,
    CatalogFileHash hash_then,
    CatalogFileHash hash_now);

  /**
   * Mark an item as having disappeared.
   *
   * @param path The path
   */

  void addItemDisappeared(Path path);
}
