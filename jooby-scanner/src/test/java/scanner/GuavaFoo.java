package scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractIdleService;

public class GuavaFoo extends AbstractIdleService {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  protected void startUp() throws Exception {
    log.info("Starting {}", getClass());
  }

  @Override
  protected void shutDown() throws Exception {
    log.info("Stopping {}", getClass());
  }

}
