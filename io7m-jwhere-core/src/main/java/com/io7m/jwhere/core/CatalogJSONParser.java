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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigInteger;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * The default implementation of the {@link CatalogJSONParserType} interface.
 */

public final class CatalogJSONParser implements CatalogJSONParserType
{
  private CatalogJSONParser()
  {

  }

  private static void parseFilesystemNode(
    final CatalogDiskBuilderType db,
    final CatalogDirectoryNode dir,
    final ObjectNode eo)
    throws CatalogJSONParseException, CatalogNodeException
  {
    final String type = CatalogJSONParserUtilities.getString(eo, "type");
    if ("directory".equals(type)) {
      CatalogJSONParser.parseFilesystemDirectory(db, dir, eo);
      return;
    }
    if ("file".equals(type)) {
      CatalogJSONParser.parseFilesystemFile(db, dir, eo);
      return;
    }

    final StringBuilder sb = new StringBuilder(128);
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
    final Set<PosixFilePermission> perms = PosixFilePermissions.fromString(
      CatalogJSONParserUtilities.getString(
        o, "permissions"));
    final String name = CatalogJSONParserUtilities.getString(o, "name");
    final String owner = CatalogJSONParserUtilities.getString(o, "owner");
    final String group = CatalogJSONParserUtilities.getString(o, "group");
    final BigInteger size = CatalogJSONParserUtilities.getBigInteger(o, "size");
    final BigInteger inode =
      CatalogJSONParserUtilities.getBigInteger(o, "inode");
    final Instant access =
      CatalogJSONParserUtilities.getInstant(o, "access-time");
    final Instant modify =
      CatalogJSONParserUtilities.getInstant(o, "modification-time");
    final Instant create =
      CatalogJSONParserUtilities.getInstant(o, "creation-time");

    final Optional<ObjectNode> opt_hash_raw =
      CatalogJSONParserUtilities.getObjectOptional(o, "hash");

    final Optional<CatalogFileHash> opt_hash;
    if (opt_hash_raw.isPresent()) {
      final ObjectNode ho = opt_hash_raw.get();
      final String algo = CatalogJSONParserUtilities.getString(ho, "algorithm");
      final String value = CatalogJSONParserUtilities.getString(ho, "value");
      opt_hash = Optional.of(new CatalogFileHash(algo, value));
    } else {
      opt_hash = Optional.empty();
    }

    final CatalogFileNode file = new CatalogFileNode(
      size, perms, owner, group, inode, access, create, modify, opt_hash);

    db.addNode(dir, name, file);
  }

  private static void parseFilesystemDirectory(
    final CatalogDiskBuilderType db,
    final CatalogDirectoryNode dir,
    final ObjectNode o)
    throws CatalogJSONParseException, CatalogNodeException
  {
    final Set<PosixFilePermission> perms = PosixFilePermissions.fromString(
      CatalogJSONParserUtilities.getString(
        o, "permissions"));
    final String name = CatalogJSONParserUtilities.getString(o, "name");
    final String owner = CatalogJSONParserUtilities.getString(o, "owner");
    final String group = CatalogJSONParserUtilities.getString(o, "group");
    final BigInteger inode =
      CatalogJSONParserUtilities.getBigInteger(o, "inode");
    final Instant access =
      CatalogJSONParserUtilities.getInstant(o, "access-time");
    final Instant modify =
      CatalogJSONParserUtilities.getInstant(o, "modification-time");
    final Instant create =
      CatalogJSONParserUtilities.getInstant(o, "creation-time");
    final CatalogDirectoryNode dir_new = new CatalogDirectoryNode(
      perms, owner, group, inode, access, create, modify);

    db.addNode(dir, name, dir_new);

    final ArrayNode entries = CatalogJSONParserUtilities.getArray(o, "entries");
    for (int i = 0; i < entries.size(); ++i) {
      final JsonNode ee = entries.get(i);
      final ObjectNode eo = CatalogJSONParserUtilities.checkObject(null, ee);
      CatalogJSONParser.parseFilesystemNode(db, dir_new, eo);
    }
  }

  /**
   * @return A new parser
   */

  public static CatalogJSONParserType newParser()
  {
    return new CatalogJSONParser();
  }

  @Override public CatalogDisk parseDisk(final ObjectNode c)
    throws CatalogJSONParseException, CatalogNodeException
  {
    CatalogJSONParserUtilities.getStringWithValue(c, "type", "disk");

    final String name = CatalogJSONParserUtilities.getString(c, "disk-name");
    final BigInteger size =
      CatalogJSONParserUtilities.getBigInteger(c, "disk-size");
    final BigInteger index =
      CatalogJSONParserUtilities.getBigInteger(c, "disk-archive-number");
    final String fs_type =
      CatalogJSONParserUtilities.getString(c, "disk-filesystem-type");

    final ObjectNode jroot =
      CatalogJSONParserUtilities.getObject(c, "disk-filesystem-root");

    final Set<PosixFilePermission> perms = PosixFilePermissions.fromString(
      CatalogJSONParserUtilities.getString(
        jroot, "permissions"));
    final String owner = CatalogJSONParserUtilities.getString(jroot, "owner");
    final String group = CatalogJSONParserUtilities.getString(jroot, "group");
    final BigInteger inode =
      CatalogJSONParserUtilities.getBigInteger(jroot, "inode");
    final Instant access =
      CatalogJSONParserUtilities.getInstant(jroot, "access-time");
    final Instant modify =
      CatalogJSONParserUtilities.getInstant(jroot, "modification-time");
    final Instant create =
      CatalogJSONParserUtilities.getInstant(jroot, "creation-time");
    final CatalogDirectoryNode root = new CatalogDirectoryNode(
      perms, owner, group, inode, access, create, modify);

    final CatalogDiskBuilderType db =
      CatalogDisk.newDiskBuilder(root, name, fs_type, index, size);

    final ArrayNode entries =
      CatalogJSONParserUtilities.getArray(jroot, "entries");
    for (int i = 0; i < entries.size(); ++i) {
      final JsonNode ee = entries.get(i);
      final ObjectNode eo = CatalogJSONParserUtilities.checkObject(null, ee);
      CatalogJSONParser.parseFilesystemNode(db, root, eo);
    }

    return db.build();
  }
}
