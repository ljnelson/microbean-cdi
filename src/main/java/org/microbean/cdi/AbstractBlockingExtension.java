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

import java.util.Objects;

import java.util.concurrent.CountDownLatch;

import javax.enterprise.event.Event; // for javadoc only
import javax.enterprise.event.NotificationOptions;
import javax.enterprise.event.ObservesAsync;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

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
   * shutdown hook} that calls {@link CountDownLatch#countDown()
   * countDown()} on the supplied {@code latch}.
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
   * @param beanManager the {@link BeanManager} to use to fire the
   * event; must not be {@code null}
   *
   * @exception NullPointerException if {@code beanManager} is {@code
   * null}
   *
   * @see #unblock()
   */
  protected void fireBlockingEvent(final BeanManager beanManager) {
    if (this.logger.isTraceEnabled()) {
      this.logger.trace("ENTRY {} {} {}", this.getClass().getName(), "fireBlockingEvent", beanManager);
    }
    Objects.requireNonNull(beanManager);
    beanManager.getEvent().select(BlockingEvent.class).fireAsync(new BlockingEvent());
    if (this.logger.isTraceEnabled()) {
      this.logger.trace("EXIT {} {}", this.getClass().getName(), "fireBlockingEvent");
    }
  }

  /**
   * Uses the supplied {@link BeanManager} to {@linkplain
   * Event#fireAsync(Object, NotificationOptions) fire an asynchronous
   * event} that is also processed by this {@link
   * AbstractBlockingExtension} that will cause {@link
   * CountDownLatch#await()} to be called in a separate thread.
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
   * @see #unblock()
   */
  protected void fireBlockingEvent(final BeanManager beanManager, final NotificationOptions options) {
    if (this.logger.isTraceEnabled()) {
      this.logger.trace("ENTRY {} {} {}, {}", this.getClass().getName(), "fireBlockingEvent", beanManager, options);
    }
    Objects.requireNonNull(beanManager);
    beanManager.getEvent().select(BlockingEvent.class).fireAsync(new BlockingEvent(), options);
    if (this.logger.isTraceEnabled()) {
      this.logger.trace("EXIT {} {}", this.getClass().getName(), "fireBlockingEvent");
    }
  }

  /**
   * {@linkplain ObservesAsync Observes the asynchronous firing} of a
   * {@link BlockingEvent}, such as fired by the {@link
   * #fireBlockingEvent(BeanManager)} method, by calling {@link
   * CountDownLatch#await()} on the {@link CountDownLatch} {@linkplain
   * #AbstractBlockingExtension(CountDownLatch) supplied at
   * construction time}.
   *
   * @param event the event in question; may be {@code null}; ignored
   *
   * @exception InterruptedException if the current {@link Thread} is
   * {@linkplain Thread#interrupt() interrupted}
   *
   * @see #fireBlockingEvent(BeanManager)
   *
   * @see #unblock()
   */
  private final void block(@ObservesAsync final BlockingEvent event) throws InterruptedException {
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
   * @see #fireBlockingEvent(BeanManager)
   */
  public void unblock() {
    if (this.logger.isTraceEnabled()) {
      this.logger.trace("ENTRY {} {}", this.getClass().getName(), "unblock");
    }
    this.latch.countDown();
    if (this.logger.isTraceEnabled()) {
      this.logger.trace("EXIT {} {}", this.getClass().getName(), "unblock");
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
   * A CDI event indicating that, when {@linkplain
   * Event#fireAsync(Object) fired asynchronously}, indicates that the
   * receiver should wait for its current {@link Thread} to die.
   *
   * @author <a href="https://about.me/lairdnelson"
   * target="_parent">Laird Nelson</a>
   *
   * @see #fireBlockingEvent(BeanManager)
   *
   * @see CountDownLatch#await()
   */
  protected static final class BlockingEvent {


    /*
     * Constructors.
     */


    /**
     * Creates a new {@link BlockingEvent}.
     */
    private BlockingEvent() {
      super();
    }
    
  }
  
}
