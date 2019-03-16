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
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import com.io7m.junreachable.UnreachableCodeException;
import org.jgrapht.graph.UnmodifiableGraph;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.zip.GZIPOutputStream;

/**
 * The default implementation of the {@link CatalogJSONSerializerType}
 * interface.
 */

public final class CatalogJSONSerializer implements CatalogJSONSerializerType
{
  private CatalogJSONSerializer()
  {

  }

  private static ObjectNode serializeFile(
    final ObjectMapper jom,
    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> g,
    final CatalogFileNode node,
    final String name)
  {
    final Instant atime = node.getAccessTime();
    final Instant mtime = node.getModificationTime();
    final Instant ctime = node.getCreationTime();

    final ObjectNode jout = jom.createObjectNode();
    jout.put("type", "file");
    jout.put("name", name);
    jout.set("size", new BigIntegerNode(node.getSize()));
    jout.put("owner", node.getOwner());
    jout.put("group", node.getGroup());
    jout.put("access-time", atime.toString());
    jout.put("modification-time", mtime.toString());
    jout.put("creation-time", ctime.toString());
    jout.set("inode", new BigIntegerNode(node.getID()));
    jout.put(
      "permissions", PosixFilePermissions.toString(node.getPermissions()));

    final Optional<CatalogFileHash> hash_opt = node.getHash();
    if (hash_opt.isPresent()) {
      final ObjectNode jhash =
        CatalogJSONSerializer.serializeHash(jom, hash_opt.get());
      jout.set("hash", jhash);
    }

    return jout;
  }

  private static ObjectNode serializeHash(
    final ObjectMapper jom,
    final CatalogFileHash hash)
  {
    final ObjectNode jout = jom.createObjectNode();
    jout.put("type", "hash");
    jout.put("algorithm", hash.getAlgorithm());
    jout.put("value", hash.getValue());
    return jout;
  }

  private static ObjectNode serializeNode(
    final ObjectMapper jom,
    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> g,
    final CatalogNodeType node,
    final String name)
  {
    return node.matchNode(
      new CatalogNodeMatcherType<ObjectNode, UnreachableCodeException>()
      {
        @Override public ObjectNode onFile(final CatalogFileNode f)
        {
          return CatalogJSONSerializer.serializeFile(jom, g, f, name);
        }

        @Override public ObjectNode onDirectory(final CatalogDirectoryNode d)
        {
          return CatalogJSONSerializer.serializeDirectory(jom, g, d, name);
        }
      });
  }

  private static ObjectNode serializeDirectory(
    final ObjectMapper jom,
    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> g,
    final CatalogDirectoryNode node,
    final String name)
  {
    final Instant atime = node.getAccessTime();
    final Instant mtime = node.getModificationTime();
    final Instant ctime = node.getCreationTime();

    final ObjectNode jout = jom.createObjectNode();
    jout.put("type", "directory");
    jout.put("name", name);
    jout.put("owner", node.getOwner());
    jout.put("group", node.getGroup());
    jout.put("access-time", atime.toString());
    jout.put("modification-time", mtime.toString());
    jout.put("creation-time", ctime.toString());
    jout.set("inode", new BigIntegerNode(node.getID()));
    jout.put(
      "permissions", PosixFilePermissions.toString(node.getPermissions()));

    final ArrayNode ee = jom.createArrayNode();
    final Set<CatalogDirectoryEntry> oe = g.outgoingEdgesOf(node);
    for (final CatalogDirectoryEntry edge : oe) {
      final String e_name = edge.getName();
      final CatalogNodeType e_node = edge.getTarget();
      ee.add(CatalogJSONSerializer.serializeNode(jom, g, e_node, e_name));
    }

    jout.set("entries", ee);
    return jout;
  }

  /**
   * @return A new serializer
   */

  public static CatalogJSONSerializerType newSerializer()
  {
    return new CatalogJSONSerializer();
  }

  @Override public void serializeCatalogToPath(
    final Catalog c,
    final CatalogSaveSpecification s)
    throws IOException
  {
    switch (s.getCompression()) {
      case COMPRESS_NONE:
        try (final OutputStream os = Files.newOutputStream(s.getPath())) {
          this.serializeCatalogToStream(c, os);
        }
        break;
      case COMPRESS_GZIP:
        try (final OutputStream os = new GZIPOutputStream(
          Files.newOutputStream(s.getPath()))) {
          this.serializeCatalogToStream(c, os);
        }
        break;
    }
  }

  @Override public void serializeCatalogToStream(
    final Catalog c,
    final OutputStream os)
    throws IOException
  {
    Objects.requireNonNull(c, "c");
    Objects.requireNonNull(os, "os");

    final ObjectMapper jom = new ObjectMapper();
    final ObjectWriter jw = jom.writerWithDefaultPrettyPrinter();
    jw.writeValue(os, this.serializeCatalog(c));
  }

  @Override public ObjectNode serializeCatalog(final Catalog c)
  {
    Objects.requireNonNull(c, "c");

    final ObjectMapper jom = new ObjectMapper();

    final ObjectNode jcat = jom.createObjectNode();
    jcat.put("type", "catalog");
    final ArrayNode jda = jom.createArrayNode();
    final SortedMap<CatalogDiskID, CatalogDisk> disks = c.getDisks();
    final Iterator<CatalogDiskID> iter = disks.keySet().iterator();
    while (iter.hasNext()) {
      final CatalogDisk disk = disks.get(iter.next());
      jda.add(this.serializeDisk(disk));
    }
    jcat.set("catalog-disks", jda);

    final ObjectNode jroot = jom.createObjectNode();
    jroot.put("schema", "http://schemas.io7m.com/jwhere");
    jroot.put("schema-version", "1.0.0");
    jroot.set("catalog", jcat);

    return jroot;
  }

  @Override public ObjectNode serializeDisk(final CatalogDisk d)
  {
    Objects.requireNonNull(d, "d");

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode jd = jom.createObjectNode();

    final ObjectNode jfs = CatalogJSONSerializer.serializeDirectory(
      jom, d.getFilesystemGraph(), d.getFilesystemRoot(), "/");

    jd.put("type", "disk");

    final CatalogDiskMetadata meta = d.getMeta();

    jd.put("disk-name", meta.getDiskName().value());
    jd.set("disk-size", new BigIntegerNode(meta.getSize()));
    jd.set("disk-id", new BigIntegerNode(meta.getDiskID().value()));
    jd.put("disk-filesystem-type", meta.getFilesystemType());
    jd.set("disk-filesystem-root", jfs);

    return jd;
  }
}
