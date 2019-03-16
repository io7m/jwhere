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

package com.io7m.jwhere.tests.core;

import com.io7m.jwhere.core.CatalogCompress;
import com.io7m.jwhere.core.CatalogFileHash;
import com.io7m.jwhere.core.CatalogIgnoreAccessTime;
import com.io7m.jwhere.core.CatalogVerificationMetadataField;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JWBruteForceEqualityTest
{
  private static final Class<?> API_CLASSES[] = {
    com.io7m.jwhere.core.CatalogDirectoryNode.class,
    com.io7m.jwhere.core.CatalogDiskID.class,
    com.io7m.jwhere.core.CatalogDiskName.class,
    com.io7m.jwhere.core.CatalogFileHash.class,
    com.io7m.jwhere.core.CatalogFileNode.class,
    com.io7m.jwhere.core.CatalogSaveSpecification.class,
    com.io7m.jwhere.core.CatalogVerificationChangedHash.class,
    com.io7m.jwhere.core.CatalogVerificationChangedMetadata.class,
    com.io7m.jwhere.core.CatalogVerificationOKItem.class,
    com.io7m.jwhere.core.CatalogVerificationReportSettings.class,
    com.io7m.jwhere.core.CatalogVerificationUncataloguedItem.class,
    com.io7m.jwhere.core.CatalogVerificationVanishedItem.class,
  };

  private static void checkClassEquality(
    final Class<?> clazz)
  {
    final var fields =
      Stream.of(clazz.getDeclaredFields())
        .filter(JWBruteForceEqualityTest::fieldIsOK)
        .map(Field::getName)
        .collect(Collectors.toList());

    final var field_names = new String[fields.size()];
    fields.toArray(field_names);

    EqualsVerifier.forClass(clazz)
      .withNonnullFields(field_names)
      .verify();
  }

  private static final class SensibleAnswers implements Answer<Object>
  {
    @Override
    public Object answer(final InvocationOnMock invocation)
      throws Throwable
    {
      final var return_type = invocation.getMethod().getReturnType();
      if (return_type.equals(String.class)) {
        return "xyz";
      }
      if (return_type.equals(URI.class)) {
        return URI.create("xyz");
      }
      if (return_type.equals(BigInteger.class)) {
        return BigInteger.valueOf(23L);
      }
      if (return_type.equals(Path.class)) {
        return Paths.get("/tmp");
      }
      if (return_type.equals(Instant.class)) {
        return Instant.now();
      }
      if (return_type.equals(CatalogCompress.class)) {
        return CatalogCompress.COMPRESS_GZIP;
      }
      if (return_type.equals(CatalogFileHash.class)) {
        return CatalogFileHash.builder()
          .setValue("ABCD")
          .setAlgorithm("SHA-512")
          .build();
      }
      if (return_type.equals(CatalogVerificationMetadataField.class)) {
        return CatalogVerificationMetadataField.ACCESS_TIME;
      }
      if (return_type.equals(CatalogIgnoreAccessTime.class)) {
        return CatalogIgnoreAccessTime.IGNORE_ACCESS_TIME;
      }

      return Mockito.RETURNS_DEFAULTS.answer(invocation);
    }
  }

  private static void checkCopyOf(
    final Class<?> clazz)
    throws Exception
  {
    final var interface_type = clazz.getInterfaces()[0];
    final var mock = Mockito.mock(interface_type, new SensibleAnswers());
    final var copy_method = clazz.getMethod("copyOf", interface_type);
    final var copy = copy_method.invoke(clazz, mock);
    Assertions.assertTrue(interface_type.isAssignableFrom(copy.getClass()));
  }

  private static boolean fieldIsOK(
    final Field field)
  {
    if (Objects.equals(field.getName(), "$jacocoData")) {
      return false;
    }

    return !field.getType().isPrimitive();
  }

  @TestFactory
  public Stream<DynamicTest> testEquals()
  {
    final var all_classes =
      Stream.of(API_CLASSES);

    return all_classes
      .map(clazz -> DynamicTest.dynamicTest(
        "testEquals" + clazz.getSimpleName(),
        () -> checkClassEquality(clazz)));
  }

  @TestFactory
  public Stream<DynamicTest> testCopyOf()
  {
    final var all_classes =
      Stream.of(API_CLASSES);

    return all_classes
      .map(clazz -> DynamicTest.dynamicTest(
        "testCopyOf" + clazz.getSimpleName(),
        () -> checkCopyOf(clazz)));
  }
}
