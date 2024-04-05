package cast.placeholder;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Iteration;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import java.io.Writer;

/**
 * Treats 0 as falsey.
 *
 * @author Bob Lee (bob@present.co)
 */
public class CastMustacheFactory extends DefaultMustacheFactory {
  public CastMustacheFactory() {
    setObjectHandler(new ReflectionObjectHandler() {
      @Override public Writer falsey(
          Iteration iteration, Writer writer, Object object, Object[] scopes) {
        if (object instanceof Integer) {
          if ((int) object != 0) return writer;
        }
        return super.falsey(iteration, writer, object, scopes);
      }
    });
  }
}
