/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.guice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;
import com.google.inject.spi.Message;
import io.jooby.Reified;
import io.jooby.ServiceKey;
import io.jooby.exception.RegistryException;

public class GuiceRegistryTest {

  private Injector injector;
  private GuiceRegistry registry;

  @BeforeEach
  public void setUp() {
    injector = mock(Injector.class);
    registry = new GuiceRegistry(injector);
  }

  @Test
  public void requireClass() {
    Key<String> key = Key.get(String.class);
    when(injector.getInstance(key)).thenReturn("foo");

    assertEquals("foo", registry.require(String.class));
  }

  @Test
  public void requireClassAndName() {
    Key<String> key = Key.get(String.class, Names.named("bar"));
    when(injector.getInstance(key)).thenReturn("foo-bar");

    assertEquals("foo-bar", registry.require(String.class, "bar"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void requireReified() {
    Reified<List<String>> reified = Reified.list(String.class);
    Key<?> key = Key.get(reified.getType());
    when(injector.getInstance((Key<Object>) key)).thenReturn(Collections.singletonList("reified"));

    assertEquals(Collections.singletonList("reified"), registry.require(reified));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void requireReifiedAndName() {
    Reified<List<String>> reified = Reified.list(String.class);
    Key<?> key = Key.get(reified.getType(), Names.named("baz"));
    when(injector.getInstance((Key<Object>) key))
        .thenReturn(Collections.singletonList("reified-named"));

    assertEquals(Collections.singletonList("reified-named"), registry.require(reified, "baz"));
  }

  @Test
  public void requireServiceKeyWithoutName() {
    ServiceKey<String> serviceKey = ServiceKey.key(String.class);
    Key<String> key = Key.get(String.class);
    when(injector.getInstance(key)).thenReturn("service-no-name");

    assertEquals("service-no-name", registry.require(serviceKey));
  }

  @Test
  public void requireServiceKeyWithName() {
    ServiceKey<String> serviceKey = ServiceKey.key(String.class, "named-service");
    Key<String> key = Key.get(String.class, Names.named("named-service"));
    when(injector.getInstance(key)).thenReturn("service-named");

    assertEquals("service-named", registry.require(serviceKey));
  }

  @Test
  public void requireProvisionException() {
    Key<String> key = Key.get(String.class);
    ProvisionException provisionException =
        new ProvisionException(Collections.singleton(new Message("provision error")));
    when(injector.getInstance(key)).thenThrow(provisionException);

    RegistryException ex =
        assertThrows(RegistryException.class, () -> registry.require(String.class));
    assertTrue(ex.getMessage().contains("Provisioning of `" + key + "` resulted in exception"));
    assertEquals(provisionException, ex.getCause());
  }

  @Test
  public void requireConfigurationException() {
    Key<String> key = Key.get(String.class);
    ConfigurationException configException =
        new ConfigurationException(Collections.singleton(new Message("config error")));
    when(injector.getInstance(key)).thenThrow(configException);

    RegistryException ex =
        assertThrows(RegistryException.class, () -> registry.require(String.class));
    assertTrue(ex.getMessage().contains("Provisioning of `" + key + "` resulted in exception"));
    assertEquals(configException, ex.getCause());
  }
}
