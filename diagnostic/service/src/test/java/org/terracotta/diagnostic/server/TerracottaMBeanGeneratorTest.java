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
package org.terracotta.diagnostic.server;

import javax.management.StandardMBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.server.ServerMBean;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class TerracottaMBeanGeneratorTest {

  private TerracottaMBeanGenerator generator = new TerracottaMBeanGenerator();

  @Spy MyServiceImpl service;

  @Test
  public void test_generateMBeanInterface() throws NoSuchMethodException {
    assertThat(
        () -> generator.generateMBeanInterface(null),
        is(throwing(instanceOf(NullPointerException.class))));

    Class<? extends ServerMBean> mBeanInterface = generator.generateMBeanInterface(MyService.class);

    assertThat(mBeanInterface.getName(), is(equalTo(MyService.class.getName() + "MBean")));
    assertThat(MyService.class.isAssignableFrom(mBeanInterface), is(true));
    assertThat(ServerMBean.class.isAssignableFrom(mBeanInterface), is(true));
    assertThat(mBeanInterface.getMethod("say", String.class), is(not(nullValue())));
  }

  @Test
  public void test_generateMBeanImplementation() {
    Class<? extends ServerMBean> mBeanInterface = generator.generateMBeanInterface(MyService.class);

    assertThat(
        () -> generator.generateMBeanImplementation(null, MyService.class, service),
        is(throwing(instanceOf(NullPointerException.class))));
    assertThat(
        () -> generator.generateMBeanImplementation(mBeanInterface, null, service),
        is(throwing(instanceOf(NullPointerException.class))));
    assertThat(
        () -> generator.generateMBeanImplementation(mBeanInterface, MyService.class, null),
        is(throwing(instanceOf(NullPointerException.class))));

    StandardMBean mBean = generator.generateMBeanImplementation(mBeanInterface, MyService.class, service);
    assertThat(mBean, is(instanceOf(mBeanInterface)));
    verifyMBean(mBean);
  }

  @Test
  public void test_generateMBean() {
    assertThat(
        () -> generator.generateMBean(null),
        is(throwing(instanceOf(NullPointerException.class))));

    StandardMBean mBean = generator.generateMBean(MyService.class, service);
    verifyMBean(mBean);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void test_generateMBean_with_generics() {
    MyService2<String> delegate = mock(MyService2.class);
    when(delegate.say(ArgumentMatchers.any())).thenReturn("Hello you");
    Class<? extends ServerMBean> mBeanInterface = generator.generateMBeanInterface(MyService2.class);
    StandardMBean mBean = generator.generateMBeanImplementation(mBeanInterface, MyService2.class, new MyServiceImpl2<>(delegate));
    assertThat(mBean, is(instanceOf(mBeanInterface)));
    verifyMBean(mBean);
    verify(delegate, times(1)).say("you");
  }

  private void verifyMBean(StandardMBean mBean) {
    assertThat(mBean, is(
        either(instanceOf(MyService.class))
            .or(instanceOf(MyService2.class))));
    assertThat(mBean.getMBeanInterface().getName(), is(
        either(equalTo(MyService.class.getName() + "MBean"))
            .or(equalTo(MyService.class.getName() + "2MBean"))));
    
    if (mBean instanceof MyService) {
      assertThat(((MyService) mBean).say("you"), is(equalTo("Hello you")));
      verify(service, times(1)).say("you");
    } else {
      assertThat(((MyService2) mBean).say("you"), is(equalTo("Hello you")));
    }
  }

  public interface MyService {
    String say(String word);
  }

  public static class MyServiceImpl implements MyService {
    @Override
    public String say(String word) {
      return "Hello " + word;
    }
  }

  public interface MyService2<T> {
    T say(String word);
  }

  public static class MyServiceImpl2<T> implements MyService2<T> {

    private final MyService2<T> delegate;

    public MyServiceImpl2(MyService2<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public T say(String word) {
      return delegate.say(word);
    }
  }

}
