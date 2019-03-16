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

import java.util.Objects;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskBuilderType;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskName;
import com.io7m.jwhere.core.CatalogNodeException;
import com.io7m.jwhere.core.CatalogNodeType;
import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.IntegerGenerator;
import net.java.quickcheck.generator.support.StringGenerator;

import java.math.BigInteger;

public final class CatalogDiskGenerator implements Generator<CatalogDisk>
{
  private final Generator<BigInteger>           long_gen;
  private final Generator<String>               string_gen;
  private final Generator<CatalogDirectoryNode> dir_gen;
  private final Generator<CatalogNodeType>      node_gen;
  private final IntegerGenerator                depth_gen;
  private final IntegerGenerator                breadth_gen;
  private final FileNameGenerator               file_gen;

  public CatalogDiskGenerator(
    final Generator<CatalogDirectoryNode> in_dir_gen,
    final Generator<BigInteger> in_long_gen,
    final Generator<String> in_string_gen,
    final Generator<CatalogNodeType> in_node_gen)
  {
    this.dir_gen = Objects.requireNonNull(in_dir_gen, "in_dir_gen");
    this.long_gen = Objects.requireNonNull(in_long_gen, "in_long_gen");
    this.string_gen = Objects.requireNonNull(in_string_gen, "in_string_gen");
    this.node_gen = Objects.requireNonNull(in_node_gen, "in_node_gen");
    this.depth_gen = new IntegerGenerator(1, 3);
    this.breadth_gen = new IntegerGenerator(0, 20);
    this.file_gen = new FileNameGenerator();
  }

  public static Generator<CatalogDisk> getDefault()
  {
    final Generator<CatalogDirectoryNode> in_dir_gen =
      CatalogDirectoryNodeGenerator.getDefault();
    final Generator<BigInteger> in_long_gen = new BigIntegerGenerator();
    final Generator<String> in_string_gen = new StringGenerator();
    final Generator<CatalogNodeType> in_node_gen =
      CatalogNodeGenerator.getDefault();
    return new CatalogDiskGenerator(
      in_dir_gen, in_long_gen, in_string_gen, in_node_gen);
  }

  @Override public CatalogDisk next()
  {
    final CatalogDiskID index = CatalogDiskID.of(this.long_gen.next());
    final BigInteger size = this.long_gen.next();
    final CatalogDiskName disk_name =
      CatalogDiskName.of(this.string_gen.next());
    final String fs_type = this.string_gen.next();
    final CatalogDirectoryNode root = this.dir_gen.next();

    final CatalogDiskBuilderType db =
      CatalogDisk.newDiskBuilder(root, disk_name, fs_type, index, size);

    final int depth = this.depth_gen.nextInt();
    this.populateDirectory(db, root, depth);

    return db.build();
  }

  private void populateDirectory(
    final CatalogDiskBuilderType db,
    final CatalogDirectoryNode dir,
    final int depth)
  {
    if (depth == 0) {
      return;
    }

    final int breadth = this.breadth_gen.nextInt();
    for (int index = 0; index < breadth; ++index) {
      final CatalogNodeType node = this.node_gen.next();
      final String name = this.file_gen.next();
      try {
        db.addNode(dir, name, node);
      } catch (final CatalogNodeException e) {
        throw new UnreachableCodeException(e);
      }
      if (node instanceof CatalogDirectoryNode) {
        this.populateDirectory(db, (CatalogDirectoryNode) node, depth - 1);
      }
    }
  }
}
