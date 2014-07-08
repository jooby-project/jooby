package jooby;

import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import jooby.MediaType.Matcher;
import jooby.internal.ForwardingMessageConverter;

import com.google.common.base.Joiner;

@Singleton
public class BodyConverterSelector {

  private Set<BodyConverter> converters;

  private Map<MediaType, BodyConverter> converterMap = new HashMap<>();

  @Inject
  public BodyConverterSelector(final Set<BodyConverter> converters) {
    checkState(converters != null && converters.size() > 0, "No message converters were found.");
    this.converters = converters;
    converters.forEach(c -> c.types().forEach(t -> converterMap.put(t, c)));
  }

  public Optional<BodyConverter> get(final Class<?> type, final List<MediaType> supported) {
    for (BodyConverter converter : converters) {
      for (MediaType it : supported) {
        if (converter.types().contains(it)) {
          return Optional.of(converter);
        }
      }
    }
    return Optional.empty();
  }

  public BodyConverter getOrThrow(final Iterable<MediaType> candidates, final HttpStatus status) {
    return get(candidates)
        .orElseThrow(
            () -> new HttpException(status, Joiner.on(", ").join(candidates))
        );
  }

  public Optional<BodyConverter> get(final Iterable<MediaType> candidates) {
    for (MediaType mediaType : candidates) {
      BodyConverter converter = converterMap.get(mediaType);
      if (converter != null) {
        return Optional.of(converter);
      }
    }
    // degrade lookup
    Matcher matcher = MediaType.matcher(candidates);
    TreeMap<MediaType, BodyConverter> matches = new TreeMap<>();
    for (BodyConverter converter : converters) {
      matcher.first(converter.types()).ifPresent((m) -> matches.putIfAbsent(m, converter));
    }
    if (matches.isEmpty()) {
      return Optional.empty();
    }
    Map.Entry<MediaType, BodyConverter> entry = matches.firstEntry();
    return Optional.of(new ForwardingMessageConverter(entry.getValue(), entry.getKey()));
  }

}
