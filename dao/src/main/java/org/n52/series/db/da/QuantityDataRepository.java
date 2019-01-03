/*
 * Copyright (C) 2015-2019 52°North Initiative for Geospatial Open Source
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
package org.n52.series.db.da;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.Session;

import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetMetadata;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataRepositoryComponent;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

@DataRepositoryComponent(value = "quantity", datasetEntityType = QuantityDatasetEntity.class)
public class QuantityDataRepository extends
        AbstractDataRepository<QuantityDatasetEntity, QuantityDataEntity, QuantityValue, BigDecimal> {

    @Override
    public List<ReferenceValueOutput<QuantityValue>> getReferenceValues(QuantityDatasetEntity datasetEntity,
                                                                        DbQuery query) {
        return datasetEntity.getReferenceValues().stream().map(referenceSeriesEntity -> {
             ReferenceValueOutput<QuantityValue> refenceValueOutput = new ReferenceValueOutput<>();
            ProcedureEntity procedure = referenceSeriesEntity.getProcedure();
            refenceValueOutput.setLabel(procedure.getNameI18n(query.getLocale()));
            refenceValueOutput.setReferenceValueId(createReferenceDatasetId(query, referenceSeriesEntity));

            QuantityDataEntity lastValue = (QuantityDataEntity) referenceSeriesEntity.getLastValue();
            refenceValueOutput.setLastValue(assembleDataValue(lastValue, referenceSeriesEntity, query));
            return refenceValueOutput;
        }).collect(toList());

    }

    @Override
    protected Data<QuantityValue> assembleExpandedData(QuantityDatasetEntity dataset,
                                                       DbQuery query,
                                                       Session session)
            throws DataAccessException {
        Data<QuantityValue> result = assembleData(dataset, query, session);
        DatasetMetadata<QuantityValue> metadata = result.getMetadata();

        if (metadata == null) {
            result.setMetadata(metadata = new DatasetMetadata<>());
        }

        QuantityDataEntity previousValue = getClosestValueBeforeStart(dataset, query);
        QuantityDataEntity nextValue = getClosestValueAfterEnd(dataset, query);

        metadata.setValueBeforeTimespan(createValue(previousValue, dataset, query));
        metadata.setValueAfterTimespan(createValue(nextValue, dataset, query));

        List<QuantityDatasetEntity> referenceValues = dataset.getReferenceValues();
        if ((referenceValues != null) && !referenceValues.isEmpty()) {
            metadata.setReferenceValues(assembleReferenceSeries(referenceValues, query, session));
        }
        return result;
    }

    private Map<String, Data<QuantityValue>> assembleReferenceSeries(List<QuantityDatasetEntity> referenceValues,
                                                                     DbQuery query,
                                                                     Session session)
            throws DataAccessException {
        return referenceValues.stream().filter(QuantityDatasetEntity::isPublished)
                .collect(toMap(dataset -> createReferenceDatasetId(query, dataset),
                               dataset -> {
                                   Data<QuantityValue> data = assembleData(dataset, query, session);
                                   return haveToExpandReferenceData(data)
                                                  ? expandReferenceDataIfNecessary(dataset, query, session)
                                                  : data;
                               }));
    }

    protected String createReferenceDatasetId(DbQuery query, QuantityDatasetEntity referenceSeriesEntity) {
        String valueType = referenceSeriesEntity.getValueType();
        DatasetOutput<?> dataset = DatasetOutput.create(valueType, query.getParameters());
        Long id = referenceSeriesEntity.getPkid();
        dataset.setId(id.toString());
        return dataset.getId();
    }

    private boolean haveToExpandReferenceData(Data<QuantityValue> referenceSeriesData) {
        return referenceSeriesData.getValues().size() <= 1;
    }

    private Data<QuantityValue> expandReferenceDataIfNecessary(QuantityDatasetEntity seriesEntity,
                                                               DbQuery query,
                                                               Session session)
            throws DataAccessException {
        Data<QuantityValue> result = new Data<>();
        DataDao<QuantityDataEntity> dao = createDataDao(session);
        List<QuantityDataEntity> observations = dao.getAllInstancesFor(seriesEntity, query);
        if (!hasValidEntriesWithinRequestedTimespan(observations)) {
            QuantityValue lastValue = getLastValue(seriesEntity, session, query);
            result.addValues(expandToInterval(lastValue.getValue(), seriesEntity, query));
        }

        if (hasSingleValidReferenceValue(observations)) {
            QuantityDataEntity entity = observations.get(0);
            result.addValues(expandToInterval(entity.getValue(), seriesEntity, query));
        }
        return result;
    }

    @Override
    protected Data<QuantityValue> assembleData(QuantityDatasetEntity seriesEntity, DbQuery query, Session session) {
        Data<QuantityValue> result = new Data<>(new DatasetMetadata<>());
        createDataDao(session)
                .getAllInstancesFor(seriesEntity, query).stream()
                .filter(Objects::nonNull)
                .map(observation -> assembleDataValue(observation, seriesEntity, query))
                .forEachOrdered(result::addNewValue);
        return result;
    }

    private QuantityValue[] expandToInterval(BigDecimal value, QuantityDatasetEntity series, DbQuery query) {
        QuantityDataEntity referenceStart = new QuantityDataEntity();
        Date startDate = query.getTimespan().getStart().toDate();
        referenceStart.setTimestart(startDate);
        referenceStart.setTimeend(startDate);
        referenceStart.setValue(value);

        Date endDate = query.getTimespan().getEnd().toDate();
        QuantityDataEntity referenceEnd = new QuantityDataEntity();
        referenceEnd.setTimestart(endDate);
        referenceEnd.setTimeend(endDate);
        referenceEnd.setValue(value);

        return new QuantityValue[] {
            assembleDataValue(referenceStart, series, query),
            assembleDataValue(referenceEnd, series, query)
        };
    }

    @Override
    public QuantityValue assembleDataValue(QuantityDataEntity observation,
                                           QuantityDatasetEntity dataset,
                                           DbQuery query) {
        QuantityValue value = createValue(observation, dataset, query);
        return addMetadatasIfNeeded(observation, value, dataset, query);
    }

    private QuantityValue createValue(QuantityDataEntity observation, QuantityDatasetEntity dataset, DbQuery query) {
        return getServiceEntity(dataset).isNoDataValue(observation) ? null
                       : createValue(format(observation, dataset), observation, query);
    }

    QuantityValue createValue(BigDecimal observationValue, QuantityDataEntity observation, DbQuery query) {
        Date timeend = observation.getTimeend();
        Date timestart = observation.getTimestart();
        long end = timeend.getTime();
        long start = timestart.getTime();
        return query.getParameters().isShowTimeIntervals()
            ? new QuantityValue(start, end, observationValue)
            : new QuantityValue(end, observationValue);
    }

    private BigDecimal format(QuantityDataEntity observation, QuantityDatasetEntity series) {
        return Optional.ofNullable(observation).map(QuantityDataEntity::getValue)
                .map(v -> v.setScale(series.getNumberOfDecimals(), RoundingMode.HALF_UP))
                .orElse(null);
    }

}
