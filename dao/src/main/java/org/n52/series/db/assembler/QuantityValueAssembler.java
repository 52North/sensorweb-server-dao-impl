/*
 * Copyright (C) 2015-2018 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package org.n52.series.db.assembler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetMetadata;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.series.db.DataRepository;
import org.n52.series.db.DatasetRepository;
import org.n52.series.db.ValueAssemblerComponent;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.old.dao.DbQuery;

@ValueAssemblerComponent(value = org.n52.series.db.beans.data.Data.QuantityData.VALUE_TYPE,
                         datasetEntityType = QuantityDatasetEntity.class)
public class QuantityValueAssembler extends
        AbstractValueAssembler<QuantityDatasetEntity, QuantityDataEntity, QuantityValue, BigDecimal> {

    public QuantityValueAssembler(DataRepository<QuantityDataEntity> dataRepository,
                                  DatasetRepository<QuantityDatasetEntity> datasetRepository) {
        super(dataRepository, datasetRepository);
    }

    @Override
    public QuantityValue assembleDataValue(QuantityDataEntity data,
                                           QuantityDatasetEntity dataset,
                                           DbQuery query) {
        final QuantityValue assembledValue = prepareValue(new QuantityValue(), data, query);
        assembledValue.setValue(getDataValue(data, dataset));
        return assembledValue;
    }

    private BigDecimal getDataValue(QuantityDataEntity observation,
                                    QuantityDatasetEntity dataset) {
        return !isNoDataValue(observation, dataset)
            ? format(observation, dataset)
            : null;
    }

    private BigDecimal format(QuantityDataEntity data, QuantityDatasetEntity dataset) {
        if (data.getValue() == null) {
            return data.getValue();
        }
        final Integer scale = dataset.getNumberOfDecimals();
        return (scale != null) && (scale.intValue() >= 0)
            ? data.getValue().setScale(scale, RoundingMode.HALF_UP)
            : data.getValue();
    }

    @Override
    protected Data<QuantityValue> assembleExpandedDataValues(QuantityDatasetEntity dataset, DbQuery query) {
        Data<QuantityValue> result = assembleDataValues(dataset, query);
        DatasetMetadata<Data<QuantityValue>> metadata = result.getMetadata();

        QuantityDataEntity previousValue = getClosestValueAfterEnd(dataset, query);
        QuantityDataEntity nextValue = getClosestValueAfterEnd(dataset, query);
        metadata.setValueBeforeTimespan(assembleDataValue(previousValue, dataset, query));
        metadata.setValueAfterTimespan(assembleDataValue(nextValue, dataset, query));

        List<QuantityDatasetEntity> referenceValues = dataset.getReferenceValues();
        if ((referenceValues != null) && !referenceValues.isEmpty()) {
            metadata.setReferenceValues(assembleReferenceSeries(referenceValues, query));
        }
        return result;
    }

    private Map<String, Data<QuantityValue>> assembleReferenceSeries(List<QuantityDatasetEntity> referenceValues,
                                                                     DbQuery query) {
        Map<String, Data<QuantityValue>> referenceSeries = new HashMap<>();
        for (QuantityDatasetEntity referenceSeriesEntity : referenceValues) {
            if (referenceSeriesEntity.isPublished() && (referenceSeriesEntity instanceof QuantityDatasetEntity)) {
                Data<QuantityValue> referenceSeriesData = assembleDataValues(referenceSeriesEntity, query);
                if (haveToExpandReferenceData(referenceSeriesData)) {
                    referenceSeriesData = expandReferenceDataIfNecessary(referenceSeriesEntity, query);
                }
                referenceSeries.put(createReferenceDatasetId(query, referenceSeriesEntity), referenceSeriesData);
            }
        }
        return referenceSeries;
    }

    private boolean haveToExpandReferenceData(Data<QuantityValue> referenceSeriesData) {
        List<QuantityValue> values = referenceSeriesData.getValues();
        return values.size() <= 1;
    }

    private Data<QuantityValue> expandReferenceDataIfNecessary(QuantityDatasetEntity dataset,
                                                               DbQuery query) {
        Data<QuantityValue> result = new Data<>();
        List<QuantityDataEntity> observations = findAll(dataset, query).collect(Collectors.toList());
        if (!hasValidEntriesWithinRequestedTimespan(observations)) {
            QuantityValue lastValue = getLastValue(dataset, query);
            result.addValues(expandToInterval(lastValue.getValue(), dataset, query));
        }

        if (hasSingleValidReferenceValue(observations)) {
            QuantityDataEntity entity = observations.get(0);
            result.addValues(expandToInterval(entity.getValue(), dataset, query));
        }
        return result;
    }

    private QuantityValue[] expandToInterval(BigDecimal value, QuantityDatasetEntity series, DbQuery query) {
        QuantityDataEntity referenceStart = new QuantityDataEntity();
        QuantityDataEntity referenceEnd = new QuantityDataEntity();
        referenceStart.setSamplingTimeEnd(query.getTimespan()
                                               .getStart()
                                               .toDate());
        referenceEnd.setSamplingTimeEnd(query.getTimespan()
                                             .getEnd()
                                             .toDate());
        referenceStart.setValue(value);
        referenceEnd.setValue(value);
        return new QuantityValue[] {
                                    assembleDataValue(referenceStart, series, query),
                                    assembleDataValue(referenceEnd, series, query),
        };
    }

    @Override
    public List<ReferenceValueOutput<QuantityValue>> getReferenceValues(QuantityDatasetEntity datasetEntity,
                                                                        DbQuery query) {
        List<QuantityDatasetEntity> referenceValues = datasetEntity.getReferenceValues();
        List<ReferenceValueOutput<QuantityValue>> outputs = new ArrayList<>();
        for (QuantityDatasetEntity referenceSeriesEntity : referenceValues) {
            ReferenceValueOutput<QuantityValue> refenceValueOutput = new ReferenceValueOutput<>();
            ProcedureEntity procedure = referenceSeriesEntity.getProcedure();
            refenceValueOutput.setLabel(procedure.getNameI18n(query.getLocale()));
            refenceValueOutput.setReferenceValueId(createReferenceDatasetId(query, referenceSeriesEntity));

            QuantityDataEntity lastValue = (QuantityDataEntity) referenceSeriesEntity.getLastObservation();
            refenceValueOutput.setLastValue(assembleDataValue(lastValue, referenceSeriesEntity, query));
            outputs.add(refenceValueOutput);
        }
        return outputs;
    }

    private String createReferenceDatasetId(DbQuery query, QuantityDatasetEntity referenceSeriesEntity) {
        String valueType = referenceSeriesEntity.getValueType();
        DatasetOutput< ? > dataset = DatasetOutput.create(valueType, query.getParameters());
        Long id = referenceSeriesEntity.getId();
        dataset.setId(id.toString());

        String referenceDatasetId = dataset.getId();
        return referenceDatasetId.toString();
    }
}
