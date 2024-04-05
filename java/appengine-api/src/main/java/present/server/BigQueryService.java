package present.server;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobConfiguration;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.LoadJobConfiguration;
import com.google.cloud.bigquery.TableId;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.environment.Environment;
import present.server.model.PresentEntities;

/**
 * Dispatches import requests to Google BigQuery.
 *
 * @author Gabrielle Taylor (gabrielle@present.co)
 */

@Path("copyBackupToBigQuery") public class BigQueryService {
  private static final Logger logger = LoggerFactory.getLogger(BigQueryService.class);

  @GET public Response bigQueryImport(@QueryParam("path") String path) {

    // Get current platform for correct backup storage bucket.
    String appId = Environment.applicationId();
    logger.info("Importing to BigQuery for " + appId);

    // Create paths for bigquery import
    // Assumes default namespace was used in export
    String bucketName = appId + "-backups/" + path;
    String kindFolderLocation = "gs://" + bucketName + "/default_namespace/kind_";
    String kindBackupFile = "/default_namespace_kind_";
    String backupExt = ".export_metadata";

    // Instantiate a client, authenticates with google
    BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

    // Create import request for each kind
    for (Class type : PresentEntities.TYPES) {
      // Create job configuration
      JobConfiguration jobConfig =
          LoadJobConfiguration.newBuilder(TableId.of(appId, "present", type.getSimpleName()),
              kindFolderLocation
                  + type.getSimpleName()
                  + kindBackupFile
                  + type.getSimpleName()
                  + backupExt)
              .setWriteDisposition(JobInfo.WriteDisposition.WRITE_TRUNCATE)
              .setFormatOptions(FormatOptions.datastoreBackup())
              .build();

      // Create job
      Job job = bigquery.create(JobInfo.of(jobConfig),
          BigQuery.JobOption.fields(BigQuery.JobField.ID, BigQuery.JobField.STATUS));

      logger.debug("Bigquery import started for type "
          + type.getSimpleName()
          + "\nID: "
          + job.getGeneratedId());
    }
    // Backup request successfully made.
    logger.info("Backup for " + path + " imported to BigQuery.");
    return Response.ok().build();
  }
}
