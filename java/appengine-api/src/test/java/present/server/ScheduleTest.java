package present.server;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.group.Schedule;
import present.wire.rpc.core.ClientException;

import static org.junit.Assert.*;

/**
 * Tests event schedules for groups.
 *
 * @author Gabrielle Taylor (gabrielle@present.co)
 */
public class ScheduleTest {

  private static final Logger logger = LoggerFactory.getLogger(ScheduleTest.class);

  @Test public void testSchedules() throws IOException, InterruptedException {
    Long startTime = System.currentTimeMillis();
    Long endTime = startTime + TimeUnit.HOURS.toMillis(5);
    Long badEndTime = startTime - TimeUnit.HOURS.toMillis(4);

    // Test that schedules are correctly converted to and from protos.
    present.proto.Schedule protoSchedule1 = new present.proto.Schedule(startTime, endTime);
    Schedule schedule1 = new Schedule(protoSchedule1);
    assertTrue(protoSchedule1.equals(schedule1.toProto()));

    // Test that schedules with no end time are accepted.
    present.proto.Schedule protoSchedule2 = new present.proto.Schedule(startTime, null);
    Schedule schedule2 = new Schedule(protoSchedule2);
    assertTrue(protoSchedule2.equals(schedule2.toProto()));
    assertTrue(schedule2.endTime == schedule2.startTime);

    // Test that schedules with end times before start times are not permitted.
    present.proto.Schedule protoSchedule3 = new present.proto.Schedule(startTime, badEndTime);
    try {
      Schedule schedule3 = new Schedule(protoSchedule3);
      Assert.fail();
    } catch (ClientException|IllegalArgumentException e) {
      assertEquals(e.getMessage(), "End time can not be before start time.");
    }



  }
}
