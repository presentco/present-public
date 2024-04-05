package present.server;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.Translator;
import com.googlecode.objectify.impl.translate.TranslatorFactory;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import java.io.IOException;

/**
 * Automatically converts Wire objects to byte[]s.
 *
 * @author Bob Lee (bob@present.co)
 */
public class WireTranslatorFactory implements TranslatorFactory<Object, Blob> {

  @Override
  public Translator<Object, Blob> create(TypeKey<Object> tk, CreateContext ctx, Path path) {
    if (!tk.isAssignableTo(Message.class)) return null;
    ProtoAdapter adapter = ProtoAdapter.get(tk.getTypeAsClass());
    return new Translator<Object, Blob>() {
      @Override public Object load(Blob node, LoadContext ctx, Path path) throws SkipException {
        try {
          return adapter.decode(node.getBytes());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      @Override public Blob save(Object pojo, boolean index, SaveContext ctx, Path path)
          throws SkipException {
        return new Blob(adapter.encode(pojo));
      }
    };
  }
}
