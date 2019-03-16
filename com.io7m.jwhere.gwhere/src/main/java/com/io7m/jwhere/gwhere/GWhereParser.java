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

package com.io7m.jwhere.gwhere;

import com.io7m.jfunctional.Pair;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskBuilderType;
import com.io7m.jwhere.core.CatalogDiskDuplicateIDException;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskName;
import com.io7m.jwhere.core.CatalogFileNode;
import com.io7m.jwhere.core.CatalogNodeException;
import com.io7m.jwhere.core.CatalogNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The default implementation of the {@link GWhereParserType} interface.
 */

public final class GWhereParser implements GWhereParserType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(GWhereParser.class);
  }

  private final BufferedReader reader;
  private BigInteger pos_line;
  private BigInteger pos_column;

  private GWhereParser(final BufferedReader r)
  {
    this.reader = Objects.requireNonNull(r, "r");
    this.pos_line = BigInteger.ZERO;
    this.pos_column = BigInteger.ZERO;
  }

  /**
   * Construct a new parser.
   *
   * @param is A readable stream
   *
   * @return A new parser
   */

  public static GWhereParserType newParser(final InputStream is)
  {
    Objects.requireNonNull(is, "is");

    final var r = new BufferedReader(new InputStreamReader(is));
    return new GWhereParser(r);
  }

  @Override
  public CatalogDisk parseDisk()
    throws IOException, GWhereParserException, CatalogNodeException
  {
    LOG.debug("parsing disk");

    final var header_line = this.getLineNotEOF();
    final var header_segments = header_line.split(":");

    final var disk_name = Objects.requireNonNull(header_segments[0], "header_segments[0]");
    final var disk_index =
      CatalogDiskID.of(new BigInteger(Objects.requireNonNull(
        header_segments[1],
        "header_segments[1]")));
    final var disk_type = Objects.requireNonNull(header_segments[4], "header_segments[4]");
    final var disk_size =
      new BigInteger(Objects.requireNonNull(header_segments[6], "header_segments[6]"));

    final var root = this.parseDiskDirectory();
    final var root_node = root.getRight();
    final var root_name = root.getLeft();
    if (!".".equals(root_name)) {
      final var sb = new StringBuilder(128);
      sb.append("Expected a directory named '.'");
      sb.append(System.lineSeparator());
      sb.append("Got: A ");
      sb.append(root_node.getClass());
      sb.append(" named ");
      sb.append(root_name);
      sb.append(System.lineSeparator());
      final var m = sb.toString();
      throw new GWhereUnreadableRootDirectoryException(
        this.pos_line, this.pos_column, m);
    }

    final var db = CatalogDisk.newDiskBuilder(
      root_node,
      CatalogDiskName.of(disk_name),
      disk_type,
      disk_index,
      disk_size);

    this.parseDirectory(db, root_node);
    return db.build();
  }

  @Override
  public Catalog parseCatalog()
    throws
    IOException,
    GWhereParserException,
    CatalogNodeException,
    CatalogDiskDuplicateIDException
  {
    this.parseHeader();
    this.parseArchiveHeader();

    final SortedMap<CatalogDiskID, CatalogDisk> disks = new TreeMap<>();
    while (this.reader.ready()) {
      final var line = this.getLineOrEOF();
      if (line == null) {
        break;
      }

      if ("//".equals(line)) {
        final var disk = this.parseDisk();
        final var meta = disk.getMeta();
        final var disk_id = meta.getDiskID();
        if (disks.containsKey(disk_id)) {
          throw new CatalogDiskDuplicateIDException(disk_id.toString());
        }
        disks.put(disk_id, disk);
      }
    }

    return new Catalog(disks);
  }

  private void parseArchiveHeader()
    throws
    IOException,
    GWhereUnexpectedEOFException,
    GWhereExpectedArchiveLineException
  {
    final var line = this.getLineNotEOF();
    if (!line.startsWith("archive:")) {
      final var sb = new StringBuilder(128);
      sb.append("Expected a line starting with 'archive:'");
      sb.append(System.lineSeparator());
      sb.append("Got: ");
      sb.append(line);
      sb.append(System.lineSeparator());
      final var m = sb.toString();
      throw new GWhereExpectedArchiveLineException(
        this.pos_line, this.pos_column, m);
    }
  }

  private void parseHeader()
    throws
    IOException,
    GWhereUnexpectedEOFException,
    GWhereExpectedArchiveLineException
  {
    final var line = this.getLineNotEOF();
    if (!line.startsWith("GWhere")) {
      final var sb = new StringBuilder(128);
      sb.append("Expected a line starting with 'GWhere'");
      sb.append(System.lineSeparator());
      sb.append("Got: ");
      sb.append(line);
      sb.append(System.lineSeparator());
      final var m = sb.toString();
      throw new GWhereExpectedArchiveLineException(
        this.pos_line, this.pos_column, m);
    }
  }

  private Pair<String, CatalogDirectoryNode> parseDiskDirectory()
    throws IOException, GWhereParserException
  {
    return this.parseDiskDirectoryFromLine(this.getLineNotEOF());
  }

  private boolean parseDirectory(
    final CatalogDiskBuilderType db,
    final CatalogDirectoryNode dir)
    throws IOException, GWhereParserException, CatalogNodeException
  {
    while (true) {
      final var line = this.getLineNotEOF();
      if ("//".equals(line)) {
        return false;
      }
      if ("/".equals(line)) {
        final var p = this.parseDiskDirectory();
        final var name = p.getLeft();
        final var new_dir = p.getRight();
        db.addNode(dir, name, new_dir);
        if (this.parseDirectory(db, new_dir)) {
          continue;
        } else {
          return false;
        }
      }
      if ("\\".equals(line)) {
        return true;
      }

      final var p = this.parseDiskFileFromLine(line);
      final var name = p.getLeft();
      if (".".equals(name)) {
        continue;
      }
      if ("..".equals(name)) {
        continue;
      }
      final var node = p.getRight();
      db.addNode(dir, name, node);
    }
  }

  private Pair<String, CatalogDirectoryNode> parseDiskDirectoryFromLine(
    final String line)
    throws IOException, GWhereParserException
  {
    final var dp = this.parseDiskFileFromLine(line);
    final var name = dp.getLeft();
    final var node = dp.getRight();
    if (!(node instanceof CatalogDirectoryNode)) {
      final var sb = new StringBuilder(128);
      sb.append("Expected a directory.");
      sb.append(System.lineSeparator());
      sb.append("Got: A ");
      sb.append(node.getClass());
      sb.append(" named ");
      sb.append(name);
      sb.append(System.lineSeparator());
      final var m = sb.toString();
      throw new GWhereExpectedDirectoryException(
        this.pos_line, this.pos_column, m);
    }
    return Pair.pair(name, (CatalogDirectoryNode) node);
  }

  private Pair<String, CatalogNodeType> parseDiskFileFromLine(final String line)
    throws GWhereUnreadablePermissionsException
  {
    final var segments = line.split(":");

    final var name = Objects.requireNonNull(segments[0], "segments[0]");

    final var p =
      EnumSet.noneOf(PosixFilePermission.class);
    final var type = this.parsePermissions(
      Objects.requireNonNull(segments[1], "segments[1]"), p);
    final var owner = Objects.requireNonNull(segments[2], "segments[2]");
    final var group = Objects.requireNonNull(segments[3], "segments[3]");
    final var inode = new BigInteger(Objects.requireNonNull(segments[4], "segments[4]"));
    final var size = new BigInteger(Objects.requireNonNull(segments[5], "segments[5]"));

    final var creation = Instant.ofEpochSecond(
      Long.valueOf(Objects.requireNonNull(segments[6], "segments[6]")).longValue());
    final var access = Instant.ofEpochSecond(
      Long.valueOf(Objects.requireNonNull(segments[7], "segments[7]")).longValue());
    final var modification = Instant.ofEpochSecond(
      Long.valueOf(Objects.requireNonNull(segments[8], "segments[8]")).longValue());

    switch (type) {
      case DIRECTORY:
        final var cdn =
          CatalogDirectoryNode.builder()
            .setPermissions(p)
            .setGroup(owner)
            .setOwner(group)
            .setId(inode)
            .setModificationTime(modification)
            .setAccessTime(access)
            .setCreationTime(creation)
            .build();
        return Pair.pair(name, cdn);
      case FILE:
        final var cfn =
          CatalogFileNode.builder()
            .setPermissions(p)
            .setGroup(owner)
            .setOwner(group)
            .setId(inode)
            .setModificationTime(modification)
            .setAccessTime(access)
            .setCreationTime(creation)
            .setSize(size)
            .build();
        return Pair.pair(name, cfn);
      case SYMBOLIC_LINK:
        throw new UnimplementedCodeException();
    }

    throw new UnreachableCodeException();
  }

  private FileType parsePermissions(
    final String text,
    final Set<PosixFilePermission> perms)
    throws GWhereUnreadablePermissionsException
  {
    if (text.length() != 10) {
      throw new GWhereUnreadablePermissionsException(
        this.pos_line, this.pos_column, String.format(
        "Expected a ten-character permissions string, got '%s'", text));
    }

    final var mode_text = text.substring(1);
    final Set<PosixFilePermission> parsed;

    try {
      parsed = PosixFilePermissions.fromString(mode_text);
    } catch (final IllegalArgumentException e) {
      throw new GWhereUnreadablePermissionsException(
        this.pos_line, this.pos_column, e);
    }
    perms.addAll(parsed);

    final var type_char = text.codePointAt(0);
    switch (type_char) {
      case 'd':
        return FileType.DIRECTORY;
      case '-':
        return FileType.FILE;
      case 'l':
        return FileType.SYMBOLIC_LINK;
    }

    throw new GWhereUnreadablePermissionsException(
      this.pos_line, this.pos_column, String.format(
      "Unknown file type '%s'", text.substring(0, 1)));
  }

  private String getLineNotEOF()
    throws IOException, GWhereUnexpectedEOFException
  {
    final var line = this.getLineOrEOF();

    if (line == null) {
      throw new GWhereUnexpectedEOFException(
        this.pos_line, this.pos_column, "Unexpected EOF");
    }
    return line;
  }

  private String getLineOrEOF()
    throws IOException
  {
    final var line = this.reader.readLine();
    this.pos_line = this.pos_line.add(BigInteger.ONE);
    this.pos_column = BigInteger.ZERO;
    LOG.debug("read: {}", line);
    return line;
  }

  private enum FileType
  {
    DIRECTORY,
    FILE,
    SYMBOLIC_LINK
  }
}
