
package org.n52.series.springdata.assembler;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.springdata.DataRepository;

public class QuantityDataAssembler extends DataAssembler<QuantityDatasetEntity, QuantityDataEntity, QuantityValue> {

    public QuantityDataAssembler(final DataRepository<QuantityDataEntity> dataRepository) {
        super(dataRepository);
    }

    @Override
    protected QuantityValue createEmptyValue() {
        return new QuantityValue();
    }

    @Override
    public QuantityValue assembleDataValue(final QuantityDataEntity observation,
                                         final QuantityDatasetEntity dataset,
                                         final DbQuery query) {
        final BigDecimal observationValue = getDataValue(observation, dataset);
        final QuantityValue value = assembleDataValue(observationValue, observation, query);
        return addMetadatasIfNeeded(observation, value, dataset, query);
    }

    private BigDecimal getDataValue(final QuantityDataEntity observation,
                                    final QuantityDatasetEntity dataset) {
        final ServiceEntity service = getServiceEntity(dataset);
        return !service.isNoDataValue(observation)
            ? format(observation, dataset)
            : null;
    }

    QuantityValue assembleDataValue(final BigDecimal observationValue,
                              final QuantityDataEntity observation,
                              final DbQuery query) {
        final QuantityValue value = prepareValue(observation, query);
        value.setValue(observationValue);
        return value;
    }

    private BigDecimal format(final QuantityDataEntity observation, final QuantityDatasetEntity series) {
        if (observation.getValue() == null) {
            return observation.getValue();
        }
        final Integer scale = series.getNumberOfDecimals();
        return (scale != null) && (scale.intValue() >= 0)
            ? observation.getValue().setScale(scale, RoundingMode.HALF_UP)
            : observation.getValue();
    }
}
