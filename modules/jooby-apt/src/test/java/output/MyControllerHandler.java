package output;

import io.jooby.Context;
import io.jooby.Reified;
import io.jooby.Route;

import javax.annotation.Nonnull;
import javax.inject.Provider;

public class MyControllerHandler implements Route.Handler {

  private Provider<MyController> provider;

  public MyControllerHandler(Provider<MyController> provider) {
    this.provider = provider;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    return provider.get().doIt(ctx.body(Reified.map(String.class, Object.class)));
  }
}