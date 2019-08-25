package io.jooby.banner;

import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.inject.Provider;

import static com.github.lalyos.jfiglet.FigletFont.convertOneLine;
import static io.jooby.banner.BannerModule.fontPath;
import static io.jooby.banner.BannerModule.rtrim;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class BannerModuleTest {

  @Test
  public void install() throws Exception {
    Jooby app = setup("Jooby", "2.0.0");

    new BannerModule("banner").install(app);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<ServiceKey<String>> keyCaptor = ArgumentCaptor.forClass(ServiceKey.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Provider<String>> providerCaptor = ArgumentCaptor.forClass(Provider.class);
    verify(app.getServices()).put(keyCaptor.capture(), providerCaptor.capture());

    assertEquals(String.class, keyCaptor.getValue().getType());
    assertEquals("application.banner", keyCaptor.getValue().getName());
    assertEquals(rtrim(convertOneLine(fontPath("speed"), "banner")), providerCaptor.getValue().get());

    verify(app).onStarting(Mockito.any(SneakyThrows.Runnable.class));
  }

  @Test
  public void trimEnd() throws Exception {
    Jooby app = setup("Jooby", "2.0.0");

    new BannerModule("banner      ").install(app);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Provider<String>> providerCaptor = ArgumentCaptor.forClass(Provider.class);
    verify(app.getServices()).put(Mockito.<ServiceKey<String>>any(), providerCaptor.capture());

    String banner = providerCaptor.getValue().get();
    assertFalse(Character.isWhitespace(banner.charAt(banner.length() - 1)));
  }

  @Test
  public void print() throws Exception {
    Jooby app = setup("Jooby", "2.0.0");

    new BannerModule("banner").install(app);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Provider<String>> providerCaptor = ArgumentCaptor.forClass(Provider.class);
    verify(app.getServices()).put(Mockito.<ServiceKey<String>>any(), providerCaptor.capture());

    ArgumentCaptor<SneakyThrows.Runnable> runnableCaptor = ArgumentCaptor.forClass(SneakyThrows.Runnable.class);
    verify(app).onStarting(runnableCaptor.capture());

    runnableCaptor.getValue().run();
    verify(app.getLog()).info("\n{} v{}\n", providerCaptor.getValue().get(), "2.0.0");
  }

  @Test
  public void font() throws Exception {
    Jooby app = setup("Jooby", "2.0.0");

    new BannerModule("banner").font("doom").install(app);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Provider<String>> providerCaptor = ArgumentCaptor.forClass(Provider.class);
    verify(app.getServices()).put(Mockito.<ServiceKey<String>>any(), providerCaptor.capture());

    assertEquals(rtrim(convertOneLine(fontPath("doom"), "banner")), providerCaptor.getValue().get());
  }

  @Test
  public void defprint() throws Exception {
    Jooby app = setup("Jooby", "2.0.0");

    new BannerModule().install(app);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Provider<String>> providerCaptor = ArgumentCaptor.forClass(Provider.class);
    verify(app.getServices()).put(Mockito.<ServiceKey<String>>any(), providerCaptor.capture());

    assertEquals(rtrim(convertOneLine(fontPath("speed"), "Jooby")), providerCaptor.getValue().get());
  }

  private Jooby setup(String appName, String appVersion) {
    ServiceRegistry registry = mock(ServiceRegistry.class);
    when(registry.put(Mockito.<ServiceKey<String>>any(), Mockito.<Provider<String>>any())).thenReturn(null);

    Logger logger = mock(Logger.class);
    doNothing().when(logger).info(anyString(), any(), any());

    Jooby app = mock(Jooby.class);
    when(app.getName()).thenReturn(appName);
    when(app.getVersion()).thenReturn(appVersion);
    when(app.getServices()).thenReturn(registry);
    when(app.getLog()).thenReturn(logger);
    when(app.onStarting(any(SneakyThrows.Runnable.class))).thenReturn(app);

    return app;
  }
}
