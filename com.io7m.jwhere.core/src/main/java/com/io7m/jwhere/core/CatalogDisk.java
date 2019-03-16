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

import com.io7m.jaffirm.core.Preconditions;
import net.jcip.annotations.Immutable;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.UnmodifiableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.file.NotDirectoryException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A catalogued disk.
 */

@Immutable
public final class CatalogDisk
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CatalogDisk.class);
  }

  private final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> graph;
  private final CatalogDirectoryNode root;
  private final CatalogDiskMetadata meta;

  private CatalogDisk(
    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> in_g,
    final CatalogDirectoryNode in_root,
    final CatalogDiskMetadata in_meta)
  {
    this.graph = Objects.requireNonNull(in_g, "in_g");
    this.root = Objects.requireNonNull(in_root, "in_root");
    this.meta = Objects.requireNonNull(in_meta, "in_meta");

    Preconditions.checkPreconditionV(
      this.graph.containsVertex(this.root),
      "Root node %s must be in filesystem",
      this.root);
    Preconditions.checkPreconditionV(
      this.graph.incomingEdgesOf(this.root).isEmpty(),
      "Root node %s must have no parents",
      this.root);
  }

  /**
   * Construct a copy of the given disk.
   *
   * @param d The original disk
   *
   * @return A new disk
   */

  public static CatalogDisk fromDisk(final CatalogDisk d)
  {
    Objects.requireNonNull(d, "d");

    final var d_root = d.getFilesystemRoot();

    final var in_root =
      CatalogDirectoryNode.builder()
        .setPermissions(d_root.permissions())
        .setOwner(d_root.owner())
        .setGroup(d_root.group())
        .setId(d_root.id())
        .setAccessTime(d_root.accessTime())
        .setCreationTime(d_root.creationTime())
        .setModificationTime(d_root.modificationTime())
        .build();

    return new CatalogDisk(
      d.getFilesystemGraph(), in_root, d.getMeta());
  }

  /**
   * Construct a new disk catalog builder.
   *
   * @param in_root            The root directory
   * @param in_disk_name       The name of the disk
   * @param in_filesystem_type The name of the filesystem type
   * @param in_index           The disk ID
   * @param in_size            The size of the disk in bytes
   *
   * @return A new mutable disk builder
   */

  public static CatalogDiskBuilderType newDiskBuilder(
    final CatalogDirectoryNode in_root,
    final CatalogDiskName in_disk_name,
    final String in_filesystem_type,
    final CatalogDiskID in_index,
    final BigInteger in_size)
  {
    return new Builder(
      in_root, in_disk_name, in_filesystem_type, in_index, in_size);
  }

  private static Optional<CatalogNodeType> getNodeForPathIterator(
    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> g,
    final CatalogDirectoryNodeType node,
    final Iterator<String> iter)
    throws NotDirectoryException
  {
    final var name = iter.next();
    final var edges = g.outgoingEdgesOf(node);
    for (final var e : edges) {
      if (e.getName().equals(name)) {
        return e.getTarget().matchNode(
          new CatalogNodeMatcherType<Optional<CatalogNodeType>, NotDirectoryException>()
          {
            @Override
            public Optional<CatalogNodeType> onFile(final CatalogFileNodeType f)
              throws NotDirectoryException
            {
              return getNodeForPathIteratorFile(f, iter, name);
            }

            @Override
            public Optional<CatalogNodeType> onDirectory(
              final CatalogDirectoryNodeType d)
              throws NotDirectoryException
            {
              return getNodeForPathIteratorDirectory(d, iter, g);
            }
          });
      }
    }

    return Optional.empty();
  }

  private static Optional<CatalogNodeType> getNodeForPathIteratorDirectory(
    final CatalogDirectoryNodeType d,
    final Iterator<String> iter,
    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> g)
    throws NotDirectoryException
  {
    if (iter.hasNext()) {
      return getNodeForPathIterator(g, d, iter);
    }
    return Optional.of(d);
  }

  private static Optional<CatalogNodeType> getNodeForPathIteratorFile(
    final CatalogFileNodeType f,
    final Iterator<String> iter,
    final String name)
    throws NotDirectoryException
  {
    if (iter.hasNext()) {
      throw new NotDirectoryException(name);
    }
    return Optional.of(f);
  }

  @Override
  public String toString()
  {
    final var sb = new StringBuilder("CatalogDisk{");
    sb.append("graph=").append(this.graph);
    sb.append(", root=").append(this.root);
    sb.append(", meta=").append(this.meta);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || !Objects.equals(this.getClass(), o.getClass())) {
      return false;
    }

    final var that = (CatalogDisk) o;

    return this.graph.equals(that.graph)
      && this.root.equals(that.root)
      && this.getMeta().equals(that.getMeta());
  }

  @Override
  public int hashCode()
  {
    var result = this.graph.hashCode();
    result = 31 * result + this.root.hashCode();
    result = 31 * result + this.getMeta().hashCode();
    return result;
  }

  /**
   * @return The disk metadata
   */

  public CatalogDiskMetadata getMeta()
  {
    return this.meta;
  }

  /**
   * Construct the path from the root of the disk to the given node.
   *
   * @param node The node
   *
   * @return A path to the node
   *
   * @throws NoSuchElementException If the node is not in the filesystem
   */

  public List<String> getPathForNode(final CatalogNodeType node)
  {
    Objects.requireNonNull(node, "node");

    if (this.graph.containsVertex(node)) {
      final var dsp =
        new DijkstraShortestPath<>(this.graph, this.root, node);

      final var edge_stream =
        dsp.getPathEdgeList().stream();
      return edge_stream.map(CatalogDirectoryEntry::getName)
        .collect(Collectors.toList());
    } else {
      throw new NoSuchElementException();
    }
  }

  /**
   * @return The directed acyclic graph representing the filesystem
   */

  public UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry>
  getFilesystemGraph()
  {
    return this.graph;
  }

  /**
   * @return The filesystem root directory
   */

  public CatalogDirectoryNode getFilesystemRoot()
  {
    return this.root;
  }

  /**
   * Look up a node in the filesystem graph.
   *
   * @param p The path to the node
   *
   * @return A node, if one exists
   *
   * @throws NotDirectoryException If an element of the path other than the final one does not refer
   *                               to a directory
   */

  public Optional<CatalogNodeType> getNodeForPath(final List<String> p)
    throws NotDirectoryException
  {
    Objects.requireNonNull(p, "p");
    final var iter = p.iterator();
    return getNodeForPathIterator(this.graph, this.root, iter);
  }

  private static final class Builder implements CatalogDiskBuilderType
  {
    private final DirectedGraph<CatalogNodeType, CatalogDirectoryEntry> graph;
    private final CatalogDirectoryNode root;
    private final String type;
    private final CatalogDiskID index;
    private final BigInteger size;
    private final CatalogDiskName disk_name;
    private boolean finished;

    private Builder(
      final CatalogDirectoryNode in_root,
      final CatalogDiskName in_name,
      final String in_filesystem_type,
      final CatalogDiskID in_index,
      final BigInteger in_size)
    {
      this.disk_name = Objects.requireNonNull(in_name, "in_name");
      this.root = Objects.requireNonNull(in_root, "in_root");
      this.type = Objects.requireNonNull(in_filesystem_type, "in_filesystem_type");
      this.index = Objects.requireNonNull(in_index, "in_index");
      this.size = Objects.requireNonNull(in_size, "in_size");

      this.graph = new DirectedAcyclicGraph<>(CatalogDirectoryEntry.class);
      this.graph.addVertex(in_root);
    }

    @Override
    public void addNode(
      final CatalogDirectoryNode parent,
      final String name,
      final CatalogNodeType node)
      throws CatalogNodeException
    {
      Preconditions.checkPreconditionV(!this.finished, "Builders cannot be reused");

      LOG.debug("adding {}: {} → {}", name, parent, node);

      if (this.graph.containsVertex(parent)) {
        this.checkNoDuplicateEntry(parent, name);
      }

      if (this.graph.containsVertex(node)) {
        final var sb = new StringBuilder(256);
        sb.append("Node already in filesystem.");
        sb.append(System.lineSeparator());
        sb.append("  Node: ");
        sb.append(node);
        sb.append(System.lineSeparator());
        final var m = sb.toString();
        throw new CatalogNodeDuplicateException(m);
      }

      final var edge =
        new CatalogDirectoryEntry(parent, node, name);

      this.graph.addVertex(parent);
      this.graph.addVertex(node);
      this.graph.addEdge(parent, node, edge);
    }

    private void checkNoDuplicateEntry(
      final CatalogDirectoryNode parent,
      final String name)
      throws CatalogNodeDuplicateDirectoryEntryException
    {
      final var out = this.graph.outgoingEdgesOf(parent);
      for (final var e : out) {
        if (e.getName().equals(name)) {
          final var sb = new StringBuilder(256);
          sb.append(
            "Directory already contains an entry for the given name.");
          sb.append(System.lineSeparator());
          sb.append("  Directory: ");
          sb.append(parent);
          sb.append(System.lineSeparator());
          sb.append("  Name: ");
          sb.append(name);
          sb.append(System.lineSeparator());
          sb.append("  Node: ");
          sb.append(e.getTarget());
          sb.append(System.lineSeparator());
          final var m = sb.toString();
          throw new CatalogNodeDuplicateDirectoryEntryException(m);
        }
      }
    }

    @Override
    public CatalogDisk build()
    {
      Preconditions.checkPreconditionV(!this.finished, "Builders cannot be reused");

      try {
        return new CatalogDisk(
          new UnmodifiableGraph<>(this.graph),
          this.root,
          new CatalogDiskMetadata(
            this.disk_name, this.type, this.index, this.size));
      } finally {
        this.finished = true;
      }
    }
  }
}
