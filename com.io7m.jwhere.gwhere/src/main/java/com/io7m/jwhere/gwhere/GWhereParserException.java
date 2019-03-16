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

import java.util.Objects;
import com.io7m.jwhere.core.CatalogException;

import java.math.BigInteger;

/**
 * The type of errors for parsers that consume {@code GWhere} data.
 */

public abstract class GWhereParserException extends CatalogException
{
  private final BigInteger line;
  private final BigInteger column;

  /**
   * Construct an exception
   *
   * @param in_line   The line number
   * @param in_column The column number
   * @param m         The exception message
   */

  public GWhereParserException(
    final BigInteger in_line,
    final BigInteger in_column,
    final String m)
  {
    super(m);
    this.line = Objects.requireNonNull(in_line, "in_line");
    this.column = Objects.requireNonNull(in_column, "in_column");
  }

  /**
   * Construct an exception
   *
   * @param in_line   The line number
   * @param in_column The column number
   * @param e         The exception cause
   */

  public GWhereParserException(
    final BigInteger in_line,
    final BigInteger in_column,
    final Throwable e)
  {
    super(e);
    this.line = Objects.requireNonNull(in_line, "in_line");
    this.column = Objects.requireNonNull(in_column, "in_column");
  }

  /**
   * @return The column number where the error occurred
   */

  public final BigInteger getColumn()
  {
    return this.column;
  }

  /**
   * @return The line number where the error occurred
   */

  public final BigInteger getLine()
  {
    return this.line;
  }
}
