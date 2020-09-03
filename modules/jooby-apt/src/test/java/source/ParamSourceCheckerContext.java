package source;

import io.jooby.MockContext;
import io.jooby.ParamSource;
import io.jooby.Value;

import java.util.function.Consumer;

public class ParamSourceCheckerContext extends MockContext {

  private final Consumer<ParamSource[]> onLookup;

  public ParamSourceCheckerContext(Consumer<ParamSource[]> onLookup) {
    this.onLookup = onLookup;
  }

  @Override
  public Value lookup(String name, ParamSource... sources) {
    onLookup.accept(sources);
    return super.lookup(name, sources);
  }
}
