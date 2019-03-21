package io.jooby.internal.mvc;

import io.jooby.Context;
import io.jooby.Err;
import io.jooby.QueryString;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.Value;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;
import io.jooby.annotations.QueryParam;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QPoint {
}

class Poc {

  @POST
  @Path(("/body/json"))
  public Long getIt(@PathParam Long l) {
    return l;
  }

  @GET
  @Path("/{s}/{i}/{j}/{f}/{d}/{b}")
  public String mix(@PathParam String s, @PathParam Integer i, @PathParam double d, Context ctx,
      @PathParam long j, @PathParam Float f, @PathParam boolean b) {
    return ctx.pathString();
  }
}

class MvcHandlerImpl implements MvcHandler {

  private Provider<Poc> provider;

  public MvcHandlerImpl(Provider<Poc> provider) {
    this.provider = provider;
  }

  private String tryParam0(Context ctx, String desc) {
    try {
      return ctx.path("s").to(String.class);
    } catch (Err.Provisioning x) {
      throw x;
    } catch (Exception x) {
      throw new Err.Provisioning(desc, x);
    }
  }

  private Integer tryParam1(Context ctx, String desc) {
    try {
      return ctx.path("i").to(Integer.class);
    } catch (Err.Provisioning x) {
      throw x;
    } catch (Exception x) {
      throw new Err.Provisioning(desc, x);
    }
  }

  private double tryParam2(Context ctx, String desc) {
    try {
      return ctx.path("d").to(double.class);
    } catch (Err.Provisioning x) {
      throw x;
    } catch (Exception x) {
      throw new Err.Provisioning(desc, x);
    }
  }

  private Context tryParam3(Context ctx, String desc) {
    try {
      return ctx;
    } catch (Err.Provisioning x) {
      throw x;
    } catch (Exception x) {
      throw new Err.Provisioning(desc, x);
    }
  }

  private long tryParam4(Context ctx, String desc) {
    try {
      return ctx.path("j").to(long.class);
    } catch (Err.Provisioning x) {
      throw x;
    } catch (Exception x) {
      throw new Err.Provisioning(desc, x);
    }
  }

  private Float tryParam5(Context ctx, String desc) {
    try {
      return ctx.path("f").to(Float.class);
    } catch (Err.Provisioning x) {
      throw x;
    } catch (Exception x) {
      throw new Err.Provisioning(desc, x);
    }
  }

  private boolean tryParam6(Context ctx, String desc) {
    try {
      return ctx.path("b").to(boolean.class);
    } catch (Err.Provisioning x) {
      throw x;
    } catch (Exception x) {
      throw new Err.Provisioning(desc, x);
    }
  }

  public final Object[] args(Context ctx) {
    return new Object[]{tryParam0(ctx, "s: String"), tryParam1(ctx, ""), tryParam2(ctx, ""),
        tryParam3(ctx, ""), tryParam4(ctx, ""), tryParam5(ctx, ""), tryParam6(ctx, "")};
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    Poc target = provider.get();
    return target.mix(tryParam0(ctx, "s: String"), tryParam1(ctx, ""), tryParam2(ctx, ""),
        tryParam3(ctx, ""), tryParam4(ctx, ""), tryParam5(ctx, ""), tryParam6(ctx, ""));
  }
}

public class MvcHandlerASM {

  @Test
  public void compare() throws IOException, NoSuchMethodException, ClassNotFoundException {
    // Lio/jooby/Throwing$Supplier<Lio/jooby/mvc/NoTopLevelPath;>;
    //    ASMifier.main(new String[] {"-debug",MvcHandler.class.getName()});
//public String mix(@PathParam String s, @PathParam Integer i, @PathParam double d, Context ctx,
    //      @PathParam long j, @PathParam Float f, @PathParam boolean b) {
    Method handler = Poc.class.getDeclaredMethod("mix", String.class, Integer.class, double.class, Context.class, long.class, Float.class, boolean.class);
    MvcCompiler writer = new MvcCompiler();
    Class runtime = writer.compileClass(mvc(handler));

    System.out.println("Loaded: " + runtime);
    //    byte[] asm = writer.toByteCode(classname, handler);

    assertEquals(asmifier(new ClassReader(MvcHandlerImpl.class.getName())),
        asmifier(new ClassReader(writer.compile(mvc(handler)))));

  }

  private MvcMethod mvc(Method handler) {
    MvcMethod result = new MvcMethod();
    result.method = handler;
    return result;
  }

  private String asmifier(ClassReader reader) throws IOException {
    ByteArrayOutputStream buff = new ByteArrayOutputStream();
    Printer printer = new ASMifier();
    TraceClassVisitor traceClassVisitor =
        new TraceClassVisitor(null, printer, new PrintWriter(buff));

    reader.accept(traceClassVisitor, ClassReader.SKIP_DEBUG);

    return new String(buff.toByteArray());
  }

  private String listOf(org.objectweb.asm.Type owner) {
    StringBuilder signature = new StringBuilder(
        org.objectweb.asm.Type.getType(List.class).getDescriptor());
    signature.insert(signature.length() - 1, "<" + owner.getDescriptor() + ">");
    return signature.toString();
  }
}
