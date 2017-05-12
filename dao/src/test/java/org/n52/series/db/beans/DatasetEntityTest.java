package org.n52.series.db.beans;

import static org.hamcrest.CoreMatchers.is;

import java.sql.Timestamp;

import org.hamcrest.MatcherAssert;
import org.joda.time.DateTime;
import org.junit.Test;

public class DatasetEntityTest {

    @Test
    public void when_settingFirstValueAtWithNanos_then_nanosAvailableOnGetting() {
        DatasetEntity<DataEntity<?>> datasetEntity = new DatasetEntity<>();
        Timestamp timestamp = createTimestamp("2015-07-17T21:14:35.022+02", 321);

        datasetEntity.setFirstValueAt(timestamp);
        Timestamp firstValueAt = (Timestamp) datasetEntity.getFirstValueAt();
        MatcherAssert.assertThat(firstValueAt.getNanos(), is(timestamp.getNanos()));
    }

    @Test
    public void when_settingLastValueAtWithNanos_then_nanosAvailableOnGetting() {
        DatasetEntity<DataEntity<?>> datasetEntity = new DatasetEntity<>();
        Timestamp timestamp = createTimestamp("2015-07-17T21:14:35.022+02", 321);

        datasetEntity.setLastValueAt(timestamp);
        Timestamp lastValueAt = (Timestamp) datasetEntity.getLastValueAt();
        MatcherAssert.assertThat(lastValueAt.getNanos(), is(timestamp.getNanos()));
    }

    private Timestamp createTimestamp(String date, int nanos) {
        DateTime timeValue = DateTime.parse(date);
        Timestamp timestamp = new Timestamp(timeValue.getMillis());
        timestamp.setNanos(nanos);
        return timestamp;
    }
}
