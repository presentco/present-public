package present.server.tool;

import present.server.model.user.VerificationRequest;

import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * @author Pat Niemeyer (pat@pat.net)
 * Date: 2/21/18
 */
public class GenerateVerificationRequest {
  public static void main(String[] args) {
    against(STAGING_SERVER, () -> {
      VerificationRequest verificationRequest = VerificationRequest.clientIndependent();
      String code = verificationRequest.code();
      verificationRequest.email = "jane-" + code + "@present.co";
      verificationRequest.firstName = "Jane";
      verificationRequest.lastName = "User-" + code;
      verificationRequest.save();
      System.out.println(verificationRequest.email + ", " + verificationRequest.url());
    });
  }
}
