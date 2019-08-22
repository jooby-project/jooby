package io.jooby.banner;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.inject.Provider;
import java.util.HashMap;
import java.util.Map;

import static com.github.lalyos.jfiglet.FigletFont.convertOneLine;
import static io.jooby.banner.BannerModule.fontPath;
import static io.jooby.banner.BannerModule.rtrim;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class BannerModuleTest {

  @Test
  public void install() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("application.name", "Jooby");
    map.put("application.version", "2.0.0");

    Config config = ConfigFactory.parseMap(map);

    ServiceRegistry registry = mock(ServiceRegistry.class);
    when(registry.put(Mockito.<ServiceKey<String>>any(), Mockito.<Provider<String>>any())).thenReturn(null);

    Logger logger = mock(Logger.class);
    doNothing().when(logger).info(anyString(), any(), any());

    Jooby app = mock(Jooby.class);
    when(app.getConfig()).thenReturn(config);
    when(app.getServices()).thenReturn(registry);
    when(app.getLog()).thenReturn(logger);
    when(app.onStarting(any(SneakyThrows.Runnable.class))).thenReturn(app);

    new BannerModule().install(app);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<ServiceKey<String>> keyCaptor = ArgumentCaptor.forClass(ServiceKey.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Provider<String>> providerCaptor = ArgumentCaptor.forClass(Provider.class);
    verify(registry).put(keyCaptor.capture(), providerCaptor.capture());

    assertEquals(String.class, keyCaptor.getValue().getType());
    assertEquals("application.banner", keyCaptor.getValue().getName());
    assertEquals(rtrim(convertOneLine(fontPath("speed"), map.get("application.name"))), providerCaptor.getValue().get());

    ArgumentCaptor<SneakyThrows.Runnable> runnableCaptor = ArgumentCaptor.forClass(SneakyThrows.Runnable.class);
    verify(app).onStarting(runnableCaptor.capture());

    runnableCaptor.getValue().run();
    verify(logger).info("\n{} v{}\n", providerCaptor.getValue().get(), map.get("application.version"));
  }

  @Test
  public void trimEnd() throws Exception {

  }

  @Test
  public void print() throws Exception {

  }

  @Test
  public void font() throws Exception {

  }

  @Test
  public void defprint() throws Exception {

  }
}
