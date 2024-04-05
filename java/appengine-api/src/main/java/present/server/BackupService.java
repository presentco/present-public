package present.server;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.environment.Environment;
import present.server.model.PresentEntities;

/**
 * Handles cron job request and dispatches backup requests to Google Cloud storage.
 *
 * @author Gabrielle Taylor (gabrielle@present.co)
 */
@Path("createBackup") public class BackupService {
  private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

  @GET public Response backup(@HeaderParam("X-Appengine-Cron") boolean cron) {

    // Check that request actually came from appengine cron job
    if (!cron) return Response.status(Response.Status.UNAUTHORIZED).build();

    // Get current platform for correct backup storage bucket.
    String appId = Environment.applicationId();
    logger.info("Creating backup for " + appId);

    // Create folder name for this backup
    DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd-'T'HH-mm-ss");
    String timestamp = LocalDateTime.now().format(format);
    String outputUrlPrefix = "gs://" + appId + "-backups/" + timestamp;

    String backupUrl = "https://datastore.googleapis.com/v1beta1/projects/" + appId + ":export";

    // Make request to Google Cloud data store.
    int response;
    try {
      response = doGetRequest(backupUrl, outputUrlPrefix);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Backup request successfully made.
    logger.info("Backup created for " + appId + ". Response: " + response);

    // Make BigQuery request
    if (Environment.isProduction()) {
      try {
        TaskOptions taskOptions =
            TaskOptions.Builder.withCountdownMillis(TimeUnit.MINUTES.toMillis(10))
                .method(TaskOptions.Method.GET)
                .url("/rest/copyBackupToBigQuery?path=" + timestamp);
        Queue queue = QueueFactory.getDefaultQueue();
        queue.add(taskOptions);
      } catch (Exception e) {
        logger.error("BigQuery import unsuccessful. Exception: " + e.getMessage());
      }
    }

    return Response.ok().build();
  }

  private int doGetRequest(String requestUrl, String outputUrlPrefix) throws Exception {

    URL url = new URL(requestUrl);

    // Generate list of kinds, and use default namespace
    List<String> kinds = new ArrayList<>();
    for (Class type : PresentEntities.TYPES) {
      kinds.add(type.getSimpleName());
    }
    EntityFilter entityFilter = new EntityFilter(kinds, Collections.emptyList());

    // Generate backup request
    BackupRequest backupRequest = new BackupRequest(entityFilter, outputUrlPrefix);

    // Create request json
    Gson gson = new Gson();
    String request = gson.toJson(backupRequest);

    // Get request as bytes
    byte[] postData = request.getBytes(StandardCharsets.UTF_8);

    logger.debug("Request: " + request);

    // Create connection
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    // Add authentication to header
    List<String> scopes = ImmutableList.of("https://www.googleapis.com/auth/datastore",
        "https://www.googleapis.com/auth/taskqueue");
    Environment.authorize(connection, scopes);

    // Set headers
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("charset", "utf-8");
    connection.addRequestProperty("Content-Type", "application/json");
    connection.addRequestProperty("Content-Length", String.valueOf(postData.length));

    // Write request
    OutputStream outputStream = connection.getOutputStream();
    outputStream.write(postData);
    outputStream.flush();

    // Get response from server
    int responseCode = connection.getResponseCode();
    if (responseCode >= 200 && responseCode < 300) {
      return responseCode;
    } else {
      logger.error("Error creating backup. Server Response: "
          + responseCode
          + "\nResponse message: "
          + connection.getResponseMessage()
          + "\nConnection: "
          + connection.toString()
          + "\nRequest: "
          + request);
      try (InputStream s = connection.getErrorStream();
           InputStreamReader r = new InputStreamReader(s, StandardCharsets.UTF_8)) {
        throw new RuntimeException(
            String.format("Got error (%d) response \n%s from %s", responseCode,
                CharStreams.toString(r), connection.toString()));
      }
    }
  }

  private static class BackupRequest {
    private final EntityFilter entityFilter;
    private final String outputUrlPrefix;

    BackupRequest(EntityFilter entityFilter, String outputUrlPrefix) {
      this.entityFilter = entityFilter;
      this.outputUrlPrefix = outputUrlPrefix;
    }
  }

  private static class EntityFilter {
    private final List<String> kinds;
    private final List<String> namespaceIds;

    EntityFilter(List<String> kinds, List<String> namespaceIds) {
      this.kinds = kinds;
      this.namespaceIds = namespaceIds;
    }
  }
}
