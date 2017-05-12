package org.n52.series.db.beans;

import static org.hamcrest.CoreMatchers.is;

import java.sql.Timestamp;

import org.hamcrest.MatcherAssert;
import org.joda.time.DateTime;
import org.junit.Test;

public class OfferingEntityTest {

    @Test
    public void when_settingPhenomenonTimeStartWithNanos_then_nanosAvailableOnGetting() {
        OfferingEntity entity = new OfferingEntity();
        Timestamp expected = createTimestamp("2015-07-17T21:14:35.022+02", 321);

        entity.setPhenomenonTimeStart(expected);
        Timestamp actual = (Timestamp) entity.getPhenomenonTimeStart();
        MatcherAssert.assertThat(actual.getNanos(), is(expected.getNanos()));
    }

    @Test
    public void when_settingPhenomenonTimeEndWithNanos_then_nanosAvailableOnGetting() {
        OfferingEntity entity = new OfferingEntity();
        Timestamp expected = createTimestamp("2015-07-17T21:14:35.022+02", 321);

        entity.setPhenomenonTimeEnd(expected);
        Timestamp actual = (Timestamp) entity.getPhenomenonTimeEnd();
        MatcherAssert.assertThat(actual.getNanos(), is(expected.getNanos()));
    }

    @Test
    public void when_settingResultTimeStartWithNanos_then_nanosAvailableOnGetting() {
        OfferingEntity entity = new OfferingEntity();
        Timestamp expected = createTimestamp("2015-07-17T21:14:35.022+02", 321);

        entity.setResultTimeStart(expected);
        Timestamp actual = (Timestamp) entity.getResultTimeStart();
        MatcherAssert.assertThat(actual.getNanos(), is(expected.getNanos()));
    }

    @Test
    public void when_settingResultTimeEndWithNanos_then_nanosAvailableOnGetting() {
        OfferingEntity entity = new OfferingEntity();
        Timestamp expected = createTimestamp("2015-07-17T21:14:35.022+02", 321);

        entity.setResultTimeEnd(expected);
        Timestamp actual = (Timestamp) entity.getResultTimeEnd();
        MatcherAssert.assertThat(actual.getNanos(), is(expected.getNanos()));
    }

    private Timestamp createTimestamp(String date, int nanos) {
        DateTime timeValue = DateTime.parse(date);
        Timestamp timestamp = new Timestamp(timeValue.getMillis());
        timestamp.setNanos(nanos);
        return timestamp;
    }
}
