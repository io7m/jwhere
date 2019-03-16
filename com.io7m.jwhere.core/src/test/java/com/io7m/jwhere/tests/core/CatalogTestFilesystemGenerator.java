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
import com.io7m.jwhere.core.CatalogFileHash;
import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.ByteArrayGenerator;
import net.java.quickcheck.generator.support.IntegerGenerator;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestException;
import java.util.Random;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class CatalogTestFilesystemGenerator
  implements Generator<CatalogTestFilesystem>
{
  private final IntegerGenerator         depth_gen;
  private final IntegerGenerator         breadth_gen;
  private final FileNameGenerator        file_gen;
  private final CatalogFilesystemProfile profile;
  private final Random                   random;
  private final ByteArrayGenerator       data_gen;

  public CatalogTestFilesystemGenerator(
    final CatalogFilesystemProfile in_profile)
  {
    this.profile = Objects.requireNonNull(in_profile, "in_profile");

    this.depth_gen = new IntegerGenerator(1, 3);
    this.breadth_gen = new IntegerGenerator(0, 20);
    this.file_gen = new FileNameGenerator();
    this.data_gen = new ByteArrayGenerator(new IntegerGenerator(0, 1024));
    this.random = new Random();
  }

  @Override public CatalogTestFilesystem next()
  {
    try {
      FileSystem fs = null;
      switch (this.profile) {
        case PROFILE_UNIX:
          fs = CatalogTestFilesystems.makeEmptyUnixFilesystem();
          break;
        case PROFILE_WINDOWS:
          fs = CatalogTestFilesystems.makeEmptyDOSFilesystem();
          break;
      }

      assert fs != null;
      final int depth = this.depth_gen.nextInt();
      final Path root = fs.getRootDirectories().iterator().next();
      final SortedMap<Path, CatalogFileHash> hashes = new TreeMap<>();
      final SortedSet<Path> directories = new TreeSet<>();
      directories.add(root);
      this.populateDirectory(hashes, directories, root, depth);
      return new CatalogTestFilesystem(fs, hashes, directories);
    } catch (final IOException | DigestException e) {
      throw new UnreachableCodeException(e);
    }
  }

  private void populateDirectory(
    final SortedMap<Path, CatalogFileHash> hashes,
    final SortedSet<Path> directories,
    final Path current_dir,
    final int depth)
    throws IOException, DigestException
  {
    if (depth == 0) {
      return;
    }

    final int breadth = this.breadth_gen.nextInt();
    for (int index = 0; index < breadth; ++index) {
      final boolean file = this.random.nextBoolean();
      final Path p = current_dir.resolve(this.file_gen.next());

      if (file) {
        Files.write(p, this.data_gen.next());
        final CatalogFileHash hash = CatalogFileHash.fromFile(p);
        hashes.put(p, hash);
      } else {
        Files.createDirectories(p);
        directories.add(p);
        this.populateDirectory(hashes, directories, p, depth - 1);
      }
    }
  }
}
