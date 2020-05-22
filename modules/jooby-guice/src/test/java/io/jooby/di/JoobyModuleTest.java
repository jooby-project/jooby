package io.jooby.di;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.AnnotatedConstantBindingBuilder;
import com.google.inject.binder.ConstantBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JoobyModuleTest {
  @Test
  @DisplayName("Guice should not break on empty list property. Empty list fallback to List<String>")
  public void issue1337() {
    List<String> emptyList = Collections.emptyList();
    Map<String, Object> map = new HashMap<>();
    map.put("some", emptyList);

    Config config = ConfigFactory.parseMap(map);

    Environment environment = mock(Environment.class);
    when(environment.getConfig()).thenReturn(config);

    ServiceRegistry registry = mock(ServiceRegistry.class);
    when(registry.entrySet()).thenReturn(Collections.emptySet());

    Jooby app = mock(Jooby.class);
    when(app.getEnvironment()).thenReturn(environment);
    when(app.getServices()).thenReturn(registry);

    Binder binder = mock(Binder.class);

    AnnotatedBindingBuilder<Environment> envbinding = mock(AnnotatedBindingBuilder.class);
    when(binder.bind(Environment.class)).thenReturn(envbinding);

    AnnotatedBindingBuilder<Config> confbinding = mock(AnnotatedBindingBuilder.class);
    when(binder.bind(Config.class)).thenReturn(confbinding);

    LinkedBindingBuilder emptyListBinding = mock(LinkedBindingBuilder.class);
    emptyListBinding.toInstance(emptyList);
    when(binder.bind(Key.get(Types.listOf(String.class), Names.named("some")))).thenReturn(emptyListBinding);
    when(binder.bind(Key.get(Types.listOf(Integer.class), Names.named("some")))).thenReturn(emptyListBinding);
    when(binder.bind(Key.get(Types.listOf(Long.class), Names.named("some")))).thenReturn(emptyListBinding);
    when(binder.bind(Key.get(Types.listOf(Float.class), Names.named("some")))).thenReturn(emptyListBinding);
    when(binder.bind(Key.get(Types.listOf(Double.class), Names.named("some")))).thenReturn(emptyListBinding);
    when(binder.bind(Key.get(Types.listOf(Boolean.class), Names.named("some")))).thenReturn(emptyListBinding);
    when(binder.bind(Key.get(Types.listOf(Object.class), Names.named("some")))).thenReturn(emptyListBinding);

    ConstantBindingBuilder constantBindingBuilder = mock(ConstantBindingBuilder.class);
    constantBindingBuilder.to("");
    AnnotatedConstantBindingBuilder constantBinding = mock(AnnotatedConstantBindingBuilder.class);
    when(constantBinding.annotatedWith(Names.named("some"))).thenReturn(constantBindingBuilder);
    when(binder.bindConstant()).thenReturn(constantBinding);

    JoobyModule module = new JoobyModule(app);

    module.configure(binder);

    verify(envbinding).toInstance(environment);
    verify(confbinding).toInstance(config);
  }

}
