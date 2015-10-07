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

import com.io7m.jnull.NullCheck;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.UnmodifiableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valid4j.Assertive;

import java.math.BigInteger;
import java.nio.file.NotDirectoryException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A catalogued disk.
 */

public final class CatalogDisk
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CatalogDisk.class);
  }

  private final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> graph;
  private final CatalogDirectoryNode                                      root;
  private final String
                                                                          disk_name;
  private final String
                                                                          fs_type;
  private final BigInteger                                                index;
  private final BigInteger                                                size;

  private CatalogDisk(
    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> in_g,
    final CatalogDirectoryNode in_root,
    final String in_disk_name,
    final String in_filesystem_type,
    final BigInteger in_index,
    final BigInteger in_size)
  {
    this.graph = NullCheck.notNull(in_g);
    this.root = NullCheck.notNull(in_root);
    this.disk_name = NullCheck.notNull(in_disk_name);
    this.fs_type = NullCheck.notNull(in_filesystem_type);
    this.index = NullCheck.notNull(in_index);
    this.size = NullCheck.notNull(in_size);

    Assertive.require(
      this.graph.containsVertex(this.root),
      "Root node %s must be in filesystem",
      this.root);
    Assertive.require(
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
    NullCheck.notNull(d);

    final CatalogDirectoryNode d_root = d.getFilesystemRoot();
    final CatalogDirectoryNode in_root = new CatalogDirectoryNode(
      d_root.getPermissions(),
      d_root.getOwner(),
      d_root.getGroup(),
      d_root.getID(),
      d_root.getAccessTime(),
      d_root.getCreationTime(),
      d_root.getModificationTime());

    return new CatalogDisk(
      d.getFilesystemGraph(),
      in_root,
      d.getDiskName(),
      d.getFilesystemType(),
      d.getArchiveIndex(),
      d.getDiskSize());
  }

  /**
   * Construct a new disk catalog builder.
   *
   * @param in_root            The root directory
   * @param in_disk_name       The name of the disk
   * @param in_filesystem_type The name of the filesystem type
   * @param in_index           The disk archive number
   * @param in_size            The size of the disk in bytes
   *
   * @return A new mutable disk builder
   */

  public static CatalogDiskBuilderType newDiskBuilder(
    final CatalogDirectoryNode in_root,
    final String in_disk_name,
    final String in_filesystem_type,
    final BigInteger in_index,
    final BigInteger in_size)
  {
    return new Builder(
      in_root, in_disk_name, in_filesystem_type, in_index, in_size);
  }

  private static Optional<CatalogNodeType> getNodeForPathIterator(
    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> g,
    final CatalogDirectoryNode node,
    final Iterator<String> iter)
    throws NotDirectoryException
  {
    final String name = iter.next();
    final Set<CatalogDirectoryEntry> edges = g.outgoingEdgesOf(node);
    for (final CatalogDirectoryEntry e : edges) {
      if (e.getName().equals(name)) {
        return e.getTarget().matchNode(
          new CatalogNodeMatcherType<Optional<CatalogNodeType>,
            NotDirectoryException>()
          {
            @Override
            public Optional<CatalogNodeType> onFile(final CatalogFileNode f)
              throws NotDirectoryException
            {
              if (iter.hasNext()) {
                throw new NotDirectoryException(name);
              }
              return Optional.of(f);
            }

            @Override public Optional<CatalogNodeType> onDirectory(
              final CatalogDirectoryNode d)
              throws NotDirectoryException
            {
              return CatalogDisk.getNodeForPathIterator(g, d, iter);
            }
          });
      }
    }

    return Optional.empty();
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
    NullCheck.notNull(node);

    if (this.graph.containsVertex(node)) {
      final DijkstraShortestPath<CatalogNodeType, CatalogDirectoryEntry> dsp =
        new DijkstraShortestPath<>(this.graph, this.root, node);

      final Stream<CatalogDirectoryEntry> edge_stream =
        dsp.getPathEdgeList().stream();
      return edge_stream.map(CatalogDirectoryEntry::getName)
        .collect(Collectors.toList());
    } else {
      throw new NoSuchElementException();
    }
  }

  @Override public String toString()
  {
    final StringBuilder sb = new StringBuilder("CatalogDisk{");
    sb.append("disk_name='").append(this.disk_name).append('\'');
    sb.append(", graph=").append(this.graph);
    sb.append(", root=").append(this.root);
    sb.append(", fs_type='").append(this.fs_type).append('\'');
    sb.append(", index=").append(this.index);
    sb.append(", size=").append(this.size);
    sb.append('}');
    return sb.toString();
  }

  /**
   * @return The name of the disk
   */

  public String getDiskName()
  {
    return this.disk_name;
  }

  /**
   * @return The name of the filesystem type
   */

  public String getFilesystemType()
  {
    return this.fs_type;
  }

  /**
   * @return The archive number of the disk
   */

  public BigInteger getArchiveIndex()
  {
    return this.index;
  }

  /**
   * @return The size of the disk
   */

  public BigInteger getDiskSize()
  {
    return this.size;
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
   * @throws NotDirectoryException If an element of the path other than the
   *                               final one does not refer to a directory
   */

  public Optional<CatalogNodeType> getNodeForPath(final List<String> p)
    throws NotDirectoryException
  {
    NullCheck.notNull(p);
    final Iterator<String> iter = p.iterator();
    return CatalogDisk.getNodeForPathIterator(this.graph, this.root, iter);
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final CatalogDisk that = (CatalogDisk) o;
    return this.graph.equals(that.graph)
           && this.root.equals(that.root)
           && this.disk_name.equals(that.disk_name)
           && this.fs_type.equals(that.fs_type)
           && this.index.equals(that.index)
           && this.size.equals(that.size);
  }

  @Override public int hashCode()
  {
    int result = this.graph.hashCode();
    result = 31 * result + this.root.hashCode();
    result = 31 * result + this.disk_name.hashCode();
    result = 31 * result + this.fs_type.hashCode();
    result = 31 * result + this.index.hashCode();
    result = 31 * result + this.size.hashCode();
    return result;
  }

  private static final class Builder implements CatalogDiskBuilderType
  {
    private final DirectedGraph<CatalogNodeType, CatalogDirectoryEntry> graph;
    private final CatalogDirectoryNode                                  root;
    private final String                                                type;
    private final BigInteger                                            index;
    private final BigInteger                                            size;
    private final String
                                                                        disk_name;
    private       boolean
                                                                        finished;

    private Builder(
      final CatalogDirectoryNode in_root,
      final String in_name,
      final String in_filesystem_type,
      final BigInteger in_index,
      final BigInteger in_size)
    {
      this.disk_name = NullCheck.notNull(in_name);
      this.root = NullCheck.notNull(in_root);
      this.type = NullCheck.notNull(in_filesystem_type);
      this.index = NullCheck.notNull(in_index);
      this.size = NullCheck.notNull(in_size);

      this.graph = new DirectedAcyclicGraph<>(CatalogDirectoryEntry.class);
      this.graph.addVertex(in_root);
    }

    @Override public void addNode(
      final CatalogDirectoryNode parent,
      final String name,
      final CatalogNodeType node)
      throws CatalogNodeException
    {
      CatalogDisk.LOG.debug("adding {}: {} → {}", name, parent, node);

      if (this.graph.containsVertex(parent)) {
        this.checkNoDuplicateEntry(parent, name);
      }

      if (this.graph.containsVertex(node)) {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("Node already in filesystem.");
        sb.append(System.lineSeparator());
        sb.append("  Node: ");
        sb.append(node);
        sb.append(System.lineSeparator());
        final String m = sb.toString();
        throw new CatalogNodeDuplicateException(m);
      }

      final CatalogDirectoryEntry edge =
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
      final Set<CatalogDirectoryEntry> out = this.graph.outgoingEdgesOf(parent);
      for (final CatalogDirectoryEntry e : out) {
        if (e.getName().equals(name)) {
          final StringBuilder sb = new StringBuilder(256);
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
          final String m = sb.toString();
          throw new CatalogNodeDuplicateDirectoryEntryException(m);
        }
      }
    }

    @Override public CatalogDisk build()
    {
      Assertive.require(!this.finished, "Builders cannot be reused");

      try {
        return new CatalogDisk(
          new UnmodifiableGraph<>(this.graph),
          this.root,
          this.disk_name,
          this.type,
          this.index,
          this.size);
      } finally {
        this.finished = true;
      }
    }
  }
}
