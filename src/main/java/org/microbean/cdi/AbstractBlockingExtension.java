/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017 MicroBean.
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

import java.util.IdentityHashMap;
import java.util.Objects;

import java.util.concurrent.CountDownLatch;

import javax.annotation.Priority;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;

import javax.enterprise.event.Event; // for javadoc only
import javax.enterprise.event.NotificationOptions;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.event.Observes;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import javax.interceptor.Interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  

  /*
   * Instance fields.
   */


  /**
   * The {@link CountDownLatch} governing blocking behavior.
   *
   * <p>This field will never be {@code null}.</p>
   *
   * @see #AbstractBlockingExtension(CountDownLatch)
   */
  private final CountDownLatch latch;

  /**
   * The {@link Logger} used by instances of this class.
   *
   * <p>This field is never {@code null}.</p>
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
   * methods will be used to control blocking behavior; must not be
   * {@code null}
   *
   * @exception NullPointerException if {@code latch} is {@code null}
   */
  protected AbstractBlockingExtension(final CountDownLatch latch) {
    super();
    Objects.requireNonNull(latch);
    this.latch = latch;
    this.logger = LoggerFactory.getLogger(this.getClass());
    Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    synchronized (instances) {
      instances.put(this, null);
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Uses the supplied {@link BeanManager} to {@linkplain
   * Event#fireAsync(Object) fire an asynchronous event} that is also
   * processed by this {@link AbstractBlockingExtension} that will
   * cause {@link CountDownLatch#await()} to be called in a separate
   * thread.
   *
   * <p><strong>This method is no longer necessary and is slated for
   * removal.</strong></p>
   *
   * @param beanManager the {@link BeanManager} to use to fire the
   * event; must not be {@code null}
   *
   * @exception NullPointerException if {@code beanManager} is {@code
   * null}
   *
   * @see #unblock()
   *
   * @deprecated This method is slated for removal.
   */
  @Deprecated
  protected void fireBlockingEvent(final BeanManager beanManager) {
    Objects.requireNonNull(beanManager);
  }

  /**
   * Uses the supplied {@link BeanManager} to {@linkplain
   * Event#fireAsync(Object, NotificationOptions) fire an asynchronous
   * event} that is also processed by this {@link
   * AbstractBlockingExtension} that will cause {@link
   * CountDownLatch#await()} to be called in a separate thread.
   *
   * <p><strong>This method is no longer necessary and is slated for
   * removal.</strong></p>
   *
   * @param beanManager the {@link BeanManager} to use to fire the
   * event; must not be {@code null}
   *
   * @param options the {@link NotificationOptions} to use in the
   * {@linkplain Event#fireAsync(Object, NotificationOptions)
   * asynchronous event notification}; no documentation is available
   * in the CDI specification as to whether this parameter may be
   * {@code null} or not
   *
   * @exception NullPointerException if {@code beanManager} is {@code 
   * null}
   *
   * @exception InterruptedException if the thread was interrupted
   *
   * @see #unblock()
   *
   * @deprecated This method is slated for removal.
   */
  @Deprecated
  protected void fireBlockingEvent(final BeanManager beanManager,
                                   final NotificationOptions options)
    throws InterruptedException {
    Objects.requireNonNull(beanManager);
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
    if (this.logger.isTraceEnabled()) {
      this.logger.trace("ENTRY {} {} {}", this.getClass().getName(), "block", event);
    }
    this.latch.await();
    if (this.logger.isTraceEnabled()) {
      this.logger.trace("EXIT {} {}", this.getClass().getName(), "block");
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
    if (this.logger.isTraceEnabled()) {
      this.logger.trace("ENTRY {} {}", this.getClass().getName(), "unblock");
    }
    assert this.latch.getCount() == 0 || this.latch.getCount() == 1;
    this.latch.countDown();
    if (remove) {
      synchronized (instances) {
        instances.remove(this);
      }
    }
    if (this.logger.isTraceEnabled()) {
      this.logger.trace("EXIT {} {}", this.getClass().getName(), "unblock");
    }
  }

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
      unblock();
    }
    
  }


  /**
   * An {@link Object} serving as a CDI event indicating that,
   * when {@linkplain Event#fireAsync(Object) fired asynchronously},
   * indicates that the receiver should wait for its current {@link
   * Thread} to die.
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see CountDownLatch#await()
   *
   * @deprecated This class is slated for removal.
   */
  @Deprecated
  protected static final class BlockingEvent {

    
    /*
     * Constructors.
     */


    /**
     * Creates a new {@link BlockingEvent}.
     *
     * @deprecated This class is slated for removal.
     */
    @Deprecated
    private BlockingEvent() {
      super();
    }

  }
  
}
