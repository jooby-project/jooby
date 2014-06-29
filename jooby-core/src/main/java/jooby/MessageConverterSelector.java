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

@Singleton
public class MessageConverterSelector {

  private Set<MessageConverter> converters;

  private Map<MediaType, MessageConverter> converterMap = new HashMap<>();

  @Inject
  public MessageConverterSelector(final Set<MessageConverter> converters) {
    checkState(converters != null && converters.size() > 0, "No message converters were found.");
    this.converters = converters;
    converters.forEach(c -> c.types().forEach(t -> converterMap.put(t, c)));
  }

  public Optional<MessageConverter> get(final Class<?> type, final List<MediaType> supported) {
    for (MessageConverter converter : converters) {
      for (MediaType it : supported) {
        if (converter.types().contains(it)) {
          return Optional.of(converter);
        }
      }
    }
    return Optional.empty();
  }

  public Optional<MessageConverter> select(final List<MediaType> supported) {
    return select(supported, null);
  }

  public Optional<MessageConverter> select(final List<MediaType> supported,
      final MessageConverter defaultConverter) {
    for (MediaType mediaType : supported) {
      MessageConverter converter = converterMap.get(mediaType);
      if (converter != null) {
        return Optional.of(converter);
      }
    }
    // degrade lookup a bit
    Matcher matcher = MediaType.matcher(supported);
    TreeMap<MediaType, MessageConverter> matches = new TreeMap<>();
    for (MessageConverter converter : converters) {
      Optional<MediaType> match = matcher.first(converter.types());
      match.ifPresent((m) -> matches.putIfAbsent(m, converter));
    }
    if (matches.isEmpty()) {
      return Optional.ofNullable(defaultConverter);
    }
    Map.Entry<MediaType, MessageConverter> entry = matches.firstEntry();
    return Optional.of(new ForwardingMessageConverter(entry.getValue(), entry.getKey()));
  }

}
