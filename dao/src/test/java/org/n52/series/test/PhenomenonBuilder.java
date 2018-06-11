package org.n52.series.test;

import org.n52.series.db.beans.PhenomenonEntity;

public class PhenomenonBuilder extends DescribableEntityBuilder<PhenomenonEntity> {

    private PhenomenonBuilder(String identifier) {
        super(identifier);
    }

    public static PhenomenonBuilder newPhenomenon(String identifier) {
        return new PhenomenonBuilder(identifier);
    }

    @Override
    public PhenomenonEntity build() {
        PhenomenonEntity entity = prepare(new PhenomenonEntity());
        return entity;
    }

}
