package jooby.internal.mvc;

import java.lang.reflect.Executable;
import java.util.List;

public interface ParamProvider {

  List<Param> parameters(Executable exec);

}
