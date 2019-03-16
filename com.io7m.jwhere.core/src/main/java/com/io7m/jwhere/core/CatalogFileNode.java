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
import net.jcip.annotations.Immutable;
import org.valid4j.Assertive;

import java.math.BigInteger;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * A node representing a file.
 */

@Immutable public final class CatalogFileNode implements CatalogNodeType
{
  private final Set<PosixFilePermission>  permissions;
  private final String                    owner;
  private final String                    group;
  private final BigInteger                id;
  private final Instant                   creation_time;
  private final Instant                   modify_time;
  private final Instant                   access_time;
  private final BigInteger                size;
  private final Optional<CatalogFileHash> hash;

  /**
   * Construct a directory.
   *
   * @param in_size          The size of the file in bytes
   * @param in_permissions   The file POSIX permissions
   * @param in_owner         The owner
   * @param in_group         The group
   * @param in_inode         The filesystem id value
   * @param in_access_time   The time of the most recent access
   * @param in_creation_time The time of creation
   * @param in_modify_time   The time of the most recent modification
   * @param in_hash          An optional hash value
   */

  public CatalogFileNode(
    final BigInteger in_size,
    final Set<PosixFilePermission> in_permissions,
    final String in_owner,
    final String in_group,
    final BigInteger in_inode,
    final Instant in_access_time,
    final Instant in_creation_time,
    final Instant in_modify_time,
    final Optional<CatalogFileHash> in_hash)
  {
    this.access_time = Objects.requireNonNull(in_access_time, "in_access_time");
    this.permissions = Objects.requireNonNull(in_permissions, "in_permissions");
    this.owner = Objects.requireNonNull(in_owner, "in_owner");
    this.group = Objects.requireNonNull(in_group, "in_group");
    this.id = Objects.requireNonNull(in_inode, "in_inode");
    this.creation_time = Objects.requireNonNull(in_creation_time, "in_creation_time");
    this.modify_time = Objects.requireNonNull(in_modify_time, "in_modify_time");
    this.size = Objects.requireNonNull(in_size, "in_size");
    this.hash = Objects.requireNonNull(in_hash, "in_hash");

    Assertive.require(!this.owner.isEmpty(), "Owner name cannot be empty");
    Assertive.require(!this.group.isEmpty(), "Group name cannot be empty");
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final CatalogFileNode that = (CatalogFileNode) o;
    return this.getPermissions().equals(that.getPermissions())
           && this.getOwner().equals(that.getOwner())
           && this.getGroup().equals(that.getGroup())
           && this.id.equals(that.id)
           && this.creation_time.equals(that.creation_time)
           && this.modify_time.equals(that.modify_time)
           && this.access_time.equals(that.access_time)
           && this.getSize().equals(that.getSize())
           && this.getHash().equals(that.getHash());
  }

  @Override public int hashCode()
  {
    int result = this.getPermissions().hashCode();
    result = 31 * result + this.getOwner().hashCode();
    result = 31 * result + this.getGroup().hashCode();
    result = 31 * result + this.id.hashCode();
    result = 31 * result + this.creation_time.hashCode();
    result = 31 * result + this.modify_time.hashCode();
    result = 31 * result + this.access_time.hashCode();
    result = 31 * result + this.getSize().hashCode();
    result = 31 * result + this.getHash().hashCode();
    return result;
  }

  /**
   * @return The file hash, if any
   */

  public Optional<CatalogFileHash> getHash()
  {
    return this.hash;
  }

  @Override public String toString()
  {
    final StringBuilder sb = new StringBuilder("CatalogFileNode{");
    sb.append("access_time=").append(this.access_time);
    sb.append(", permissions=").append(this.permissions);
    sb.append(", owner='").append(this.owner).append('\'');
    sb.append(", group='").append(this.group).append('\'');
    sb.append(", id=").append(this.id);
    sb.append(", creation_time=").append(this.creation_time);
    sb.append(", modify_time=").append(this.modify_time);
    sb.append(", size=").append(this.size);
    sb.append(", hash=").append(this.hash);
    sb.append('}');
    return sb.toString();
  }

  /**
   * @return The file size
   */

  public BigInteger getSize()
  {
    return this.size;
  }

  @Override public <A, E extends Exception> A matchNode(
    final CatalogNodeMatcherType<A, E> m)
    throws E
  {
    return m.onFile(this);
  }

  @Override public Instant getAccessTime()
  {
    return this.access_time;
  }

  @Override public Instant getCreationTime()
  {
    return this.creation_time;
  }

  @Override public String getGroup()
  {
    return this.group;
  }

  @Override public BigInteger getID()
  {
    return this.id;
  }

  @Override public Instant getModificationTime()
  {
    return this.modify_time;
  }

  @Override public String getOwner()
  {
    return this.owner;
  }

  @Override public Set<PosixFilePermission> getPermissions()
  {
    return this.permissions;
  }
}
