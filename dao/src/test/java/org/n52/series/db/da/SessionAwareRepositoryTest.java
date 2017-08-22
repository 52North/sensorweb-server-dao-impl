package org.n52.series.db.da;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.n52.io.response.PlatformType;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.ObservationConstellationEntity;
import org.n52.series.db.beans.ProcedureEntity;

public class SessionAwareRepositoryTest {

    @Test
    public void when_mobileInsituString_then_recognizeType() {
        String platformId = getPlatformId(42L, true, true);
        Assert.assertThat(PlatformType.extractType(platformId), Matchers.is(PlatformType.MOBILE_INSITU));
    }

    @Test
    public void when_mobileRemoteString_then_recognizeType() {
        String platformId = getPlatformId(42L, true, false);
        Assert.assertThat(PlatformType.extractType(platformId), Matchers.is(PlatformType.MOBILE_REMOTE));
    }

    @Test
    public void when_stationaryInsituString_then_recognizeType() {
        String platformId = getPlatformId(42L, false, true);
        Assert.assertThat(PlatformType.extractType(platformId), Matchers.is(PlatformType.STATIONARY_INSITU));
    }

    @Test
    public void when_stationaryRemoteString_then_recognizeType() {
        String platformId = getPlatformId(42L, false, false);
        Assert.assertThat(PlatformType.extractType(platformId), Matchers.is(PlatformType.STATIONARY_REMOTE));
    }

    private String getPlatformId(Long id, boolean mobile, boolean insitu) {
        DatasetEntity dataset = new DatasetEntity();

        ProcedureEntity procedure = new ProcedureEntity();
        procedure.setMobile(mobile);
        procedure.setInsitu(insitu);
        procedure.setPkid(id);

        ObservationConstellationEntity constellation = new ObservationConstellationEntity();
        dataset.setObservationConstellation(constellation);
        constellation.setProcedure(procedure);

        FeatureEntity feature = new FeatureEntity();
        dataset.setFeature(feature);
        feature.setPkid(id);

        SessionAwareRepository repo = new SessionAwareRepository() {};
        return repo.getPlatformId(dataset);
    }

}
