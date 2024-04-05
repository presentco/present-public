package present.live.server;

import present.proto.DispatchCommentRequest;
import present.proto.Empty;
import present.proto.LiveService;
import java.io.IOException;
import javax.inject.Inject;

public class LiveServiceImpl implements LiveService {

  private final CommentDispatcher commentDispatcher;

  @Inject public LiveServiceImpl(CommentDispatcher commentDispatcher) {
    this.commentDispatcher = commentDispatcher;
  }

  @Override
  public Empty dispatchComment(DispatchCommentRequest dispatchCommentRequest)
      throws IOException {
    // TODO: Verify that this call came from an internal server.
    commentDispatcher.dispatch(dispatchCommentRequest);

    return new Empty();
  }
}
