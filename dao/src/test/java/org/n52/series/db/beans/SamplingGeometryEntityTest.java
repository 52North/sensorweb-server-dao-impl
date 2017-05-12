package org.n52.series.db.beans;

import static org.hamcrest.CoreMatchers.is;

import java.sql.Timestamp;

import org.hamcrest.MatcherAssert;
import org.joda.time.DateTime;
import org.junit.Test;

public class SamplingGeometryEntityTest {

    @Test
    public void when_settingTimestampWithNanos_then_nanosAvailableOnGetting() {
        SamplingGeometryEntity samplingGeometryEntity = new SamplingGeometryEntity();
        Timestamp expected = createTimestamp("2015-07-17T21:14:35.022+02", 321);

        samplingGeometryEntity.setTimestamp(expected);
        Timestamp actual = (Timestamp) samplingGeometryEntity.getTimestamp();
        MatcherAssert.assertThat(actual.getNanos(), is(expected.getNanos()));
    }

    private Timestamp createTimestamp(String date, int nanos) {
        DateTime timeValue = DateTime.parse(date);
        Timestamp timestamp = new Timestamp(timeValue.getMillis());
        timestamp.setNanos(nanos);
        return timestamp;
    }
}
