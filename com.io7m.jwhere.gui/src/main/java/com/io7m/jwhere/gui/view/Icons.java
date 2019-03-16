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

import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

final class Icons
{
  private static final Logger LOG;
  private static final Map<String, ImageIcon> ICON_CACHE;

  static {
    LOG = LoggerFactory.getLogger(Icons.class);
    ICON_CACHE = new WeakHashMap<>(8);
  }

  private Icons()
  {
    throw new UnreachableCodeException();
  }

  static ImageIcon getErrorIcon()
  {
    return getIcon("dialog-error-32.png");
  }

  static ImageIcon getWarningIcon16()
  {
    return getIcon("dialog-warning-16.png");
  }

  static ImageIcon getDiskOpticalIcon16()
  {
    return getIcon("media-optical-16.png");
  }

  static ImageIcon getDiskIcon16()
  {
    return getIcon("drive-harddisk-16.png");
  }

  private static ImageIcon getIcon(final String name)
  {
    if (ICON_CACHE.containsKey(name)) {
      return ICON_CACHE.get(name);
    } else {
      try (var stream = Icons.class.getResourceAsStream(name)) {
        final var image = ImageIO.read(stream);
        final var icon = new ImageIcon(image);
        ICON_CACHE.put(name, icon);
        return icon;
      } catch (final IOException e) {
        LOG.error("unable to load icon: ", e);
        return new ImageIcon();
      }
    }
  }

  static Icon getFile16()
  {
    return getIcon("text-x-generic-16.png");
  }

  static Icon getFolder16()
  {
    return getIcon("folder-16.png");
  }

  static Icon getFolderUp16()
  {
    return getIcon("folder-up-16.png");
  }

  static Icon getCatalogIcon16()
  {
    return getIcon("system-file-manager-16.png");
  }
}
