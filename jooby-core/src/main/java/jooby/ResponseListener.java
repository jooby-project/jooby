package jooby;

public interface ResponseListener {

  void beforeWrite(Response response) throws Exception;

}
