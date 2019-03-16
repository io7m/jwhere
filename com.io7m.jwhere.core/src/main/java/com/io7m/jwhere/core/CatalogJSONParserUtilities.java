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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * <p>Utility functions for deserializing elements from JSON.</p>
 *
 * <p>The functions take a strict approach: Types are checked upon key retrieval
 * and exceptions are raised if the type is not exactly as expected.</p>
 */

public final class CatalogJSONParserUtilities
{
  private CatalogJSONParserUtilities()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Check that {@code n} is an object.
   *
   * @param key An optional advisory key to be used in error messages
   * @param n   A node
   *
   * @return {@code n} as an {@link ObjectNode}
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static ObjectNode checkObject(
    final @Nullable String key,
    final JsonNode n)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(n, "n");

    switch (n.getNodeType()) {
      case ARRAY:
      case BINARY:
      case BOOLEAN:
      case MISSING:
      case NULL:
      case NUMBER:
      case POJO:
      case STRING: {
        final StringBuilder sb = new StringBuilder(128);
        if (key != null) {
          sb.append("Expected: A key '");
          sb.append(key);
          sb.append("' with a value of type Object");
          sb.append(System.lineSeparator());
          sb.append("Got: A value of type ");
          sb.append(n.getNodeType());
          sb.append(System.lineSeparator());
        } else {
          sb.append("Expected: A value of type Object");
          sb.append(System.lineSeparator());
          sb.append("Got: A value of type ");
          sb.append(n.getNodeType());
          sb.append(System.lineSeparator());
        }

        final String m = Objects.requireNonNull(sb.toString(), "sb.toString()");
        throw new CatalogJSONParseException(m);
      }
      case OBJECT: {
        return (ObjectNode) n;
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   *
   * @return An array from key {@code key}
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static ArrayNode getArray(
    final ObjectNode s,
    final String key)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(s, "s");
    Objects.requireNonNull(key, "key");

    final JsonNode n = CatalogJSONParserUtilities.getNode(
      s, key);
    switch (n.getNodeType()) {
      case ARRAY: {
        return (ArrayNode) n;
      }
      case BINARY:
      case BOOLEAN:
      case MISSING:
      case NULL:
      case NUMBER:
      case POJO:
      case STRING:
      case OBJECT: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Expected: A key '");
        sb.append(key);
        sb.append("' with a value of type Array");
        sb.append(System.lineSeparator());
        sb.append("Got: A value of type ");
        sb.append(n.getNodeType());
        sb.append(System.lineSeparator());
        final String m = Objects.requireNonNull(sb.toString(), "sb.toString()");
        throw new CatalogJSONParseException(m);
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param o   A node
   *
   * @return A boolean value from key {@code key}
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static boolean getBoolean(
    final ObjectNode o,
    final String key)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(o, "o");
    Objects.requireNonNull(key, "key");

    final JsonNode v = CatalogJSONParserUtilities.getNode(
      o, key);
    switch (v.getNodeType()) {
      case ARRAY:
      case BINARY:
      case MISSING:
      case NULL:
      case OBJECT:
      case POJO:
      case STRING:
      case NUMBER: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Expected: A key '");
        sb.append(key);
        sb.append("' with a value of type Boolean");
        sb.append(System.lineSeparator());
        sb.append("Got: A value of type ");
        sb.append(v.getNodeType());
        sb.append(System.lineSeparator());
        final String m = Objects.requireNonNull(sb.toString(), "sb.toString()");
        throw new CatalogJSONParseException(m);
      }
      case BOOLEAN: {
        return v.asBoolean();
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   *
   * @return An integer value from key {@code key}
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static int getInteger(
    final ObjectNode n,
    final String key)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(n, "n");
    Objects.requireNonNull(key, "key");

    final JsonNode v = CatalogJSONParserUtilities.getNode(
      n, key);
    switch (v.getNodeType()) {
      case ARRAY:
      case BINARY:
      case BOOLEAN:
      case MISSING:
      case NULL:
      case OBJECT:
      case POJO:
      case STRING: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Expected: A key '");
        sb.append(key);
        sb.append("' with a value of type Integer");
        sb.append(System.lineSeparator());
        sb.append("Got: A value of type ");
        sb.append(v.getNodeType());
        sb.append(System.lineSeparator());
        final String m = Objects.requireNonNull(sb.toString(), "sb.toString()");
        throw new CatalogJSONParseException(m);
      }
      case NUMBER: {
        return v.asInt();
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   *
   * @return An arbitrary json node from key {@code key}
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static JsonNode getNode(
    final ObjectNode s,
    final String key)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(s, "s");
    Objects.requireNonNull(key, "key");

    if (s.has(key)) {
      return Objects.requireNonNull(s.get(key), "s.get(key)");
    }

    final StringBuilder sb = new StringBuilder(128);
    sb.append("Expected: A key '");
    sb.append(key);
    sb.append("'");
    sb.append(System.lineSeparator());
    sb.append("Got: nothing");
    final String m = Objects.requireNonNull(sb.toString(), "sb.toString()");
    throw new CatalogJSONParseException(m);
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   *
   * @return An object value from key {@code key}
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static ObjectNode getObject(
    final ObjectNode s,
    final String key)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(s, "s");
    Objects.requireNonNull(key, "key");

    final JsonNode n = CatalogJSONParserUtilities.getNode(
      s, key);
    return CatalogJSONParserUtilities.checkObject(
      key, n);
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   *
   * @return An object value from key {@code key}, if the key exists
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static Optional<ObjectNode> getObjectOptional(
    final ObjectNode s,
    final String key)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(s, "s");
    Objects.requireNonNull(key, "key");

    if (s.has(key)) {
      return Optional.of(CatalogJSONParserUtilities.getObject(s, key));
    }
    return Optional.empty();
  }

  /**
   * @param key   A key assumed to be holding a value
   * @param value The string value associated with the key
   * @param s     A node
   *
   * @return A string value from key {@code key}
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static String getStringWithValue(
    final ObjectNode s,
    final String key,
    final String value)
    throws CatalogJSONParseException
  {
    final String r = CatalogJSONParserUtilities.getString(s, key);
    if (!value.equals(r)) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Expected: A key '");
      sb.append(key);
      sb.append("' with a value '");
      sb.append(value);
      sb.append("' of type String");
      sb.append(System.lineSeparator());
      sb.append("Got: A value '");
      sb.append(r);
      sb.append("'");
      sb.append(System.lineSeparator());
      final String m = Objects.requireNonNull(sb.toString(), "sb.toString()");
      throw new CatalogJSONParseException(m);
    }
    return r;
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   *
   * @return A string value from key {@code key}
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static String getString(
    final ObjectNode s,
    final String key)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(s, "s");
    Objects.requireNonNull(key, "key");

    final JsonNode v = CatalogJSONParserUtilities.getNode(
      s, key);
    switch (v.getNodeType()) {
      case ARRAY:
      case BINARY:
      case BOOLEAN:
      case MISSING:
      case NULL:
      case NUMBER:
      case OBJECT:
      case POJO: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Expected: A key '");
        sb.append(key);
        sb.append("' with a value of type String");
        sb.append(System.lineSeparator());
        sb.append("Got: A value of type ");
        sb.append(v.getNodeType());
        sb.append(System.lineSeparator());
        final String m = Objects.requireNonNull(sb.toString(), "sb.toString()");
        throw new CatalogJSONParseException(m);
      }
      case STRING: {
        return Objects.requireNonNull(v.asText(), "v.asText()");
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   *
   * @return An integer value from key {@code key}, if the key exists
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static Optional<Integer> getIntegerOptional(
    final ObjectNode n,
    final String key)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(n, "n");
    Objects.requireNonNull(key, "key");

    if (n.has(key)) {
      return Optional.of(
        Integer.valueOf(CatalogJSONParserUtilities.getInteger(n, key)));
    }
    return Optional.empty();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   *
   * @return A string value from key {@code key}, if the key exists
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static Optional<String> getStringOptional(
    final ObjectNode n,
    final String key)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(n, "n");
    Objects.requireNonNull(key, "key");

    if (n.has(key)) {
      return Optional.of(CatalogJSONParserUtilities.getString(n, key));
    }
    return Optional.empty();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @param v   A default value
   *
   * @return A boolean from key {@code key}, or {@code v} if the key does not
   * exist
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static boolean getBooleanDefault(
    final ObjectNode n,
    final String key,
    final boolean v)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(n, "n");
    Objects.requireNonNull(key, "key");

    if (n.has(key)) {
      return CatalogJSONParserUtilities.getBoolean(n, key);
    }
    return v;
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   *
   * @return A big integer value from key {@code key}, if the key exists
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static Optional<BigInteger> getBigIntegerOptional(
    final ObjectNode n,
    final String key)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(n, "n");
    Objects.requireNonNull(key, "key");

    if (n.has(key)) {
      return Optional.of(CatalogJSONParserUtilities.getBigInteger(n, key));
    }
    return Optional.empty();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   *
   * @return A big integer value from key {@code key}
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static BigInteger getBigInteger(
    final ObjectNode n,
    final String key)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(n, "n");
    Objects.requireNonNull(key, "key");

    final JsonNode v = CatalogJSONParserUtilities.getNode(n, key);
    switch (v.getNodeType()) {
      case ARRAY:
      case BINARY:
      case BOOLEAN:
      case MISSING:
      case NULL:
      case OBJECT:
      case POJO:
      case STRING: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Expected: A key '");
        sb.append(key);
        sb.append("' with a value of type Integer");
        sb.append(System.lineSeparator());
        sb.append("Got: A value of type ");
        sb.append(v.getNodeType());
        sb.append(System.lineSeparator());
        final String m = Objects.requireNonNull(sb.toString(), "sb.toString()");
        throw new CatalogJSONParseException(m);
      }
      case NUMBER: {
        try {
          return new BigInteger(v.asText());
        } catch (final NumberFormatException e) {
          throw new CatalogJSONParseException(e);
        }
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   *
   * @return A timestamp value from key {@code key}
   *
   * @throws CatalogJSONParseException On type errors
   */

  public static Instant getInstant(
    final ObjectNode s,
    final String key)
    throws CatalogJSONParseException
  {
    Objects.requireNonNull(s, "s");
    Objects.requireNonNull(key, "key");

    final JsonNode v = CatalogJSONParserUtilities.getNode(s, key);
    switch (v.getNodeType()) {
      case ARRAY:
      case BINARY:
      case BOOLEAN:
      case MISSING:
      case NULL:
      case NUMBER:
      case OBJECT:
      case POJO: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Expected: A key '");
        sb.append(key);
        sb.append("' with a value of type String");
        sb.append(System.lineSeparator());
        sb.append("Got: A value of type ");
        sb.append(v.getNodeType());
        sb.append(System.lineSeparator());
        final String m = Objects.requireNonNull(sb.toString(), "sb.toString()");
        throw new CatalogJSONParseException(m);
      }
      case STRING: {
        try {
          final String text = v.asText();
          return Instant.parse(text);
        } catch (final DateTimeParseException e) {
          throw new CatalogJSONParseException(e);
        }
      }
    }

    throw new UnreachableCodeException();
  }
}
