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
import org.valid4j.Assertive;

import java.nio.file.Path;

/**
 * An error indicating that a filesystem object has changed type.
 */

public final class CatalogVerificationChangedType
  implements CatalogVerificationReportItemErrorType
{
  private final Path            path;
  private final CatalogNodeType node;
  private final CatalogNodeType node_now;

  /**
   * Construct an error.
   *
   * @param in_path     The path
   * @param in_node     The old node
   * @param in_node_now The new node
   */

  public CatalogVerificationChangedType(
    final Path in_path,
    final CatalogNodeType in_node,
    final CatalogNodeType in_node_now)
  {
    this.path = Objects.requireNonNull(in_path, "in_path");
    this.node = Objects.requireNonNull(in_node, "in_node");
    this.node_now = Objects.requireNonNull(in_node_now, "in_node_now");

    Assertive.require(!this.node.getClass().equals(this.node_now.getClass()));
  }

  private static String type(final CatalogNodeType node)
  {
    return node.matchNode(
      new CatalogNodeMatcherType<String, UnreachableCodeException>()
      {
        @Override public String onFile(final CatalogFileNode f)
        {
          return "file";
        }

        @Override public String onDirectory(final CatalogDirectoryNode d)
        {
          return "directory";
        }
      });
  }

  @Override public Path getPath()
  {
    return this.path;
  }

  @Override public String show()
  {
    return String.format(
      "Node was previously a %s but is now a %s",
      CatalogVerificationChangedType.type(this.node),
      CatalogVerificationChangedType.type(
        this.node_now));
  }
}
