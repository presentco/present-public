package present.wire.rpc.core;

public class ClientException extends RpcException {

  public ClientException(String message) {
    super(Error.Type.CLIENT, message);
  }
}
