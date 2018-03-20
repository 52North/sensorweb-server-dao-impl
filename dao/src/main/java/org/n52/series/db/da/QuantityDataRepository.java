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

package org.n52.series.db.da;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetMetadata;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataModelUtil;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.data.Data.QuantityData;
import org.n52.series.db.beans.dataset.QuantityDataset;
import org.n52.series.db.beans.ereporting.EReportingQuantityDatasetEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public class QuantityDataRepository extends
        AbstractDataRepository<QuantityDataset, QuantityData, QuantityValue> {

    @Override
    public Class<?> getDatasetEntityType(Session session) {
        return DataModelUtil.isEntitySupported(EReportingQuantityDatasetEntity.class, session)
                ? EReportingQuantityDatasetEntity.class
                : QuantityDatasetEntity.class;
    }

    @Override
    public List<ReferenceValueOutput<QuantityValue>> createReferenceValueOutputs(QuantityDataset datasetEntity,
                                                                                 DbQuery query) {
        List<QuantityDataset> referenceValues = datasetEntity.getReferenceValues();
        List<ReferenceValueOutput<QuantityValue>> outputs = new ArrayList<>();
        for (QuantityDataset referenceSeriesEntity : referenceValues) {
            ReferenceValueOutput<QuantityValue> refenceValueOutput = new ReferenceValueOutput<>();
            ProcedureEntity procedure = referenceSeriesEntity.getProcedure();
            refenceValueOutput.setLabel(procedure.getNameI18n(query.getLocale()));
            refenceValueOutput.setReferenceValueId(createReferenceDatasetId(query, referenceSeriesEntity));

            QuantityDataEntity lastValue = (QuantityDataEntity) referenceSeriesEntity.getLastObservation();
            refenceValueOutput.setLastValue(createSeriesValueFor(lastValue, referenceSeriesEntity, query));
            outputs.add(refenceValueOutput);
        }
        return outputs;
    }

    @Override
    protected Data<QuantityValue> assembleDataWithReferenceValues(QuantityDataset timeseries,
                                                                  DbQuery dbQuery,
                                                                  Session session)
            throws DataAccessException {
        Data<QuantityValue> result = assembleData(timeseries, dbQuery, session);
        List<QuantityDatasetEntity> referenceValues = timeseries.getReferenceValues();
        if (referenceValues != null && !referenceValues.isEmpty()) {
            DatasetMetadata<Data<QuantityValue>> metadata = new DatasetMetadata<>();
            metadata.setReferenceValues(assembleReferenceSeries(referenceValues, dbQuery, session));
            result.setMetadata(metadata);
        }
        return result;
    }

    private Map<String, Data<QuantityValue>> assembleReferenceSeries(List<QuantityDatasetEntity> referenceValues,
                                                                     DbQuery query,
                                                                     Session session)
            throws DataAccessException {
        Map<String, Data<QuantityValue>> referenceSeries = new HashMap<>();
        for (QuantityDatasetEntity referenceSeriesEntity : referenceValues) {
            if (referenceSeriesEntity.isPublished() && referenceSeriesEntity instanceof QuantityDatasetEntity) {
                Data<QuantityValue> referenceSeriesData = assembleData(referenceSeriesEntity, query, session);
                if (haveToExpandReferenceData(referenceSeriesData)) {
                    referenceSeriesData = expandReferenceDataIfNecessary(referenceSeriesEntity,
                                                                         query,
                                                                         session);
                }
                referenceSeries.put(createReferenceDatasetId(query, referenceSeriesEntity), referenceSeriesData);
            }
        }
        return referenceSeries;
    }

    protected String createReferenceDatasetId(DbQuery query, QuantityDataset referenceSeriesEntity) {
        String valueType = referenceSeriesEntity.getValueType();
        DatasetOutput< ? > dataset = DatasetOutput.create(valueType, query.getParameters());
        Long id = referenceSeriesEntity.getId();
        dataset.setId(id.toString());

        String referenceDatasetId = dataset.getId();
        return referenceDatasetId.toString();
    }

    private boolean haveToExpandReferenceData(Data<QuantityValue> referenceSeriesData) {
        return referenceSeriesData.getValues()
                                  .size() <= 1;
    }

    private Data<QuantityValue> expandReferenceDataIfNecessary(QuantityDatasetEntity seriesEntity,
                                                               DbQuery query,
                                                               Session session)
            throws DataAccessException {
        Data<QuantityValue> result = new Data<>();
        DataDao<QuantityData> dao = createDataDao(session);
        List<QuantityData> observations = dao.getAllInstancesFor(seriesEntity, query);
        if (!hasValidEntriesWithinRequestedTimespan(observations)) {
            QuantityValue lastValue = getLastValue(seriesEntity, session, query);
            result.addValues(expandToInterval(lastValue.getValue(), seriesEntity, query));
        }

        if (hasSingleValidReferenceValue(observations)) {
            QuantityData entity = observations.get(0);
            result.addValues(expandToInterval(entity.getValue(), seriesEntity, query));
        }
        return result;
    }

    @Override
    protected Data<QuantityValue> assembleData(QuantityDataset seriesEntity, DbQuery query, Session session)
            throws DataAccessException {
        Data<QuantityValue> result = new Data<>();
        DataDao<QuantityData > dao = createDataDao(session);
        List<QuantityData> observations = dao.getAllInstancesFor(seriesEntity, query);
        for (QuantityData observation : observations) {
            if (observation != null) {
                result.addValues(createSeriesValueFor(observation, seriesEntity, query));
            }
        }
        return result;
    }

    private QuantityValue[] expandToInterval(BigDecimal value, QuantityDataset series, DbQuery query) {
        QuantityData referenceStart = new QuantityDataEntity();
        QuantityData referenceEnd = new QuantityDataEntity();
        referenceStart.setSamplingTimeEnd(query.getTimespan()
                                               .getStart()
                                               .toDate());
        referenceEnd.setSamplingTimeEnd(query.getTimespan()
                                             .getEnd()
                                             .toDate());
        referenceStart.setValue(value);
        referenceEnd.setValue(value);
        return new QuantityValue[] {
            createSeriesValueFor(referenceStart, series, query),
            createSeriesValueFor(referenceEnd, series, query),
        };
    }

    @Override
    protected QuantityValue createEmptyValue() {
        return new QuantityValue();
    }

    @Override
    public QuantityValue createSeriesValueFor(QuantityData observation,
                                              QuantityDataset dataset,
                                              DbQuery query) {
        QuantityValue value = createValue(observation, dataset, query);
        return addMetadatasIfNeeded(observation, value, dataset, query);
    }

    private QuantityValue createValue(QuantityData observation, QuantityDataset dataset, DbQuery query) {
        ServiceEntity service = getServiceEntity(dataset);
        BigDecimal observationValue = !service.isNoDataValue(observation)
                ? format(observation, dataset)
                : null;
        return createValue(observationValue, observation, query);
    }

    QuantityValue createValue(BigDecimal observationValue, QuantityData observation, DbQuery query) {
        QuantityValue value = prepareValue(observation, query);
        value.setValue(observationValue);
        return value;
    }

    private BigDecimal format(QuantityData observation, QuantityDataset series) {
        if (observation.getValue() == null) {
            return observation.getValue();
        }
        Integer scale = series.getNumberOfDecimals();
        return scale != null
                ? observation.getValue().setScale(scale, RoundingMode.HALF_UP)
                : observation.getValue();
    }

}
