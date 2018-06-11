
package org.n52.series.test;

import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.FormatEntity;

public class FeatureBuilder extends DescribableEntityBuilder<FeatureEntity> {

    private FormatEntity format;

    private FeatureBuilder(String identifier) {
        super(identifier);
    }

    public static FeatureBuilder newFeature(String identifier) {
        return new FeatureBuilder(identifier);
    }

    public FeatureBuilder setFormat(FormatEntity format) {
        this.format = format;
        return this;
    }

    @Override
    public FeatureEntity build() {
        FeatureEntity entity = prepare(new FeatureEntity());
        entity.setFeatureType(format);
        return entity;
    }

}
