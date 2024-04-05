package present.server;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.comment.Comment;
import present.server.model.comment.Comments;

import static org.junit.Assert.assertEquals;

/**
 * Tests comment hashing.
 * @author Gabrielle A. Taylor {gabrielle@present.co}
 */
public class CommentHashTest {

  private static final Logger logger = LoggerFactory.getLogger(CommentHashTest.class);

  @Test public void hashComment() throws InterruptedException {
    Comment comment1 = new Comment();
    comment1.text = "Hello world!";
    String hash1 = Comments.hashText(comment1);
    logger.info("Comment: {} \n Hash: {}", comment1, hash1);
    assertEquals(hash1, Comments.hashText(comment1));
    assertEquals(hash1, Comments.hashText(comment1));

    Comment comment2 = new Comment();
    comment2.text = "Words words words. 🙂";
    String hash2 = Comments.hashText(comment2);
    logger.info("Comment: {} \n Hash: {}", comment2, hash2);
    assertEquals(hash2, Comments.hashText(comment2));
    assertEquals(hash2, Comments.hashText(comment2));

    Comment comment3 = new Comment();
    comment3.text = "";
    String hash3 = Comments.hashText(comment3);
    logger.info("Comment: {} \n Hash: {}", comment3, hash3);
    assertEquals(hash3, Comments.hashText(comment3));
    assertEquals(hash3, Comments.hashText(comment3));
  }

}