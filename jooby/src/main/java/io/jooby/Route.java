/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

public interface Route {

  interface Decorator {
    @Nonnull Handler apply(@Nonnull Handler next);

    @Nonnull default Decorator then(@Nonnull Decorator next) {
      return h -> apply(next.apply(h));
    }

    @Nonnull default Handler then(@Nonnull Handler next) {
      return ctx -> apply(next).apply(ctx);
    }
  }

  interface Before extends Decorator {
    @Nonnull @Override default Handler apply(@Nonnull Handler next) {
      return ctx -> {
        before(ctx);
        return next.apply(ctx);
      };
    }

    void before(@Nonnull Context ctx) throws Exception;
  }

  interface After {

    @Nonnull default After then(@Nonnull After next) {
      return (ctx, result) -> apply(ctx, next.apply(ctx, result));
    }

    @Nonnull Object apply(@Nonnull Context ctx, Object result) throws Exception;
  }

  interface Handler extends Serializable {

    @Nonnull Object apply(@Nonnull Context ctx) throws Exception;

    @Nonnull default Object execute(@Nonnull Context ctx) {
      try {
        return apply(ctx);
      } catch (Throwable x) {
        ctx.sendError(x);
        return x;
      }
    }

    @Nonnull default Handler then(After next) {
      return ctx -> next.apply(ctx, apply(ctx));
    }
  }

  interface ErrorHandler {

    static ErrorHandler log(Logger log, StatusCode... quiet) {
      Set<StatusCode> silent = Set.of(quiet);
      return (ctx, cause, statusCode) -> {
        String message = format("%s %s %s %s", ctx.method(), ctx.pathString(),
            statusCode.value(), statusCode.reason());
        if (silent.contains(statusCode)) {
          log.info(message, cause);
        } else {
          log.error(message, cause);
        }
      };
    }

    ErrorHandler DEFAULT = (ctx, cause, statusCode) -> {
      String message = cause.getMessage();
      StringBuilder html = new StringBuilder("<!doctype html>\n")
          .append("<html>\n")
          .append("<head>\n")
          .append("<meta charset=\"utf-8\">\n")
          .append("<style>\n")
          .append("body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n")
          .append("h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n")
          .append("h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n")
          .append("footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n")
          .append("hr {background-color: #f7f7f9;}\n")
          .append("div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n")
          .append("p {padding-left: 20px;}\n")
          .append("p.tab {padding-left: 40px;}\n")
          .append("</style>\n")
          .append("<title>")
          .append(statusCode)
          .append("</title>\n")
          .append("<body>\n")
          .append("<h1>").append(statusCode.reason()).append("</h1>\n")
          .append("<hr>\n");

      if (message != null && !message.equals(statusCode.toString())) {
        html.append("<h2>message: ").append(message).append("</h2>\n");
      }
      html.append("<h2>status code: ").append(statusCode.value()).append("</h2>\n");

      html.append("</body>\n")
          .append("</html>");

      ctx.statusCode(statusCode)
          .sendText(html.toString());
    };

    @Nonnull void apply(@Nonnull Context ctx, @Nonnull Throwable cause,
        @Nonnull StatusCode statusCode);

    @Nonnull default ErrorHandler then(@Nonnull ErrorHandler next) {
      return (ctx, cause, statusCode) -> {
        apply(ctx, cause, statusCode);
        if (!ctx.isResponseStarted()) {
          next.apply(ctx, cause, statusCode);
        }
      };
    }
  }

  Handler NOT_FOUND = ctx -> ctx.sendError(new Err(StatusCode.NOT_FOUND));

  Handler METHOD_NOT_ALLOWED = ctx -> ctx.sendError(new Err(StatusCode.METHOD_NOT_ALLOWED));

  Handler FAVICON = ctx -> ctx.sendStatusCode(StatusCode.NOT_FOUND);

  @Nonnull String pattern();

  @Nonnull String method();

  @Nonnull List<String> pathKeys();

  @Nonnull Handler handler();

  @Nonnull Handler pipeline();

  @Nonnull Renderer renderer();

  @Nonnull Type returnType();
}