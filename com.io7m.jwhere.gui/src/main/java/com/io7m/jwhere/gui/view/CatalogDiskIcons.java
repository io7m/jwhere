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

package com.io7m.jwhere.gui.view;

import java.util.Objects;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.core.CatalogDiskMetadata;

import javax.swing.Icon;

final class CatalogDiskIcons
{
  private CatalogDiskIcons()
  {
    throw new UnreachableCodeException();
  }

  static Icon getIconForDisk(final CatalogDiskMetadata disk_meta)
  {
    Objects.requireNonNull(disk_meta, "disk_meta");

    if ("iso9660".equals(disk_meta.getFilesystemType())) {
      return Icons.getDiskOpticalIcon16();
    } else {
      return Icons.getDiskIcon16();
    }
  }
}
