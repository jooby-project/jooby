package output;

import examples.CoroutineApp;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.util.ASMifier;
import source.NullRoutes;

public class ASMPrinter {

  @Test
  public void coroutines() throws Exception {
    ASMifier.main(new String[] {CoroutineApp.class.getName() + "$1"});
  }

  @Test
  public void mvcModule() throws Exception {
    ASMifier.main(new String[] {MyMvcModule.class.getName() });
  }

  @Test
  public void mvcExtension() throws Exception {
    ASMifier.main(new String[] {MvcExtension.class.getName() });
  }

  @Test
  public void myController() throws Exception {
    ASMifier.main(new String[] {MyControllerHandler.class.getName() });
  }

  @Test
  public void nullRoutes() throws Exception {
    ASMifier.main(new String[] {NullRoutes.class.getName() });
  }
}

