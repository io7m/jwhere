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
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskBuilderType;
import com.io7m.jwhere.core.CatalogDiskDuplicateIDException;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskMetadata;
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
import java.util.Optional;
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
  private       BigInteger     pos_line;
  private       BigInteger     pos_column;

  private GWhereParser(final BufferedReader r)
  {
    this.reader = NullCheck.notNull(r);
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
    NullCheck.notNull(is);

    final BufferedReader r = new BufferedReader(new InputStreamReader(is));
    return new GWhereParser(r);
  }

  @Override public CatalogDisk parseDisk()
    throws IOException, GWhereParserException, CatalogNodeException
  {
    GWhereParser.LOG.debug("parsing disk");

    final String header_line = this.getLineNotEOF();
    final String[] header_segments = header_line.split(":");

    final String disk_name = NullCheck.notNull(header_segments[0]);
    final CatalogDiskID disk_index =
      new CatalogDiskID(new BigInteger(NullCheck.notNull(header_segments[1])));
    final String disk_type = NullCheck.notNull(header_segments[4]);
    final BigInteger disk_size =
      new BigInteger(NullCheck.notNull(header_segments[6]));

    final Pair<String, CatalogDirectoryNode> root = this.parseDiskDirectory();
    final CatalogDirectoryNode root_node = root.getRight();
    final String root_name = root.getLeft();
    if (!".".equals(root_name)) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Expected a directory named '.'");
      sb.append(System.lineSeparator());
      sb.append("Got: A ");
      sb.append(root_node.getClass());
      sb.append(" named ");
      sb.append(root_name);
      sb.append(System.lineSeparator());
      final String m = sb.toString();
      throw new GWhereUnreadableRootDirectoryException(
        this.pos_line, this.pos_column, m);
    }

    final CatalogDiskBuilderType db = CatalogDisk.newDiskBuilder(
      root_node,
      new CatalogDiskName(disk_name),
      disk_type,
      disk_index,
      disk_size);

    this.parseDirectory(db, root_node);
    return db.build();
  }

  @Override public Catalog parseCatalog()
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
      final String line = this.getLineOrEOF();
      if (line == null) {
        break;
      }

      if ("//".equals(line)) {
        final CatalogDisk disk = this.parseDisk();
        final CatalogDiskMetadata meta = disk.getMeta();
        final CatalogDiskID disk_id = meta.getDiskID();
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
    final String line = this.getLineNotEOF();
    if (!line.startsWith("archive:")) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Expected a line starting with 'archive:'");
      sb.append(System.lineSeparator());
      sb.append("Got: ");
      sb.append(line);
      sb.append(System.lineSeparator());
      final String m = sb.toString();
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
    final String line = this.getLineNotEOF();
    if (!line.startsWith("GWhere")) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Expected a line starting with 'GWhere'");
      sb.append(System.lineSeparator());
      sb.append("Got: ");
      sb.append(line);
      sb.append(System.lineSeparator());
      final String m = sb.toString();
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
      final String line = this.getLineNotEOF();
      if ("//".equals(line)) {
        return false;
      }
      if ("/".equals(line)) {
        final Pair<String, CatalogDirectoryNode> p = this.parseDiskDirectory();
        final String name = p.getLeft();
        final CatalogDirectoryNode new_dir = p.getRight();
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

      final Pair<String, CatalogNodeType> p = this.parseDiskFileFromLine(line);
      final String name = p.getLeft();
      if (".".equals(name)) {
        continue;
      }
      if ("..".equals(name)) {
        continue;
      }
      final CatalogNodeType node = p.getRight();
      db.addNode(dir, name, node);
    }
  }

  private Pair<String, CatalogDirectoryNode> parseDiskDirectoryFromLine(
    final String line)
    throws IOException, GWhereParserException
  {
    final Pair<String, CatalogNodeType> dp = this.parseDiskFileFromLine(line);
    final String name = dp.getLeft();
    final CatalogNodeType node = dp.getRight();
    if (!(node instanceof CatalogDirectoryNode)) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Expected a directory.");
      sb.append(System.lineSeparator());
      sb.append("Got: A ");
      sb.append(node.getClass());
      sb.append(" named ");
      sb.append(name);
      sb.append(System.lineSeparator());
      final String m = sb.toString();
      throw new GWhereExpectedDirectoryException(
        this.pos_line, this.pos_column, m);
    }
    return Pair.pair(name, (CatalogDirectoryNode) node);
  }

  private Pair<String, CatalogNodeType> parseDiskFileFromLine(final String line)
    throws GWhereUnreadablePermissionsException
  {
    final String[] segments = line.split(":");

    final String name = NullCheck.notNull(segments[0]);

    final EnumSet<PosixFilePermission> p =
      EnumSet.noneOf(PosixFilePermission.class);
    final FileType type = this.parsePermissions(
      NullCheck.notNull(segments[1]), p);
    final String owner = NullCheck.notNull(segments[2]);
    final String group = NullCheck.notNull(segments[3]);
    final BigInteger inode = new BigInteger(NullCheck.notNull(segments[4]));
    final BigInteger size = new BigInteger(NullCheck.notNull(segments[5]));

    final Instant creation = Instant.ofEpochSecond(
      Long.valueOf(NullCheck.notNull(segments[6])).longValue());
    final Instant access = Instant.ofEpochSecond(
      Long.valueOf(NullCheck.notNull(segments[7])).longValue());
    final Instant modification = Instant.ofEpochSecond(
      Long.valueOf(NullCheck.notNull(segments[8])).longValue());

    switch (type) {
      case DIRECTORY:
        final CatalogDirectoryNode cdn = new CatalogDirectoryNode(
          p, owner, group, inode, access, creation, modification);
        return Pair.pair(name, cdn);
      case FILE:
        final CatalogFileNode cfn = new CatalogFileNode(
          size,
          p,
          owner,
          group,
          inode,
          access,
          creation,
          modification,
          Optional.empty());
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

    final String mode_text = text.substring(1);
    final Set<PosixFilePermission> parsed;

    try {
      parsed = PosixFilePermissions.fromString(mode_text);
    } catch (final IllegalArgumentException e) {
      throw new GWhereUnreadablePermissionsException(
        this.pos_line, this.pos_column, e);
    }
    perms.addAll(parsed);

    final int type_char = text.codePointAt(0);
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
    final String line = this.getLineOrEOF();

    if (line == null) {
      throw new GWhereUnexpectedEOFException(
        this.pos_line, this.pos_column, "Unexpected EOF");
    }
    return line;
  }

  private @Nullable String getLineOrEOF()
    throws IOException
  {
    final String line = this.reader.readLine();
    this.pos_line = this.pos_line.add(BigInteger.ONE);
    this.pos_column = BigInteger.ZERO;
    GWhereParser.LOG.debug("read: {}", line);
    return line;
  }

  private enum FileType
  {
    DIRECTORY,
    FILE,
    SYMBOLIC_LINK
  }
}
