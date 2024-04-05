package present.server.log;

import com.google.api.gax.paging.Page;
import com.google.appengine.repackaged.com.google.common.collect.Iterables;
import com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException;
import com.google.apphosting.api.logservice.LogServicePb;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utilities for searching and parsing our request logs.
 *
 * @author Bob Lee (bob@present.co)
 */
public class RequestLogs {

  private static DateFormat RFC3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

  /** Formats UTC time using RFC3339. */
  public static String toRfc3339(long timestamp) {
    return RFC3339.format(new Date(timestamp));
  }

  /**
   * Searches our logs.
   *
   * @param filter See https://cloud.google.com/logging/docs/view/advanced-filters
   */
  public static Iterable<LogServicePb.RequestLog> search(String filter) {
    LoggingOptions options = LoggingOptions.getDefaultInstance();
    Logging logging = options.getService();
    Page<LogEntry> entries = logging.listLogEntries(Logging.EntryListOption.filter(filter));
    Iterable<LogEntry> values = entries.iterateAll();
    return Iterables.transform(values, entry -> {
      // Note: We have to switch to byte[] instead of using unpack() because RequestLog depends
      // on a repackaged protocol buffer API. 🤷 As an alternative, we could compile the proto
      // ourselves: https://github.com/googleapis/googleapis/blob/master/google/appengine/logging/v1/request_log.proto‍
      byte[] bytes = entry.<Payload.ProtoPayload>getPayload().getData().getValue().toByteArray();
      try {
        LogServicePb.RequestLog requestLog = LogServicePb.RequestLog.parser().parsePartialFrom(bytes);
        // Timestamp doesn't appear to be set on the request logs.
        requestLog.setStartTime(entry.getTimestamp());
        return requestLog;
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException(e);
      }
    });
  }
}
