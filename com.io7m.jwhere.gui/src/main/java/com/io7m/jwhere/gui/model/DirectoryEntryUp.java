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

import com.io7m.jnull.NullCheck;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDiskID;

/**
 * A directory entry in a given disk that corresponds to a link to a parent
 * directory.
 */

public final class DirectoryEntryUp implements DirectoryEntryType
{
  private final CatalogDiskID        disk_index;
  private final CatalogDirectoryNode node;
  private final String               name;
  private final boolean              is_root;

  /**
   * Construct a directory entry.
   *
   * @param in_disk_index The index of the disk in which the directory exists
   * @param in_name       The name of the entry
   * @param in_node       The actual directory node
   * @param in_is_root    The directory is the root directory
   */

  public DirectoryEntryUp(
    final CatalogDiskID in_disk_index,
    final String in_name,
    final CatalogDirectoryNode in_node,
    final boolean in_is_root)
  {
    this.disk_index = NullCheck.notNull(in_disk_index);
    this.name = NullCheck.notNull(in_name);
    this.node = NullCheck.notNull(in_node);
    this.is_root = in_is_root;
  }

  /**
   * @return {@code true} iff this directory is the root directory
   */

  public boolean isRoot()
  {
    return this.is_root;
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

  public CatalogDirectoryNode getNode()
  {
    return this.node;
  }

  @Override public <A, E extends Exception> A matchEntry(
    final DirectoryEntryMatcherType<A, E> m)
    throws E
  {
    return m.onUp(this);
  }

  @Override public String getName()
  {
    return this.name;
  }
}
