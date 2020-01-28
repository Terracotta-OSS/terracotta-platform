/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.server;

import com.tc.management.TerracottaMBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;

import static com.terracottatech.testing.ExceptionMatcher.throwing;
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

    Class<? extends TerracottaMBean> mBeanInterface = generator.generateMBeanInterface(MyService.class);

    assertThat(mBeanInterface.getName(), is(equalTo(MyService.class.getName() + "MBean")));
    assertThat(MyService.class.isAssignableFrom(mBeanInterface), is(true));
    assertThat(TerracottaMBean.class.isAssignableFrom(mBeanInterface), is(true));
    assertThat(mBeanInterface.getMethod("say", String.class), is(not(nullValue())));
  }

  @Test
  public void test_generateMBeanImplementation() {
    Class<? extends TerracottaMBean> mBeanInterface = generator.generateMBeanInterface(MyService.class);

    assertThat(
        () -> generator.generateMBeanImplementation(null, MyService.class, service),
        is(throwing(instanceOf(NullPointerException.class))));
    assertThat(
        () -> generator.generateMBeanImplementation(mBeanInterface, null, service),
        is(throwing(instanceOf(NullPointerException.class))));
    assertThat(
        () -> generator.generateMBeanImplementation(mBeanInterface, MyService.class, null),
        is(throwing(instanceOf(NullPointerException.class))));

    TerracottaMBean mBean = generator.generateMBeanImplementation(mBeanInterface, MyService.class, service);
    assertThat(mBean, is(instanceOf(mBeanInterface)));
    verifyMBean(mBean);
  }

  @Test
  public void test_generateMBean() {
    assertThat(
        () -> generator.generateMBean(null),
        is(throwing(instanceOf(NullPointerException.class))));

    TerracottaMBean mBean = generator.generateMBean(MyService.class, service);
    verifyMBean(mBean);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void test_generateMBean_with_generics() {
    MyService2<String> delegate = mock(MyService2.class);
    when(delegate.say(ArgumentMatchers.any())).thenReturn("Hello you");
    Class<? extends TerracottaMBean> mBeanInterface = generator.generateMBeanInterface(MyService2.class);
    for (Method method : mBeanInterface.getMethods()) {
      System.out.println(method);
    }
    TerracottaMBean mBean = generator.generateMBeanImplementation(mBeanInterface, MyService2.class, new MyServiceImpl2<>(delegate));
    assertThat(mBean, is(instanceOf(mBeanInterface)));
    verifyMBean(mBean);
    verify(delegate, times(1)).say("you");
  }

  private void verifyMBean(TerracottaMBean mBean) {
    assertThat(mBean, is(
        either(instanceOf(MyService.class))
            .or(instanceOf(MyService2.class))));
    assertThat(mBean.getInterfaceClassName(), is(
        either(equalTo(MyService.class.getName() + "MBean"))
            .or(equalTo(MyService.class.getName() + "2MBean"))));
    assertThat(mBean.isEnabled(), is(true));
    assertThat(mBean.isNotificationBroadcaster(), is(false));
    assertThat(mBean::reset, is(not(throwing())));

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
