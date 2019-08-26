package output;

import examples.CoroutineApp;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.util.ASMifier;

public class ASMPrinter {

  @Test
  public void coroutines() throws Exception {
    ASMifier.main(new String[] {CoroutineApp.class.getName() + "$1"});
  }

  @Test
  public void mvcModule() throws Exception {
    ASMifier.main(new String[] {MyMvcModule.class.getName() });
  }
}

