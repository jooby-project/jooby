package org.jooby.servlet;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jooby.spi.Dispatcher;

@SuppressWarnings("serial")
@MultipartConfig
public class ServletHandler extends HttpServlet {

  private Dispatcher dispatcher;
  private String tmpdir;

  public ServletHandler(final Dispatcher dispatcher, final String tmpdir) {
    this.dispatcher = requireNonNull(dispatcher, "A dispatcher is required.");
    this.tmpdir = requireNonNull(tmpdir, "A tmpdir is required.");
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse rsp)
      throws ServletException, IOException {
    try {
      dispatcher.handle(new ServletServletRequest(req, tmpdir),
          new ServletServletResponse(req, rsp));
    } catch (Exception ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    }
  }

  @Override
  protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doHead(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doOptions(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doPut(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doTrace(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

}
