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
import com.io7m.jwhere.core.CatalogDirectoryEntry;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogNodeType;
import net.java.quickcheck.Generator;

public final class CatalogDirectoryEntryGenerator
  implements Generator<CatalogDirectoryEntry>
{
  private final Generator<CatalogDirectoryNode> dir_gen;
  private final Generator<CatalogNodeType>      node_gen;
  private final Generator<String>               name_gen;

  public CatalogDirectoryEntryGenerator(
    final Generator<CatalogDirectoryNode> in_dir_gen,
    final Generator<String> in_name_gen,
    final Generator<CatalogNodeType> in_node_gen)
  {
    this.dir_gen = Objects.requireNonNull(in_dir_gen, "in_dir_gen");
    this.name_gen = Objects.requireNonNull(in_name_gen, "in_name_gen");
    this.node_gen = Objects.requireNonNull(in_node_gen, "in_node_gen");
  }

  public static Generator<CatalogDirectoryEntry> getDefault()
  {
    final Generator<CatalogDirectoryNode> dg =
      CatalogDirectoryNodeGenerator.getDefault();
    final Generator<String> ng = new FileNameGenerator();
    final Generator<CatalogNodeType> nog = CatalogNodeGenerator.getDefault();
    return new CatalogDirectoryEntryGenerator(dg, ng, nog);
  }

  @Override public CatalogDirectoryEntry next()
  {
    final CatalogDirectoryNode source = this.dir_gen.next();
    final CatalogNodeType target = this.node_gen.next();
    final String name = this.name_gen.next();
    return new CatalogDirectoryEntry(source, target, name);
  }
}
