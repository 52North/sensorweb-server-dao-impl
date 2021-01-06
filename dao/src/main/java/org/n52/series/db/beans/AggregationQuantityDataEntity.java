package org.n52.series.db.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregationQuantityDataEntity extends AbstractQuantityDataEntity {

    public static final String PROPERTY_AGGREGATION = "aggregationType";

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregationQuantityDataEntity.class);

    private AggregationTypeEntity aggregationType;

    public AggregationTypeEntity getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(AggregationTypeEntity aggregationType) {
        this.aggregationType = aggregationType;
    }

}
