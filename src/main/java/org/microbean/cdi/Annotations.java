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

import java.lang.annotation.Annotation;

import java.lang.reflect.AnnotatedElement;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanManager;

/**
 * A utility class housing methods that do helpful things with {@link
 * Annotation}s, {@link Annotated}s and {@link AnnotatedElement}s.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public final class Annotations {


  /*
   * Constructors.
   */

  
  /**
   * Creates a new {@link Annotations}.
   */
  private Annotations() {
    super();
  }


  /*
   * Static methods.
   */
  

  /**
   * Returns a {@link Set} of {@link Annotation}s that are
   * <em>ultimately qualified</em> with an annotation whose
   * {@linkplain Annotation#annotationType() annotation type} is equal
   * to the supplied {@code metaAnnotationType}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param host the {@link Annotated} whose {@linkplain
   * Annotated#getAnnotations() annotations} will be used as the
   * initial set; must not be {@code null}
   *
   * @param metaAnnotationType the qualifier {@linkplain
   * Annotation#annotationType() annotation type} to look for; must
   * not be {@code null}
   *
   * @param beanManager a {@link BeanManager} for retrieving the
   * appropriate {@linkplain BeanManager#createAnnotatedType(Class)
   * annotated type} when appropriate; may be {@code null}
   *
   * @return a non-{@code null} subset of {@link Annotation}s
   *
   * @exception NullPointerException if {@code host} or {@code
   * metaAnnotationType} is {@code null}
   *
   * @see #retainAnnotationsQualifiedWith(Collection, Class,
   * BeanManager)
   */
  public static final Set<Annotation> getAnnotationsQualifiedWith(final Annotated host, final Class<? extends Annotation> metaAnnotationType, final BeanManager beanManager) {
    return retainAnnotationsQualifiedWith(host.getAnnotations(), Objects.requireNonNull(metaAnnotationType), beanManager);
  }

  /**
   * Returns a {@link Set} of {@link Annotation}s that are
   * <em>ultimately qualified</em> with an annotation whose
   * {@linkplain Annotation#annotationType() annotation type} is equal
   * to the supplied {@code metaAnnotationType}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param host the {@link AnnotatedElement} whose {@linkplain
   * Annotated#getAnnotations() annotations} will be used as the
   * initial set; must not be {@code null}
   *
   * @param metaAnnotationType the qualifier {@linkplain
   * Annotation#annotationType() annotation type} to look for; must
   * not be {@code null}
   *
   * @param beanManager a {@link BeanManager} for retrieving the
   * appropriate {@linkplain BeanManager#createAnnotatedType(Class)
   * annotated type} when appropriate; may be {@code null}
   *
   * @return a non-{@code null} subset of {@link Annotation}s
   *
   * @exception NullPointerException if {@code host} or {@code
   * metaAnnotationType} is {@code null}
   *
   * @see #retainAnnotationsQualifiedWith(Collection, Class,
   * BeanManager)
   */
  public static final Set<Annotation> getAnnotationsQualifiedWith(final AnnotatedElement host, final Class<? extends Annotation> metaAnnotationType, final BeanManager beanManager) {
    return retainAnnotationsQualifiedWith(Arrays.asList(host.getAnnotations()), Objects.requireNonNull(metaAnnotationType), beanManager);
  }

  /**
   * Given a {@link Collection} of {@link Annotation}s, returns a
   * subset of them that are found to be <em>ultimately qualified</em>
   * with an annotation whose {@linkplain Annotation#annotationType()
   * annotation type} is equal to the supplied {@code
   * metaAnnotationType}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param suppliedAnnotations a {@link Collection} of {@link
   * Annotation}s that will be used as the initial set; must not be
   * {@code null}
   *
   * @param metaAnnotationType the qualifier {@linkplain
   * Annotation#annotationType() annotation type} to look for; must
   * not be {@code null}
   *
   * @param beanManager a {@link BeanManager} for retrieving the
   * appropriate {@linkplain BeanManager#createAnnotatedType(Class)
   * annotated type} when appropriate; may be {@code null}
   *
   * @return a non-{@code null} subset of {@link Annotation}s
   *
   * @exception NullPointerException if {@code suppliedAnnotations} or
   * {@code metaAnnotationType} is {@code null}
   */
  public static final Set<Annotation> retainAnnotationsQualifiedWith(final Collection<? extends Annotation> suppliedAnnotations, final Class<? extends Annotation> metaAnnotationType, final BeanManager beanManager) {
    Objects.requireNonNull(suppliedAnnotations);
    Objects.requireNonNull(metaAnnotationType);
    final Set<Annotation> results = new LinkedHashSet<>();

    for (final Annotation annotation : suppliedAnnotations) {
      if (annotation != null && isAnnotationQualifiedWith(annotation, metaAnnotationType, beanManager)) {
        results.add(annotation);
      }
    }

    return results;
  }

  /**
   * Returns a {@link Collection} of {@link Annotation}s that are
   * <em>{@linkplain AnnotatedElement present}</em> on the supplied
   * {@link Class}, using the {@link
   * BeanManager#createAnnotatedType(Class)} method as appropriate.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param c the {@link Class} whose {@link Annotation}s should be
   * returned; must not be {@code null}
   *
   * @param beanManager a {@link BeanManager} whose {@link
   * BeanManager#createAnnotatedType(Class)} method will be called;
   * may be {@code null} in which case {@link
   * AnnotatedElement#getAnnotations()} will be called instead
   *
   * @return a non-{@code null} {@link Collection} of {@link
   * Annotation}s
   *
   * @see BeanManager#createAnnotatedType(Class)
   *
   * @see AnnotatedElement#getAnnotations()
   */
  public static final Collection<? extends Annotation> getAnnotations(final Class<?> c, final BeanManager beanManager) {
    Objects.requireNonNull(c);
    final Collection<? extends Annotation> returnValue;
    if (beanManager != null) {
      final Annotated annotated = beanManager.createAnnotatedType(c);
      assert annotated != null;
      returnValue = annotated.getAnnotations();
    } else {
      returnValue = Arrays.asList(c.getAnnotations());
    }
    return returnValue;
  }

  /**
   * Returns {@code true} if the supplied {@link Annotation} is
   * <em>ultimately qualified</em> by an annotation {@linkplain
   * Annotation#annotationType() with the supplied
   * <code>qualifierType</code> as its <code>annotationType</code>}.
   *
   * <p><em>Ultimately qualified</em> means that either the qualifier
   * annotation represented by the supplied {@code qualifierType} is
   * {@linkplain AnnotatedElement <em>present</em>} on the supplied
   * {@code annotation} or one of its {@link Annotation}s, or
   * {@linkplain AnnotatedElement <em>present</em>} on one of
   * <em>those</em> {@link Annotation}s, and so on.</p>
   *
   * @param annotation the {@link Annotation} to check; must not be
   * {@code null}
   *
   * @param qualifierType the {@link Class} representing the {@link
   * Annotation#annotationType() annotation type} to look for; must
   * not be {@code null}
   *
   * @param beanManager a {@link BeanManager} whose {@link
   * BeanManager#createAnnotatedType(Class)} method will be called;
   * may be {@code null}
   *
   * @return {@code true} if the supplied {@link Annotation} is
   * <em>ultimately qualified</em> by an annotation {@linkplain
   * Annotation#annotationType() with the supplied
   * <code>qualifierType</code> as its <code>annotationType</code>};
   * {@code false} otherwise
   *
   * @exception NullPointerException if {@code annotation} or {@code
   * qualifierType} is {@code null}
   */
  public static final boolean isAnnotationQualifiedWith(final Annotation annotation, final Class<? extends Annotation> qualifierType, final BeanManager beanManager) {
    return isAnnotationQualifiedWith(annotation, qualifierType, beanManager, new HashSet<>());
  }

  private static final boolean isAnnotationQualifiedWith(final Annotation annotation, final Class<? extends Annotation> qualifierType, final BeanManager beanManager, Set<Annotation> seen) {
    Objects.requireNonNull(annotation);
    Objects.requireNonNull(qualifierType);
    if (seen == null) {
      seen = new HashSet<>();
    }
    boolean returnValue = false;
    if (!isBlacklisted(annotation) && !seen.contains(annotation)) {
      seen.add(annotation);
      final Class<? extends Annotation> annotationType = annotation.annotationType();
      assert annotationType != null;
      if (annotationType.equals(qualifierType)) {
        returnValue = true;
      } else {
        final Collection<? extends Annotation> metaAnnotations = getAnnotations(annotationType, beanManager);
        if (metaAnnotations != null && !metaAnnotations.isEmpty()) {
          for (final Annotation metaAnnotation : metaAnnotations) {
            if (metaAnnotation != null && isAnnotationQualifiedWith(metaAnnotation, qualifierType, beanManager, seen)) { // RECURSION
              returnValue = true;
              break;
            }
          }
        }
      }
    }
    return returnValue;
  }

  private static final boolean isBlacklisted(final Annotation annotation) {
    return annotation == null;
  }

}
