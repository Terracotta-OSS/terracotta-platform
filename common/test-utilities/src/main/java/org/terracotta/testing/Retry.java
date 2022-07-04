/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing;

/**
 * @author Mathieu Carbou
 */
public class Retry {

  public static void untilInterrupted(Runnable r) throws InterruptedException {
    RuntimeException re = null;
    Error e = null;
    while (!Thread.currentThread().isInterrupted()) {
      try {
        r.run();
        return;
      } catch (RuntimeException err) {
        re = err;
      } catch (Error err) {
        e = err;
      }
    }
    if (re != null) {
      throw re;
    }
    if (e != null) {
      throw e;
    }
    throw new InterruptedException();
  }

}
