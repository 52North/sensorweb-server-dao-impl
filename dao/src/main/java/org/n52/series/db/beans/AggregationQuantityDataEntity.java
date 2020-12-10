package org.n52.series.db.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregationQuantityDataEntity extends QuantityDataEntity {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregationQuantityDataEntity.class);
    
    private AggregationType aggregationType;

    public AggregationType getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(AggregationType aggregationType) {
        this.aggregationType = aggregationType;
    }

}
