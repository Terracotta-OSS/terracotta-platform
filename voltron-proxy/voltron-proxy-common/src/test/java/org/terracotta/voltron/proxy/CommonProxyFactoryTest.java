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
package org.terracotta.voltron.proxy;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class CommonProxyFactoryTest {

  @Test
  public void test_raw_type_determined_correctly() throws Throwable {
    Class<?>[] expected = {
        String.class, Collection.class, Object.class, Object.class, Object.class,
        Collection.class, String[].class, List.class, Collection.class, String[].class,
        Object.class, Object.class};

    // to be sure we test all
    assertThat(expected.length, equalTo(AsyncEntity.class.getDeclaredMethods().length));

    for (int i = 1; i <= expected.length; i++) {
      MethodDescriptor methodDescriptor = MethodDescriptor.of(AsyncEntity.class.getMethod("test" + i));
      assertThat(methodDescriptor.isAsync(), is(true));
      assertThat(methodDescriptor.getMessageType(), Is.<Class<?>>is(expected[i-1]));
    }

    for (int i = 1; i <= expected.length; i++) {
      MethodDescriptor methodDescriptor = MethodDescriptor.of(NonAsyncEntity.class.getMethod("test" + i));
      assertThat(methodDescriptor.isAsync(), is(false));
      assertThat(methodDescriptor.getMessageType(), Is.<Class<?>>is(Future.class));
    }
  }

  @SuppressWarnings("rawtypes")
  interface AsyncEntity<V> {
    @Async Future<String> test1();
    @Async Future<Collection<String>> test2();
    @Async Future<?> test3();
    @Async Future<V> test4();
    @Async <T> Future<T> test5();
    @Async Future<Collection<V>> test6();
    @Async Future<String[]> test7();
    @Async Future<? extends List> test8();
    @Async Future<? extends Collection<String>> test9();
    @Async Future<? extends String[]> test10();
    @Async Future<? extends V> test11();
    @Async Future test12();
  }

  @SuppressWarnings("rawtypes")
  interface NonAsyncEntity<V> {
    Future<String> test1();
    Future<Collection<String>> test2();
    Future<?> test3();
    Future<V> test4();
    <T> Future<T> test5();
    Future<Collection<V>> test6();
    Future<String[]> test7();
    Future<? extends List> test8();
    Future<? extends Collection<String>> test9();
    Future<? extends String[]> test10();
    Future<? extends V> test11();
    Future test12();
  }

}
