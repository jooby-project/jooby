package org.jooby.test;

import static java.util.Objects.requireNonNull;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;

import com.google.common.base.Throwables;

@SuppressWarnings({"rawtypes", "unchecked" })
public class MockUnit {

  public class ConstructorBuilder<T> {

    private Class[] types;

    private Class<T> type;

    public ConstructorBuilder(final Class<T> type) {
      this.type = type;
    }

    public T build(final Object... args) throws Exception {
      mockClasses.add(type);
      if (types == null) {
        types = new Class[args.length];
        for (int i = 0; i < types.length; i++) {
          types[i] = args[i].getClass();
        }
      }
      T mock = PowerMock.createMockAndExpectNew(type, types, args);
      partialMocks.add(mock);
      return mock;
    }

    public ConstructorBuilder<T> args(final Class... types) {
      this.types = types;
      return this;
    }

  }

  public interface Block {

    public void run(MockUnit unit) throws Throwable;

  }

  private List<Object> mocks = new LinkedList<>();

  private List<Object> partialMocks = new LinkedList<>();

  private Map<Class, Object> globalMock = new LinkedHashMap<>();

  private Map<Class, List<Capture<Object>>> captures = new LinkedHashMap<>();

  private List<Class> mockClasses = new LinkedList<>();

  private List<Block> blocks = new LinkedList<>();

  public MockUnit(final Class... types) {
    this(false, types);
  }

  public MockUnit(final boolean strict, final Class... types) {
    Arrays.stream(types).forEach(type -> {
      registerMock(type);
    });
  }

  public <T> T capture(final Class<T> type) {
    Capture<Object> capture = new Capture<>();
    List<Capture<Object>> captures = this.captures.get(type);
    if (captures == null) {
      captures = new ArrayList<>();
      this.captures.put(type, captures);
    }
    captures.add(capture);
    return (T) EasyMock.capture(capture);
  }

  public <T> List<T> captured(final Class<T> type) {
    List<Capture<Object>> captureList = this.captures.get(type);
    List<T> result = new LinkedList<>();
    captureList.forEach(it -> result.add((T) it.getValue()));
    return result;
  }

  public <T> Class<T> mockStatic(final Class<T> type) {
    if (mockClasses.add(type)) {
      PowerMock.mockStatic(type);
      mockClasses.add(type);
    }
    return type;
  }

  public <T> Class<T> mockStaticPartial(final Class<T> type, final String... names) {
    if (mockClasses.add(type)) {
      PowerMock.mockStaticPartial(type, names);
      mockClasses.add(type);
    }
    return type;
  }

  public <T> T partialMock(final Class<T> type, final String... methods) {
    T mock = PowerMock.createPartialMock(type, methods);
    partialMocks.add(mock);
    return mock;
  }

  public <T> T partialMock(final Class<T> type, final String method, final Class<?> firstArg) {
    T mock = PowerMock.createPartialMock(type, method, firstArg);
    partialMocks.add(mock);
    return mock;
  }

  public <T> T partialMock(final Class<T> type, final String method, final Class t1,
      final Class t2) {
    T mock = PowerMock.createPartialMock(type, method, t1, t2);
    partialMocks.add(mock);
    return mock;
  }

  public <T> T mock(final Class<T> type) {
    return mock(type, false);
  }

  public <T> T powerMock(final Class<T> type) {
    T mock = PowerMock.createMock(type);
    partialMocks.add(mock);
    return mock;
  }

  public <T> T mock(final Class<T> type, final boolean strict) {
    if (Modifier.isFinal(type.getModifiers())) {
      T mock = PowerMock.createMock(type);
      partialMocks.add(mock);
      return mock;
    } else {

      T mock = strict ? createStrictMock(type) : createMock(type);
      mocks.add(mock);
      return mock;
    }
  }

  public <T> T registerMock(final Class<T> type) {
    T mock = mock(type);
    globalMock.put(type, mock);
    return mock;
  }

  public <T> T registerMock(final Class<T> type, final T mock) {
    globalMock.put(type, mock);
    return mock;
  }

  public <T> T get(final Class<T> type) {
    return (T) requireNonNull(globalMock.get(type), "Mock not found: " + type);
  }

  public MockUnit expect(final Block block) {
    blocks.add(requireNonNull(block, "A block is required."));
    return this;
  }

  public MockUnit run(final Block... blocks) throws Exception {

    for (Block block : this.blocks) {
      try {
        block.run(this);
      } catch (Exception | AssertionError ex) {
        throw ex;
      } catch (Throwable ex) {
        Throwables.propagate(ex);
      }
    }

    mockClasses.forEach(PowerMock::replay);
    partialMocks.forEach(PowerMock::replay);
    mocks.forEach(EasyMock::replay);

    for (Block main : blocks) {
      try {
        main.run(this);
      } catch (Exception | AssertionError ex) {
        throw ex;
      } catch (Throwable ex) {
        Throwables.propagate(ex);
      }
    }

    mocks.forEach(EasyMock::verify);
    partialMocks.forEach(PowerMock::verify);
    mockClasses.forEach(PowerMock::verify);

    return this;
  }

  public <T> T mockConstructor(final Class<T> type, final Class<?>[] paramTypes,
      final Object... args) throws Exception {
    mockClasses.add(type);
    T mock = PowerMock.createMockAndExpectNew(type, paramTypes, args);
    partialMocks.add(mock);
    return mock;
  }

  public <T> T mockConstructor(final Class<T> type, final Object... args) throws Exception {
    Class[] types = new Class[args.length];
    for (int i = 0; i < types.length; i++) {
      types[i] = args[i].getClass();
    }
    return mockConstructor(type, types, args);
  }

  public <T> ConstructorBuilder<T> constructor(final Class<T> type) {
    return new ConstructorBuilder<T>(type);
  }

}
