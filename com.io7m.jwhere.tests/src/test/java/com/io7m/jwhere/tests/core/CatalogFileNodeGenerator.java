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

package com.io7m.jwhere.tests.core;

import com.io7m.jwhere.core.CatalogFileHash;
import com.io7m.jwhere.core.CatalogFileNode;
import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.StringGenerator;

import java.math.BigInteger;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public final class CatalogFileNodeGenerator
  implements Generator<CatalogFileNode>
{
  private final Generator<Boolean> type_gen;
  private final Generator<BigInteger> long_gen;
  private final Generator<Set<PosixFilePermission>> perm_gen;
  private final Generator<String> user_gen;
  private final Generator<String> group_gen;
  private final Generator<Instant> time_gen;
  private final Generator<Optional<CatalogFileHash>> hash_gen;

  public CatalogFileNodeGenerator(
    final Generator<Boolean> in_type_gen,
    final Generator<BigInteger> in_long_gen,
    final Generator<Set<PosixFilePermission>> in_perm_gen,
    final Generator<String> in_user_gen,
    final Generator<String> in_group_gen,
    final Generator<Instant> in_time_gen,
    final Generator<Optional<CatalogFileHash>> in_file_hash_gen)
  {
    this.type_gen = Objects.requireNonNull(in_type_gen, "in_type_gen");
    this.long_gen = Objects.requireNonNull(in_long_gen, "in_long_gen");
    this.perm_gen = Objects.requireNonNull(in_perm_gen, "in_perm_gen");
    this.user_gen = Objects.requireNonNull(in_user_gen, "in_user_gen");
    this.group_gen = Objects.requireNonNull(in_group_gen, "in_group_gen");
    this.time_gen = Objects.requireNonNull(in_time_gen, "in_time_gen");
    this.hash_gen = Objects.requireNonNull(in_file_hash_gen, "in_file_hash_gen");
  }

  public static Generator<CatalogFileNode> getDefault()
  {
    final var r = new Random();

    final Generator<Boolean> bool_gen = () -> Boolean.valueOf(r.nextBoolean());
    final Generator<String> user_gen = new UserNameGenerator();
    final Generator<String> group_gen = new GroupNameGenerator();

    final Generator<BigInteger> long_gen = new BigIntegerGenerator();
    final Generator<Set<PosixFilePermission>> perm_set_gen =
      new PosixFilePermissionSetGenerator();
    final Generator<Instant> time_gen =
      () -> Instant.ofEpochMilli(long_gen.next().longValue());

    final var text_gen = new StringGenerator();
    final var hash_gen =
      new CatalogFileHashGenerator(text_gen);
    final var opt_hash_gen =
      new CatalogOptionalFileHashGenerator(hash_gen, bool_gen);

    return new CatalogFileNodeGenerator(
      bool_gen,
      long_gen,
      perm_set_gen,
      user_gen,
      group_gen,
      time_gen,
      opt_hash_gen);
  }

  @Override
  public CatalogFileNode next()
  {
    final var perms = this.perm_gen.next();
    final var owner = this.user_gen.next();
    final var group = this.group_gen.next();
    final var inode = this.long_gen.next();
    final var access = this.time_gen.next();
    final var creation = this.time_gen.next();
    final var modify = this.time_gen.next();
    final var size = this.long_gen.next();
    final var hash = this.hash_gen.next();

    return CatalogFileNode.builder()
      .setPermissions(perms)
      .setOwner(owner)
      .setGroup(group)
      .setId(inode)
      .setAccessTime(access)
      .setCreationTime(creation)
      .setModificationTime(modify)
      .setSize(size)
      .setHash(hash)
      .build();
  }
}
