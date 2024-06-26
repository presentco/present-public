package present.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.squareup.wire.WireTypeAdapterFactory;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import okio.ByteString;
import present.proto.AddContactsRequest;
import present.proto.ContactRequest;

/**
 * Generates JSON suitable for logging. Truncates long strings, elides bytes.
 */
public class GsonLogging {

  private static final Pattern REDACTED_PROPERTIES = Pattern.compile(
      "^.*(phone).*$", Pattern.CASE_INSENSITIVE);

  private static Gson gson = new GsonBuilder().registerTypeAdapterFactory(
      new LoggingTypeAdapterFactory()).create();

  private static final int MAX_JSON_LENGTH = 1024;
  private static final int MAX_VALUE_LENGTH = 256;

  public static String toJson(Object o) {
    StringWriter out = new StringWriter();
    JsonWriter jsonWriter = new LoggingJsonWriter(new CappedWriter(out));
    jsonWriter.setIndent("  ");
    try {
      gson.toJson(o, o.getClass(), jsonWriter);
      return out.toString();
    } catch (JsonIOException e) {
      // Assume this is a StopWriting exception
      out.write("...");
      return out.toString();
    }
  }

  private static class CappedWriter extends FilterWriter {

    private int length = 0;

    public CappedWriter(Writer out) {
      super(out);
    }

    @Override public void write(int c) throws IOException {
      if (length >= MAX_JSON_LENGTH) throw new StopWriting();
      super.write(c);
      length++;
    }

    @Override public void write(char[] cbuf, int off, int len) throws IOException {
      if (length >= MAX_JSON_LENGTH) throw new StopWriting();
      if (length + len > MAX_JSON_LENGTH) {
        int newLen = MAX_JSON_LENGTH - length;
        super.write(cbuf, off, newLen);
        length += newLen;
        flush();
        throw new StopWriting();
      }
      super.write(cbuf, off, len);
      length += len;
    }

    @Override public void write(String str, int off, int len) throws IOException {
      if (length >= MAX_JSON_LENGTH) throw new StopWriting();
      if (length + len > MAX_JSON_LENGTH) {
        int newLen = MAX_JSON_LENGTH - length;
        super.write(str, off, newLen);
        length += newLen;
        flush();
        throw new StopWriting();
      }
      super.write(str, off, len);
      length += len;
    }
  }

  private static class StopWriting extends IOException {}

  private static class LoggingTypeAdapterFactory implements TypeAdapterFactory {
    private TypeAdapterFactory delegate = new WireTypeAdapterFactory();
    @Override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if (type.getRawType().equals(ByteString.class)) {
        return (TypeAdapter<T>) new ByteStringTypeAdapter();
      }
      return delegate.create(gson, type);
    }
  }

  private static class ByteStringTypeAdapter extends TypeAdapter<ByteString> {
    @Override public void write(JsonWriter out, ByteString value) throws IOException {
      if (value == null) {
        out.nullValue();
      } else {
        out.value("[bytes]");
      }
    }
    @Override public ByteString read(JsonReader in) throws IOException {
      throw new UnsupportedOperationException();
    }
  }

  private static class LoggingJsonWriter extends JsonWriter {

    private LoggingJsonWriter(Writer out) {
      super(out);
    }

    private String currentName;

    @Override public JsonWriter name(String name) throws IOException {
      this.currentName = name;
      return super.name(name);
    }

    @Override public JsonWriter value(String value) throws IOException {
      // Hide phone numbers.
      if (currentName != null && REDACTED_PROPERTIES.matcher(currentName).matches()) {
        return super.value("[redacted]");
      }

      if (value == null) return nullValue();
      if (value.length() > MAX_VALUE_LENGTH) value = value.substring(0, MAX_VALUE_LENGTH - 3) + "...";
      return super.value(value);
    }
  }

  public static void main(String[] args) {
    List<ContactRequest> contacts = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      contacts.add(new ContactRequest("1314", "Bob Lee",
          "Bob", "Lee"));
    }
    AddContactsRequest request = new AddContactsRequest(contacts);
    String json = toJson(contacts);
    System.out.println(json);
    System.out.println(json.length());
  }
}
