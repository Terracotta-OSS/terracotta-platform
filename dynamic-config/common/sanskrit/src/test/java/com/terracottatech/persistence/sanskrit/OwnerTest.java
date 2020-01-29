/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.Closeable;
import java.io.IOException;

import static com.terracottatech.persistence.sanskrit.Owner.own;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class OwnerTest {
  @Mock
  private Closeable contained;

  @Test
  @SuppressWarnings("try")
  public void closesResourceIfNotReleased() throws Exception {
    try (Owner<Closeable, IOException> owner = own(contained, IOException.class)) {
    }

    verify(contained).close();
  }

  @Test
  public void doesNotCloseResourceIfReleased() throws Exception {
    try (Owner<Closeable, IOException> owner = own(contained, IOException.class)) {
      owner.release();
    }

    verifyNoMoreInteractions(contained);
  }

  @Test
  public void borrowHasNoImpactOnClose() throws Exception {
    try (Owner<Closeable, IOException> owner = own(contained, IOException.class)) {
      owner.borrow();
    }

    verify(contained).close();
  }

  @Test
  public void borrowHasNoImpactOnRelease() throws Exception {
    try (Owner<Closeable, IOException> owner = own(contained, IOException.class)) {
      owner.borrow();
      owner.release();
    }

    verifyNoMoreInteractions(contained);
  }

  @Test
  public void copeWithNullResource() throws Exception {
    try (Owner<Closeable, IOException> owner = own(null, IOException.class)) {
      assertNull(owner.borrow());
    }
  }

  @Test
  public void copeWithNullResourceRelease() throws Exception {
    try (Owner<Closeable, IOException> owner = own(null, IOException.class)) {
      assertNull(owner.borrow());
      assertNull(owner.release());
    }
  }
}
