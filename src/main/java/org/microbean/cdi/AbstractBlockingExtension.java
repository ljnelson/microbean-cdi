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

import java.lang.Thread.UncaughtExceptionHandler;

import java.util.IdentityHashMap;
import java.util.Objects;

import java.util.concurrent.CountDownLatch;

import javax.annotation.Priority;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.spi.Extension;

import javax.interceptor.Interceptor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@code abstract} {@link Extension} whose implementations can
 * legally and politely prevent a CDI container from exiting.
 *
 * <p>The most common use case for such an implementation is an {@link
 * Extension} that wishes to launch a server of some kind and to
 * notionally block until someone interrupts the process.</p>
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see CountDownLatch
 *
 * @see CountDownLatch#countDown()
 *
 * @see CountDownLatch#await()
 */
public abstract class AbstractBlockingExtension implements Extension {


  /*
   * Static fields.
   */


  /**
   * An {@link IdentityHashMap} tracking all known instances of the
   * {@link AbstractBlockingExtension} class for {@linkplain
   * #unblockAll() unblocking purposes}.
   *
   * @see #unblockAll()
   *
   * @see #unblock(boolean)
   */
  private static final IdentityHashMap<AbstractBlockingExtension, Void> instances = new IdentityHashMap<>(5);

  /**
   * Static initializer; decorates any {@linkplain
   * Thread#getUncaughtExceptionHandler() existing
   * <tt>UncaughtExceptionHandler</tt>} with one that calls {@link
   * #unblockAll()}.
   */
  static {
    try {
      installExceptionHandler();
    } catch (final RuntimeException securityExceptionProbably) {
      // We take great care not to call anything with .class in it
      // since we're still initializing the class!
      final Logger logger = Logger.getLogger("org.microbean.cdi.AbstractBlockingExtension");
      assert logger != null;
      if (logger.isLoggable(Level.WARNING)) {
        logger.logp(Level.WARNING, "org.microbean.cdi.AbstractBlockingExtension", "<static init>", securityExceptionProbably.getMessage(), securityExceptionProbably);
      }
    }
  }
  

  /*
   * Instance fields.
   */


  /**
   * The {@link CountDownLatch} governing blocking behavior.
   *
   * <p>This field may be {@code null}.</p>
   *
   * @see #AbstractBlockingExtension(CountDownLatch)
   */
  private final CountDownLatch latch;

  /**
   * The {@link Logger} used by instances of this class.
   *
   * <p>This field is never {@code null}.</p>
   *
   * @see #createLogger()
   */
  protected final Logger logger;


  /*
   * Constructors.
   */
  

  /**
   * Creates a new {@link AbstractBlockingExtension} by calling the
   * {@linkplain #AbstractBlockingExtension(CountDownLatch)
   * appropriate constructor} with a new {@link CountDownLatch} with
   * an {@linkplain CountDownLatch#getCount() initial count} of {@code
   * 1}.
   *
   * @see #AbstractBlockingExtension(CountDownLatch)
   */
  protected AbstractBlockingExtension() {
    this(new CountDownLatch(1));
  }

  /**
   * Creates a new {@link AbstractBlockingExtension} that uses the
   * supplied {@link CountDownLatch} for governing blocking behavior,
   * and installs a {@linkplain Runtime#addShutdownHook(Thread)
   * shutdown hook} that (indirectly) calls {@link
   * CountDownLatch#countDown() countDown()} on the supplied {@code
   * latch}.
   *
   * @param latch a {@link CountDownLatch} whose {@link
   * CountDownLatch#countDown()} and {@link CountDownLatch#await()}
   * methods will be used to control blocking behavior; may be {@code
   * null} in which case no blocking behavior will take place
   */
  protected AbstractBlockingExtension(final CountDownLatch latch) {
    super();
    this.logger = this.createLogger();
    if (this.logger == null) {
      throw new IllegalStateException("createLogger() == null");
    }
    final String cn = this.getClass().getName();
    final String mn = "<init>";
    if (this.logger.isLoggable(Level.FINER)) {
      this.logger.entering(cn, mn, latch);
    }
    this.latch = latch;
    if (latch != null) {
      Runtime.getRuntime().addShutdownHook(new ShutdownHook());
      synchronized (instances) {
        instances.put(this, null);
      }
    }
    if (this.logger.isLoggable(Level.FINER)) {
      this.logger.exiting(cn, mn);
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link Logger} suitable for use with this {@link
   * AbstractBlockingExtension} implementation.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>Overrides of this method must not return {@code null}.</p>
   *
   * @return a non-{@code null} {@link Logger}
   */
  protected Logger createLogger() {
    return Logger.getLogger(this.getClass().getName());
  }

  /**
   * Blocks the main CDI thread immediately before any other recipient
   * of the {@link BeforeDestroyed
   * BeforeDestroyed(ApplicationScoped.class)} event is notified and
   * waits for some other thread to call the {@link #unblock()}
   * method.
   *
   * <p>Note that since the {@link #unblock()} method has no side
   * effects, this very thread could have already called it, in which
   * case no blocking behavior will be observed.</p>
   *
   * @param event the event in question; may be {@code null}; ignored
   *
   * @exception InterruptedException if the current {@link Thread} is
   * {@linkplain Thread#interrupt() interrupted}
   *
   * @see #unblock()
   *
   * @see #unblockAll()
   *
   * @see Interceptor.Priority#PLATFORM_BEFORE
   */
  private final void block(@Observes
                           @BeforeDestroyed(ApplicationScoped.class)
                           @Priority(Interceptor.Priority.PLATFORM_BEFORE - 1)
                           final Object event)
    throws InterruptedException {
    final String cn = this.getClass().getName();
    final String mn = "block";
    if (this.logger.isLoggable(Level.FINER)) {
      this.logger.entering(cn, mn, event);
    }
    if (this.latch != null) {
      this.latch.await();
    }
    if (this.logger.isLoggable(Level.FINER)) {
      this.logger.exiting(cn, mn);
    }
  }

  /**
   * Calls {@link CountDownLatch#countDown()} on the {@link
   * CountDownLatch} {@linkplain
   * #AbstractBlockingExtension(CountDownLatch) supplied at
   * construction time}.
   *
   * <p>This method may be invoked more than once without any side
   * effects.</p>
   *
   * @see #unblockAll()
   */
  public void unblock() {
    this.unblock(true);
  }

  /**
   * Calls {@link CountDownLatch#countDown()} on the {@link
   * CountDownLatch} {@linkplain
   * #AbstractBlockingExtension(CountDownLatch) supplied at
   * construction time}.
   *
   * <p>This method may be invoked more than once without any side
   * effects.</p>
   *
   * @param remove whether to remove this {@link
   * AbstractBlockingExtension} from {@linkplain #instances the
   * internal set of <code>AbstractBlockingExtension</code> instances}
   */
  private final void unblock(final boolean remove) {
    final String cn = this.getClass().getName();
    final String mn = "unblock";
    if (this.logger.isLoggable(Level.FINER)) {
      this.logger.entering(cn, mn, Boolean.valueOf(remove));
    }
    if (this.latch != null) {
      assert this.latch.getCount() == 0 || this.latch.getCount() == 1;
      this.latch.countDown();
      if (remove) {
        synchronized (instances) {
          instances.remove(this);
        }
      }
    }
    if (this.logger.isLoggable(Level.FINER)) {
      this.logger.exiting(cn, mn);
    }
  }


  /*
   * Static methods.
   */

  
  /**
   * Unblocks all known instances of the {@link
   * AbstractBlockingExtension} class by calling their associated
   * {@link CountDownLatch}es {@linkplain
   * #AbstractBlockingExtension(CountDownLatch) supplied at their
   * construction time}.
   *
   * <p>This method may be invoked more than once without any side
   * effects.</p>
   *
   * @see #unblock()
   */
  public static final void unblockAll() {
    final String cn = AbstractBlockingExtension.class.getName();
    final String mn = "unblockAll";
    final Logger logger = Logger.getLogger(cn);
    if (logger.isLoggable(Level.FINER)) {
      logger.entering(cn, mn);
    }

    synchronized (instances) {
      if (!instances.isEmpty()) {
        instances.forEach((instance, ignored) -> {
            if (instance != null) {
              instance.unblock(false);
            }
          });
        instances.clear();
      }
      assert instances.isEmpty();
    }
    if (logger.isLoggable(Level.FINER)) {
      logger.exiting(cn, mn);
    }
  }

  /**
   * Decorates any {@linkplain Thread#getUncaughtExceptionHandler()
   * existing <tt>UncaughtExceptionHandler</tt>} with one that first
   * calls {@link #unblockAll()}.
   */
  private static final void installExceptionHandler() {
    final Thread currentThread = Thread.currentThread();
    final UncaughtExceptionHandler old = currentThread.getUncaughtExceptionHandler();
    currentThread.setUncaughtExceptionHandler((thread, throwable) -> {
        unblockAll();
        if (old != null) {
          old.uncaughtException(thread, throwable);
        }
      });
  }


  /*
   * Inner and nested classes.
   */


  /**
   * A {@link Thread} whose {@link Thread#run()} method calls the
   * {@link #unblock()} method.
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see #unblock()
   */
  private final class ShutdownHook extends Thread {


    /*
     * Constructors.
     */


    /**
     * Creates a new {@link ShutdownHook}.
     */
    private ShutdownHook() {
      super();
    }


    /*
     * Instance methods.
     */
    

    /**
     * Calls {@link #unblock()} when invoked.
     */
    @Override
    public final void run() {
      final String cn = this.getClass().getName();
      final String mn = "run";
      if (logger.isLoggable(Level.FINER)) {
        logger.entering(cn, mn);
      }
      unblock();
      if (logger.isLoggable(Level.FINER)) {
        logger.exiting(cn, mn);
      }
    }
    
  }
  
}
