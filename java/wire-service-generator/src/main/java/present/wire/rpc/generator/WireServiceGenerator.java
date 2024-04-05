package present.wire.rpc.generator;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.java.JavaGenerator;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Rpc;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;
import com.squareup.wire.schema.Service;
import java.io.File;
import java.io.IOException;
import javax.lang.model.element.Modifier;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;

public final class WireServiceGenerator {
  private final Log log;
  private final ImmutableSet<String> sources;
  private final ImmutableSet<String> protos;
  private final String generatedSourceDirectory;

  public WireServiceGenerator(Log log, ImmutableSet<String> sources,
      ImmutableSet<String> protos, String generatedSourceDirectory) {
    this.log = log;
    this.sources = sources;
    this.protos = protos;
    this.generatedSourceDirectory = generatedSourceDirectory;
  }

  public void execute() throws IOException {
    Schema schema = loadSchema();
    JavaGenerator javaGenerator = JavaGenerator.get(schema);
    for (ProtoFile protoFile : schema.protoFiles()) {
      for (Service service : protoFile.services()) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ClassName javaTypeName = (ClassName) javaGenerator.typeName(service.type());
        TypeSpec typeSpec = createJavaInterface(javaGenerator, service);
        writeJavaFile(javaTypeName, typeSpec, service.location(), stopwatch);
      }
    }
  }

  private Schema loadSchema() throws IOException {
    Stopwatch stopwatch = Stopwatch.createStarted();
    SchemaLoader loader = new SchemaLoader();
    for (String source : sources) loader.addSource(new File(source));
    for (String proto : protos) loader.addProto(proto);
    Schema schema = loader.load();
    log.info("Loaded %s proto files in %s", schema.protoFiles().size(), stopwatch);
    return schema;
  }

  private void writeJavaFile(ClassName javaTypeName, TypeSpec typeSpec, Location location,
      Stopwatch stopwatch) throws IOException {
    JavaFile.Builder builder = JavaFile.builder(javaTypeName.packageName(), typeSpec)
        .addFileComment("Code generated by $L, do not edit.", WireServiceGenerator.class.getName());
    if (location != null) {
      builder.addFileComment("\nSource file: $L", location.path());
    }
    JavaFile javaFile = builder.build();
    try {
      javaFile.writeTo(new File(generatedSourceDirectory));
    } catch (IOException e) {
      throw new IOException("Failed to write " + javaFile.packageName + "."
          + javaFile.typeSpec.name + " to " + generatedSourceDirectory, e);
    }
    log.info("Generated %s in %s", javaTypeName, stopwatch);
  }

  private TypeSpec createJavaInterface(JavaGenerator javaGenerator, Service service) {
    ClassName interfaceName = (ClassName) javaGenerator.typeName(service.type());

    TypeSpec.Builder typeBuilder = TypeSpec.interfaceBuilder(interfaceName.simpleName());
    typeBuilder.addModifiers(Modifier.PUBLIC);

    for (Rpc rpc : service.rpcs()) {
      ProtoType requestType = rpc.requestType();
      TypeName requestJavaType = javaGenerator.typeName(requestType);
      ProtoType responseType = rpc.responseType();
      TypeName responseJavaType = javaGenerator.typeName(responseType);

      MethodSpec.Builder rpcBuilder = MethodSpec.methodBuilder(upperToLowerCamel(rpc.name()));
      rpcBuilder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
      rpcBuilder.returns(responseJavaType);
      rpcBuilder.addException(IOException.class);

      rpcBuilder.addParameter(requestJavaType, "request");

      typeBuilder.addMethod(rpcBuilder.build());
    }

    return typeBuilder.build();
  }

  private String upperToLowerCamel(String string) {
    return UPPER_CAMEL.to(LOWER_CAMEL, string);
  }

  interface Log {
    void info(String format, Object... args);
  }
}