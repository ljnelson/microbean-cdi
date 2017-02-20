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

import javax.enterprise.event.ObservesAsync;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

public abstract class AbstractBlockingExtension implements Extension {

  private final CountDownLatch latch;
  
  protected AbstractBlockingExtension(final CountDownLatch latch) {
    super();
    Objects.requireNonNull(latch);
    this.latch = latch;
    Runtime.getRuntime().addShutdownHook(new ShutdownHook());
  }

  protected void fireBlockingEvent(final BeanManager beanManager) {
    Objects.requireNonNull(beanManager);
    beanManager.getEvent().select(BlockingEvent.class).fireAsync(new BlockingEvent());
  }
  
  private final void block(@ObservesAsync final BlockingEvent event) throws InterruptedException {    
    this.latch.await();
  }

  public void unblock() {
    this.latch.countDown();
  }

  private final class ShutdownHook extends Thread {
    
    private ShutdownHook() {
      super();
    }
    
    @Override
    public final void run() {      
      unblock();
    }
    
  }
  
  protected static final class BlockingEvent {

    private BlockingEvent() {
      super();
    }
    
  }
  
}
