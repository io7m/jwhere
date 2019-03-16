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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

/**
 * The default implementation of the {@link CatalogJSONParserType} interface.
 */

public final class CatalogJSONParser implements CatalogJSONParserType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CatalogJSONParser.class);
  }

  private CatalogJSONParser()
  {

  }

  private static void parseFilesystemNode(
    final CatalogDiskBuilderType db,
    final CatalogDirectoryNode dir,
    final ObjectNode eo)
    throws CatalogJSONParseException, CatalogNodeException
  {
    final var type = CatalogJSONParserUtilities.getString(eo, "type");
    if ("directory".equals(type)) {
      parseFilesystemDirectory(db, dir, eo);
      return;
    }
    if ("file".equals(type)) {
      parseFilesystemFile(db, dir, eo);
      return;
    }

    final var sb = new StringBuilder(128);
    sb.append("Unrecognized filesystem object type.");
    sb.append(System.lineSeparator());
    sb.append("Expected: 'file' or 'directory'");
    sb.append(System.lineSeparator());
    sb.append("Got: ");
    sb.append(type);
    throw new CatalogJSONParseException(sb.toString());
  }

  private static void parseFilesystemFile(
    final CatalogDiskBuilderType db,
    final CatalogDirectoryNode dir,
    final ObjectNode o)
    throws CatalogJSONParseException, CatalogNodeException
  {
    final var perms = PosixFilePermissions.fromString(
      CatalogJSONParserUtilities.getString(
        o, "permissions"));
    final var name = CatalogJSONParserUtilities.getString(o, "name");
    final var owner = CatalogJSONParserUtilities.getString(o, "owner");
    final var group = CatalogJSONParserUtilities.getString(o, "group");
    final var size = CatalogJSONParserUtilities.getBigInteger(o, "size");
    final var inode =
      CatalogJSONParserUtilities.getBigInteger(o, "inode");
    final var access =
      CatalogJSONParserUtilities.getInstant(o, "access-time");
    final var modify =
      CatalogJSONParserUtilities.getInstant(o, "modification-time");
    final var create =
      CatalogJSONParserUtilities.getInstant(o, "creation-time");

    final var opt_hash_raw =
      CatalogJSONParserUtilities.getObjectOptional(o, "hash");

    final Optional<CatalogFileHash> opt_hash;
    if (opt_hash_raw.isPresent()) {
      final var ho = opt_hash_raw.get();
      final var algo = CatalogJSONParserUtilities.getString(ho, "algorithm");
      final var value = CatalogJSONParserUtilities.getString(ho, "value");
      opt_hash = Optional.of(CatalogFileHash.builder().setAlgorithm(algo).setValue(value).build());
    } else {
      opt_hash = Optional.empty();
    }

    final var file =
      CatalogFileNode.builder()
        .setPermissions(perms)
        .setOwner(owner)
        .setGroup(group)
        .setId(inode)
        .setAccessTime(access)
        .setCreationTime(create)
        .setModificationTime(modify)
        .setSize(size)
        .setHash(opt_hash)
        .build();

    db.addNode(dir, name, file);
  }

  private static void parseFilesystemDirectory(
    final CatalogDiskBuilderType db,
    final CatalogDirectoryNode dir,
    final ObjectNode o)
    throws CatalogJSONParseException, CatalogNodeException
  {
    final var perms = PosixFilePermissions.fromString(
      CatalogJSONParserUtilities.getString(
        o, "permissions"));
    final var name = CatalogJSONParserUtilities.getString(o, "name");
    final var owner = CatalogJSONParserUtilities.getString(o, "owner");
    final var group = CatalogJSONParserUtilities.getString(o, "group");
    final var inode =
      CatalogJSONParserUtilities.getBigInteger(o, "inode");
    final var access =
      CatalogJSONParserUtilities.getInstant(o, "access-time");
    final var modify =
      CatalogJSONParserUtilities.getInstant(o, "modification-time");
    final var create =
      CatalogJSONParserUtilities.getInstant(o, "creation-time");
    final var dir_new =
      CatalogDirectoryNode.builder()
        .setPermissions(perms)
        .setOwner(owner)
        .setGroup(group)
        .setId(inode)
        .setAccessTime(access)
        .setCreationTime(create)
        .setModificationTime(modify)
        .build();

    db.addNode(dir, name, dir_new);

    final var entries = CatalogJSONParserUtilities.getArray(o, "entries");
    for (var i = 0; i < entries.size(); ++i) {
      final var ee = entries.get(i);
      final var eo = CatalogJSONParserUtilities.checkObject(null, ee);
      parseFilesystemNode(db, dir_new, eo);
    }
  }

  /**
   * @return A new parser
   */

  public static CatalogJSONParserType newParser()
  {
    return new CatalogJSONParser();
  }

  @Override
  public Catalog parseCatalogFromPath(final Path p)
    throws
    CatalogJSONParseException,
    CatalogNodeException,
    CatalogDiskDuplicateIDException,
    IOException
  {
    Objects.requireNonNull(p, "p");

    var guess_type = Files.probeContentType(p);
    if (guess_type == null) {
      if (p.toString().endsWith(".jcz")) {
        guess_type = "application/gzip";
      }
    }

    if ("application/gzip".equals(guess_type)) {
      LOG.debug("path {} appears to be of type {}, opening as compressed stream", p, guess_type);
      return this.parseCatalogFromPathWithCompression(p, CatalogCompress.COMPRESS_GZIP);
    } else {
      LOG.debug("path {} appears to be of type {}, opening as uncompressed stream", p, guess_type);
      return this.parseCatalogFromPathWithCompression(p, CatalogCompress.COMPRESS_NONE);
    }
  }

  @Override
  public Catalog parseCatalogFromPathWithCompression(
    final Path p,
    final CatalogCompress compression)
    throws
    CatalogJSONParseException,
    CatalogNodeException,
    CatalogDiskDuplicateIDException,
    IOException
  {
    Objects.requireNonNull(p, "p");
    Objects.requireNonNull(compression, "compression");

    // Checkstyle is unable to determine that these cases do not "fall through"
    // CHECKSTYLE:OFF
    switch (compression) {
      case COMPRESS_NONE:
        try (final var s = Files.newInputStream(p)) {
          return this.parseCatalogFromStream(s);
        }
      case COMPRESS_GZIP:
        try (final InputStream s = new GZIPInputStream(
          Files.newInputStream(p))) {
          return this.parseCatalogFromStream(s);
        }
    }
    // CHECKSTYLE:ON

    throw new UnreachableCodeException();
  }

  @Override
  public Catalog parseCatalogFromStream(final InputStream is)
    throws
    CatalogJSONParseException,
    CatalogNodeException,
    CatalogDiskDuplicateIDException,
    IOException
  {
    Objects.requireNonNull(is, "is");

    final var jom = new ObjectMapper();
    final var node = jom.readTree(is);
    return this.parseCatalog(
      CatalogJSONParserUtilities.checkObject(null, node));
  }

  @Override
  public Catalog parseCatalog(final ObjectNode c)
    throws
    CatalogJSONParseException,
    CatalogNodeException,
    CatalogDiskDuplicateIDException
  {
    Objects.requireNonNull(c, "c");

    CatalogJSONParserUtilities.getStringWithValue(
      c, "schema", "http://schemas.io7m.com/jwhere");
    CatalogJSONParserUtilities.getStringWithValue(c, "schema-version", "1.0.0");
    final var root = CatalogJSONParserUtilities.getObject(c, "catalog");
    CatalogJSONParserUtilities.getStringWithValue(root, "type", "catalog");

    final SortedMap<CatalogDiskID, CatalogDisk> disks = new TreeMap<>();

    final var jdisks =
      CatalogJSONParserUtilities.getArray(root, "catalog-disks");

    for (var index = 0; index < jdisks.size(); ++index) {
      final var jd =
        CatalogJSONParserUtilities.checkObject(null, jdisks.get(index));
      final var disk = this.parseDisk(jd);
      final var meta = disk.getMeta();
      final var disk_index = meta.getDiskID();
      if (disks.containsKey(disk_index)) {
        final var sb = new StringBuilder(128);
        sb.append("Multiple disks with the same ID.");
        sb.append(System.lineSeparator());
        sb.append("  Duplicate number: ");
        sb.append(disk_index);
        throw new CatalogDiskDuplicateIDException(sb.toString());
      }
      disks.put(disk_index, disk);
    }

    return new Catalog(disks);
  }

  @Override
  public CatalogDisk parseDisk(final ObjectNode c)
    throws CatalogJSONParseException, CatalogNodeException
  {
    Objects.requireNonNull(c, "c");

    CatalogJSONParserUtilities.getStringWithValue(c, "type", "disk");

    final var name =
      CatalogDiskName.of(CatalogJSONParserUtilities.getString(c, "disk-name"));
    final var size =
      CatalogJSONParserUtilities.getBigInteger(c, "disk-size");
    final var index = CatalogDiskID.of(
      CatalogJSONParserUtilities.getBigInteger(c, "disk-id"));
    final var fs_type =
      CatalogJSONParserUtilities.getString(c, "disk-filesystem-type");

    final var jroot =
      CatalogJSONParserUtilities.getObject(c, "disk-filesystem-root");

    final var perms = PosixFilePermissions.fromString(
      CatalogJSONParserUtilities.getString(
        jroot, "permissions"));
    final var owner = CatalogJSONParserUtilities.getString(jroot, "owner");
    final var group = CatalogJSONParserUtilities.getString(jroot, "group");
    final var inode =
      CatalogJSONParserUtilities.getBigInteger(jroot, "inode");
    final var access =
      CatalogJSONParserUtilities.getInstant(jroot, "access-time");
    final var modify =
      CatalogJSONParserUtilities.getInstant(jroot, "modification-time");
    final var create =
      CatalogJSONParserUtilities.getInstant(jroot, "creation-time");

    final var root =
      CatalogDirectoryNode.builder()
        .setPermissions(perms)
        .setOwner(owner)
        .setGroup(group)
        .setId(inode)
        .setAccessTime(access)
        .setCreationTime(create)
        .setModificationTime(modify)
        .build();

    final var db =
      CatalogDisk.newDiskBuilder(root, name, fs_type, index, size);

    final var entries =
      CatalogJSONParserUtilities.getArray(jroot, "entries");
    for (var i = 0; i < entries.size(); ++i) {
      final var ee = entries.get(i);
      final var eo = CatalogJSONParserUtilities.checkObject(null, ee);
      parseFilesystemNode(db, root, eo);
    }

    return db.build();
  }
}
