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

package com.io7m.jwhere.gui.model;

import com.io7m.jnull.NullCheck;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * A size value in bytes.
 */

public final class SizeBytes
{
  private static final BigDecimal BYTE_MEGABYTE_DIVISOR;
  private static final BigDecimal BYTE_KILOBYTE_DIVISOR;

  static {
    BYTE_MEGABYTE_DIVISOR = BigDecimal.valueOf(10000L);
    BYTE_KILOBYTE_DIVISOR = BigDecimal.valueOf(10L);
  }

  private final BigInteger value;

  /**
   * Construct a value.
   *
   * @param in_value The value
   */

  public SizeBytes(final BigInteger in_value)
  {
    this.value = NullCheck.notNull(in_value);
  }

  private static String showAsMegabytes(final BigInteger x)
  {
    final BigDecimal mb = new BigDecimal(x, 2).divide(
      SizeBytes.BYTE_MEGABYTE_DIVISOR, RoundingMode.UP);
    return mb + "mb";
  }

  private static String showAsKilobytes(final BigInteger x)
  {
    final BigDecimal mb = new BigDecimal(x, 2).divide(
      SizeBytes.BYTE_KILOBYTE_DIVISOR, RoundingMode.UP);
    return mb + "kb";
  }

  private static String showAsBytes(final BigInteger x)
  {
    return x + "b";
  }

  /**
   * @return The actual value
   */

  public BigInteger getValue()
  {
    return this.value;
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final SizeBytes other = (SizeBytes) o;
    return this.value.equals(other.value);
  }

  @Override public String toString()
  {
    return this.value.toString();
  }

  @Override public int hashCode()
  {
    return this.value.hashCode();
  }

  /**
   * @return The current byte value as a humanly readable string (as bytes,
   * kilobytes, or megabytes depending on the size)
   */

  public String toHumanString()
  {
    if (this.value.compareTo(BigInteger.valueOf(1_000L)) < 0) {
      return SizeBytes.showAsBytes(this.value);
    }

    if (this.value.compareTo(BigInteger.valueOf(1_000_000L)) < 0) {
      return SizeBytes.showAsKilobytes(this.value);
    }

    return SizeBytes.showAsMegabytes(this.value);
  }
}
