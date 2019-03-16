/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> http://io7m.com
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

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.nio.file.Path;

/**
 * An error indicating that a metadata field has changed.
 */

@ImmutablesStyleType
@Value.Immutable
public interface CatalogVerificationChangedMetadataType extends CatalogVerificationReportItemErrorType
{
  /**
   * @return The path of the file
   */

  @Override
  Path path();

  /**
   * @return The field that changed
   */

  CatalogVerificationMetadataField field();

  /**
   * @return The new field value
   */

  String valueNow();

  /**
   * @return The old field value
   */

  String valueThen();

  @Override
  default String show()
  {
    return String.format(
      "%s value was %s but is now %s",
      this.field(),
      this.valueThen(),
      this.valueNow());
  }
}
