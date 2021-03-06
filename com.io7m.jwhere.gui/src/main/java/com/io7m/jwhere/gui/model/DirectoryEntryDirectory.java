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

package com.io7m.jwhere.gui.model;

import com.io7m.jwhere.core.CatalogDirectoryNodeType;
import com.io7m.jwhere.core.CatalogDiskID;

import java.util.Objects;

/**
 * A directory entry in a given disk that corresponds to a directory.
 */

public final class DirectoryEntryDirectory implements DirectoryEntryType
{
  private final CatalogDiskID disk_index;
  private final CatalogDirectoryNodeType node;
  private final String name;

  /**
   * Construct an entry.
   *
   * @param in_disk_index The index of the disk in which the directory exists
   * @param in_name       The name of the entry
   * @param in_node       The actual directory node
   */

  public DirectoryEntryDirectory(
    final CatalogDiskID in_disk_index,
    final String in_name,
    final CatalogDirectoryNodeType in_node)
  {
    this.disk_index = Objects.requireNonNull(in_disk_index, "in_disk_index");
    this.name = Objects.requireNonNull(in_name, "in_name");
    this.node = Objects.requireNonNull(in_node, "in_node");
  }

  /**
   * @return The index of the disk
   */

  public CatalogDiskID getDiskIndex()
  {
    return this.disk_index;
  }

  /**
   * @return The actual directory node
   */

  public CatalogDirectoryNodeType getNode()
  {
    return this.node;
  }

  @Override
  public <A, E extends Exception> A matchEntry(
    final DirectoryEntryMatcherType<A, E> m)
    throws E
  {
    return m.onDirectory(this);
  }

  @Override
  public String getName()
  {
    return this.name;
  }
}
