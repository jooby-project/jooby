package org.jooby.internal;

import com.google.inject.Injector;

public interface AppManager {

  int STOP = -1;

  int RESTART = 0;

  Injector execute(int action);

}
