/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017-2018 microBean.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.cdi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import java.lang.reflect.AnnotatedElement;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanManager;

import javax.inject.Qualifier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestAnnotations {

  public TestAnnotations() {
    super();
  }

  @Test
  public void testNonInheritedMetaQualifierAbsent() throws Exception {
    final MetaQualifier[] metaQualifiers = HostWithSimpleMetaQualifiedQualifier.class.getAnnotationsByType(MetaQualifier.class);
    assertNotNull(metaQualifiers);
    assertEquals(0, metaQualifiers.length);    
  }

  @Test
  public void testInheritedMetaQualifierAbsent() throws Exception {
    final InheritedMetaQualifier[] metaQualifiers = HostWithSimpleInheritedMetaQualifiedQualifier.class.getAnnotationsByType(InheritedMetaQualifier.class);
    assertNotNull(metaQualifiers);
    assertEquals(0, metaQualifiers.length);    
  }

  @Test
  public void testIsUltimatelyQualifiedWith1() throws Exception {
    final Set<Annotation> annotations = Annotations.getAnnotationsQualifiedWith(HostWithSimpleQualifier.class, SimpleQualifier.class, null);
    assertNotNull(annotations);
    assertEquals(1, annotations.size());
  }

  @Test
  public void testIsUltimatelyQualifiedWith2() throws Exception {
    final Set<Annotation> annotations = Annotations.getAnnotationsQualifiedWith(HostWithSimpleMetaQualifiedQualifier.class, MetaQualifier.class, null);
    assertNotNull(annotations);
    assertEquals(1, annotations.size());
  }

  @Test
  public void testIsUltimatelyQualifiedWith3() throws Exception {
    final Set<Annotation> annotations = Annotations.getAnnotationsQualifiedWith(HostWithTwoSimpleMetaQualifiedQualifiers.class, MetaQualifier.class, null);
    assertNotNull(annotations);
    assertEquals(2, annotations.size());
  }

  @Documented
  @MetaQualifier
  @Retention(value = RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  private @interface SimpleMetaQualifiedQualifier {

  }

  @Documented
  @MetaQualifier
  @Retention(value = RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  private @interface SimpleMetaQualifiedQualifier2 {

  }

  @Documented
  @InheritedMetaQualifier
  @Retention(value = RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  private @interface SimpleInheritedMetaQualifiedQualifier {

  }

  @Documented
  @Retention(value = RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  private @interface SimpleQualifier {

  }

  @Documented
  @Inherited
  @Qualifier
  @Retention(value = RetentionPolicy.RUNTIME)
  @Target({ ElementType.ANNOTATION_TYPE })
  private @interface InheritedMetaQualifier {

  }

  @Documented
  @Qualifier
  @Retention(value = RetentionPolicy.RUNTIME)
  @Target({ ElementType.ANNOTATION_TYPE })
  private @interface MetaQualifier {

  }

  @SimpleQualifier
  private static final class HostWithSimpleQualifier {

  }

  @SimpleMetaQualifiedQualifier
  private static final class HostWithSimpleMetaQualifiedQualifier {

  }

  @SimpleMetaQualifiedQualifier
  @SimpleMetaQualifiedQualifier2
  @SimpleQualifier
  private static final class HostWithTwoSimpleMetaQualifiedQualifiers {

  }

  @SimpleInheritedMetaQualifiedQualifier
  private static final class HostWithSimpleInheritedMetaQualifiedQualifier {

  }
  
}
