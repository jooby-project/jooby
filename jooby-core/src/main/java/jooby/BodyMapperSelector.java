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
public class BodyMapperSelector {

  private Set<BodyMapper> converters;

  private Map<MediaType, BodyMapper> converterMap = new HashMap<>();

  @Inject
  public BodyMapperSelector(final Set<BodyMapper> converters) {
    checkState(converters != null && converters.size() > 0, "No message converters were found.");
    this.converters = converters;
    converters.forEach(c -> c.types().forEach(t -> converterMap.put(t, c)));
  }

  public Optional<BodyMapper> get(final Class<?> type, final List<MediaType> supported) {
    for (BodyMapper converter : converters) {
      for (MediaType it : supported) {
        if (converter.types().contains(it)) {
          return Optional.of(converter);
        }
      }
    }
    return Optional.empty();
  }

  public BodyMapper getOrThrow(final Iterable<MediaType> candidates, final HttpStatus status) {
    return get(candidates)
        .orElseThrow(
            () -> new HttpException(status, Joiner.on(", ").join(candidates))
        );
  }

  public Optional<BodyMapper> get(final Iterable<MediaType> candidates) {
    for (MediaType mediaType : candidates) {
      BodyMapper converter = converterMap.get(mediaType);
      if (converter != null) {
        return Optional.of(converter);
      }
    }
    // degrade lookup
    Matcher matcher = MediaType.matcher(candidates);
    TreeMap<MediaType, BodyMapper> matches = new TreeMap<>();
    for (BodyMapper converter : converters) {
      matcher.first(converter.types()).ifPresent((m) -> matches.putIfAbsent(m, converter));
    }
    if (matches.isEmpty()) {
      return Optional.empty();
    }
    Map.Entry<MediaType, BodyMapper> entry = matches.firstEntry();
    return Optional.of(new ForwardingMessageConverter(entry.getValue(), entry.getKey()));
  }

}
