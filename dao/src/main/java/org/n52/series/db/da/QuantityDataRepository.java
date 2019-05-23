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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.hibernate.Session;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetMetadata;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.janmayen.i18n.LocaleHelper;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataRepositoryComponent;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.dataset.ValueType;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

@DataRepositoryComponent(value = "quantity", datasetEntityType = DatasetEntity.class)
public class QuantityDataRepository
        extends
        AbstractDataRepository<DatasetEntity, QuantityDataEntity, QuantityValue, BigDecimal> {

    @Override
    protected QuantityValue createEmptyValue() {
        return new QuantityValue();
    }

    @Override
    public List<ReferenceValueOutput<QuantityValue>> getReferenceValues(DatasetEntity dataset,
                                                                        DbQuery query,
                                                                        Session session) {
        List<DatasetEntity> referenceValues = dataset.getReferenceValues()
                                                     .stream()
                                                     .filter(Objects::nonNull)
                                                     .filter(rv -> rv.getValueType() == ValueType.quantity)
                                                     .collect(toList());

        List<ReferenceValueOutput<QuantityValue>> outputs = new ArrayList<>();
        for (DatasetEntity referenceDatasetEntity : referenceValues) {
            ReferenceValueOutput<QuantityValue> refenceValueOutput = new ReferenceValueOutput<>();
            String datasetId = createReferenceDatasetId(query, referenceDatasetEntity);
            refenceValueOutput.setReferenceValueId(datasetId);

            ProcedureEntity procedure = referenceDatasetEntity.getProcedure();
            String label = procedure.getNameI18n(query.getLocale());
            refenceValueOutput.setLabel(label);

            QuantityValue lastValue = getLastValue(referenceDatasetEntity, session, query);
            refenceValueOutput.setLastValue(lastValue);

            outputs.add(refenceValueOutput);
        }
        return outputs;
    }

    @Override
    protected Data<QuantityValue> assembleExpandedData(Long datasetId, DbQuery query, Session session)
            throws DataAccessException {
        Data<QuantityValue> result = assembleData(datasetId, query, session);
        DatasetMetadata<QuantityValue> metadata = result.getMetadata();

        if (metadata == null) {
            result.setMetadata(metadata = new DatasetMetadata<>());
        }
        DatasetEntity dataset = session.get(DatasetEntity.class, datasetId);
        List<DatasetEntity> referenceValues = dataset.getReferenceValues();
        if ((referenceValues != null) && !referenceValues.isEmpty()) {
            metadata.setReferenceValues(assembleReferenceSeries(referenceValues, query, session));
        }
        if (query.expandWithNextValuesBeyondInterval()) {
            QuantityDataEntity previousValue = unproxy(getClosestValueBeforeStart(dataset, query, session), session);
            QuantityDataEntity nextValue = unproxy(getClosestValueAfterEnd(dataset, query, session), session);

            if (previousValue != null) {
                metadata.setValueBeforeTimespan(createValue(previousValue, dataset, query));
            }
            if (nextValue != null) {
                metadata.setValueAfterTimespan(createValue(nextValue, dataset, query));
            }
        }
        return result;
    }

    private Map<String, Data<QuantityValue>> assembleReferenceSeries(List<DatasetEntity> referenceValues,
                                                                     DbQuery query,
                                                                     Session session)
            throws DataAccessException {
        Map<String, Data<QuantityValue>> referencedDatasets = new HashMap<>();
        for (DatasetEntity referenceDatasetEntity : referenceValues) {
            if (referenceDatasetEntity != null
                    && referenceDatasetEntity.isPublished()
                    && referenceDatasetEntity.getValueType()
                                             .equals(ValueType.quantity)) {
                Data<QuantityValue> referencedDatasetData = assembleData(referenceDatasetEntity, query, session);
                if (haveToExpandReferenceData(referencedDatasetData)) {
                    referencedDatasetData = expandReferenceDataIfNecessary(referenceDatasetEntity, query, session);
                }
                referencedDatasets.put(createReferenceDatasetId(query, referenceDatasetEntity), referencedDatasetData);
            }
        }
        return referencedDatasets;
    }

    protected String createReferenceDatasetId(DbQuery query, DatasetEntity referenceDataset) {
        DatasetOutput< ? > dataset = DatasetOutput.create(query.getParameters());
        Long id = referenceDataset.getId();
        dataset.setId(id.toString());
        return dataset.getId();
    }

    private boolean haveToExpandReferenceData(Data<QuantityValue> referencedDatasetData) {
        return referencedDatasetData.getValues()
                                    .size() <= 1;
    }

    private Data<QuantityValue> expandReferenceDataIfNecessary(DatasetEntity dataset,
                                                               DbQuery query,
                                                               Session session)
            throws DataAccessException {
        Data<QuantityValue> result = new Data<>();
        DataDao<QuantityDataEntity> dao = createDataDao(session);
        List<QuantityDataEntity> observations = dao.getAllInstancesFor(dataset.getId(), query);
        if (!hasValidEntriesWithinRequestedTimespan(observations)) {
            QuantityValue lastValue = getLastValue(dataset, session, query);
            result.addValues(expandToInterval(lastValue.getValue(), dataset, query));
        }

        if (hasSingleValidReferenceValue(observations)) {
            QuantityDataEntity entity = observations.get(0);
            result.addValues(expandToInterval(entity.getValue(), dataset, query));
        }
        return result;
    }

    @Override
    protected Data<QuantityValue> assembleData(DatasetEntity dataset, DbQuery query, Session session) {
        return assembleData(dataset.getId(), query, session);
    }

    @Override
    protected Data<QuantityValue> assembleData(Long dataset, DbQuery query, Session session) {
        Data<QuantityValue> result = new Data<>();
        // TODO: How to handle observations with detection limit? Currentl, null is returned a filtered
        createDataDao(session).getAllInstancesFor(dataset, query)
                              .stream()
                              .filter(Objects::nonNull)
                              .map(observation -> assembleDataValue(observation, observation.getDataset(), query))
                              .filter(Objects::nonNull)
                              .forEachOrdered(result::addNewValue);
        return result;
    }

    private QuantityValue[] expandToInterval(BigDecimal value, DatasetEntity dataset, DbQuery query) {
        QuantityDataEntity referenceStart = new QuantityDataEntity();
        referenceStart.setDataset(dataset);
        Date startDate = query.getTimespan()
                              .getStart()
                              .toDate();
        referenceStart.setSamplingTimeStart(startDate);
        referenceStart.setSamplingTimeEnd(startDate);
        referenceStart.setValue(value);

        Date endDate = query.getTimespan()
                            .getEnd()
                            .toDate();
        QuantityDataEntity referenceEnd = new QuantityDataEntity();
        referenceEnd.setDataset(dataset);
        referenceEnd.setSamplingTimeStart(endDate);
        referenceEnd.setSamplingTimeEnd(endDate);
        referenceEnd.setValue(value);

        return new QuantityValue[] {
            assembleDataValue(referenceStart, dataset, query),
            assembleDataValue(referenceEnd, dataset, query),
        };
    }

    @Override
    public QuantityValue assembleDataValue(QuantityDataEntity observation, DatasetEntity dataset, DbQuery query) {
        QuantityValue value = createValue(observation, dataset, query);
        return addMetadatasIfNeeded(observation, value, dataset, query);
    }

    private QuantityValue createValue(QuantityDataEntity observation, DatasetEntity dataset, DbQuery query) {
        ServiceEntity service = getServiceEntity(dataset);
        return !service.isNoDataValue(observation)
                ? createValue(format(observation, dataset), observation, query)
                : null;
    }

    QuantityValue createValue(BigDecimal observationValue, QuantityDataEntity observation, DbQuery query) {
        QuantityValue value = prepareValue(observation, query);
        value.setValue(observationValue);

        Locale locale = LocaleHelper.decode(query.getLocale());
        NumberFormat formatter = NumberFormat.getInstance(locale);
        value.setValueFormatter(formatter::format);
        return value;
    }

    private BigDecimal format(QuantityDataEntity observation, DatasetEntity dataset) {
        return format(observation.getValue(), dataset.getNumberOfDecimals());
    }

}
