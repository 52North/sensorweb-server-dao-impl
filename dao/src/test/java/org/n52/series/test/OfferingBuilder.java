package org.n52.series.test;

import org.n52.series.db.beans.OfferingEntity;

public class OfferingBuilder extends DescribableEntityBuilder<OfferingEntity> {

    private OfferingBuilder(String identifier) {
        super(identifier);
    }

    public static OfferingBuilder newOffering(String identifier) {
        return new OfferingBuilder(identifier);
    }

    @Override
    public OfferingEntity build() {
        OfferingEntity entity = prepare(new OfferingEntity());
        return entity;
    }

}
