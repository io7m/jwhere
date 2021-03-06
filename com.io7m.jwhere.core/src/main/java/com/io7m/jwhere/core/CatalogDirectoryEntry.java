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

import com.io7m.jaffirm.core.Preconditions;
import net.jcip.annotations.Immutable;

import java.util.Objects;

/**
 * A directory entry.
 */

@Immutable
public final class CatalogDirectoryEntry
{
  private final CatalogDirectoryNode source;
  private final String name;
  private final CatalogNodeType target;

  /**
   * Construct a directory entry.
   *
   * @param in_parent The parent directory
   * @param in_child  The new child node
   * @param in_name   The name of the child node, which must be unique within the parent directory
   */

  public CatalogDirectoryEntry(
    final CatalogDirectoryNode in_parent,
    final CatalogNodeType in_child,
    final String in_name)
  {
    this.target = Objects.requireNonNull(in_child, "in_child");
    this.name = Objects.requireNonNull(in_name, "in_name");
    this.source = Objects.requireNonNull(in_parent, "in_parent");

    Preconditions.checkPreconditionV(
      !in_name.isEmpty(),
      "Filenames cannot be empty");
    Preconditions.checkPreconditionV(
      !in_name.contains("/"),
      "Filenames cannot contain '/' (U+002F)");
  }

  /**
   * @return The name of the entry
   */

  public String getName()
  {
    return this.name;
  }

  /**
   * @return The parent directory
   */

  public CatalogDirectoryNode getSource()
  {
    return this.source;
  }

  @Override
  public String toString()
  {
    final var sb = new StringBuilder("CatalogDirectoryEntry{");
    sb.append("name='").append(this.name).append('\'');
    sb.append(", source=").append(this.source);
    sb.append(", target=").append(this.target);
    sb.append('}');
    return sb.toString();
  }

  /**
   * @return The entry node
   */

  public CatalogNodeType getTarget()
  {
    return this.target;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || !Objects.equals(this.getClass(), o.getClass())) {
      return false;
    }

    final var that = (CatalogDirectoryEntry) o;
    return this.source.equals(that.source)
      && this.name.equals(that.name)
      && this.target.equals(that.target);
  }

  @Override
  public int hashCode()
  {
    var result = this.source.hashCode();
    result = 31 * result + this.name.hashCode();
    result = 31 * result + this.target.hashCode();
    return result;
  }
}
