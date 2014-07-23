package jooby.internal;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;

import jooby.BodyConverter;
import jooby.MediaType;
import jooby.MediaType.Matcher;

import com.google.common.annotations.Beta;
import com.google.inject.TypeLiteral;

/**
 * Choose or select a {@link BodyConverter} using {@link MediaType media types.}. Examples:
 *
 * <pre>
 *   // selector with html and json converters
 *   selector = new BodyConverterSelector(Sets.newLinkedHashSet(html, json));
 *
 *   // asking for html, produces the html converter
 *   assertEquals(html, selector.get(MediaType.html));
 *
 *   // asking for json, produces the json converter
 *   assertEquals(json, selector.get(MediaType.json));
 *
 *   // asking for * / *, produces the first matching converter
 *   assertEquals(html, selector.get(MediaType.all));
 * </pre>
 *
 * @author edgar
 * @since 0.1.0
 */
@Beta
public class BodyConverterSelector {

  /**
   * The available converters in the system.
   */
  private final Set<BodyConverter> converters;

  /**
   * A map version of the available converters.
   */
  private final Map<MediaType, BodyConverter> converterMap = new HashMap<>();

  /**
   * Creates a new {@link BodyConverterSelector}.
   *
   * @param converters The available converter in the system.
   */
  @Inject
  public BodyConverterSelector(final Set<BodyConverter> converters) {
    checkState(converters != null, "No body converter was found.");
    this.converters = converters;
    converters.forEach(c -> c.types().forEach(t -> converterMap.putIfAbsent(t, c)));
  }

  public Optional<BodyConverter> forRead(final TypeLiteral<?> type,
      final Iterable<MediaType> candidates) {
    requireNonNull(type, "The type is required.");
    requireNonNull(candidates, "Media types candidates are required.");
    for (MediaType mediaType : candidates) {
      BodyConverter converter = converterMap.get(mediaType);
      if (converter != null && converter.canRead(type)) {
        return Optional.of(converter);
      }
    }
    // degrade lookup a bit
    Matcher matcher = MediaType.matcher(candidates);
    TreeMap<MediaType, BodyConverter> matches = new TreeMap<>();
    for (BodyConverter converter : converters) {
      Optional<MediaType> result = matcher.first(converter.types());
      if (result.isPresent() && converter.canRead(type)) {
        matches.putIfAbsent(result.get(), converter);
      }
    }
    if (matches.isEmpty()) {
      return Optional.empty();
    }
    Map.Entry<MediaType, BodyConverter> entry = matches.firstEntry();
    return Optional.of(new ForwardingBodyConverter(entry.getValue(), entry.getKey()));
  }

  public Optional<BodyConverter> forWrite(final Object message,
      final Iterable<MediaType> candidates) {
    requireNonNull(message, "A message is required.");
    requireNonNull(candidates, "Media types candidates are required.");

    Class<?> type = message.getClass();

    for (MediaType mediaType : candidates) {
      BodyConverter converter = converterMap.get(mediaType);
      if (converter != null && converter.canWrite(type)) {
        return Optional.of(converter);
      }
    }
    // degrade lookup a bit
    Matcher matcher = MediaType.matcher(candidates);
    TreeMap<MediaType, BodyConverter> matches = new TreeMap<>();
    for (BodyConverter converter : converters) {
      Optional<MediaType> result = matcher.first(converter.types());
      if (result.isPresent() && converter.canWrite(type)) {
        matches.putIfAbsent(result.get(), converter);
      }
    }
    if (matches.isEmpty()) {
      return Optional.empty();
    }
    Map.Entry<MediaType, BodyConverter> entry = matches.firstEntry();
    return Optional.of(new ForwardingBodyConverter(entry.getValue(), entry.getKey()));
  }

}
