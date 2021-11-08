/*
 * Copyright (C) 2015-2021 52°North Spatial Information Research GmbH
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
package org.n52.series.db.old.da.data;

import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.Session;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.n52.io.response.TimeOutput;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetMetadata;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.janmayen.i18n.LocaleHelper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.dataset.ValueType;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.DataDao;

//@ValueAssemblerComponent(value = "quantity", datasetEntityType = DatasetEntity.class)
public class QuantityDataRepository
        extends AbstractNumericalDataRepository<QuantityDataEntity, QuantityValue, BigDecimal> {

    public QuantityDataRepository(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    @Override
    protected QuantityValue createEmptyValue() {
        return new QuantityValue();
    }

    @Override
    public QuantityValue getFirstValue(DatasetEntity entity, DbQuery query) {
        if (entity.getFirstQuantityValue() != null) {
            QuantityValue value = createEmptyValue();
            value.setValue(format(entity.getFirstQuantityValue(), entity));
            value.setTimestamp(createTimeOutput(entity.getFirstValueAt(), null, query.getParameters()));
            Locale locale = LocaleHelper.decode(query.getLocale());
            NumberFormat formatter = NumberFormat.getInstance(locale);
            value.setValueFormatter(formatter::format);
            return value;
        }
        return super.getFirstValue(entity, query);
    }

    @Override
    public QuantityValue getLastValue(DatasetEntity entity, DbQuery query) {
        if (entity.getLastQuantityValue() != null) {
            QuantityValue value = createEmptyValue();
            value.setValue(format(entity.getLastQuantityValue(), entity));
            value.setTimestamp(createTimeOutput(entity.getLastValueAt(), null, query.getParameters()));
            Locale locale = LocaleHelper.decode(query.getLocale());
            NumberFormat formatter = NumberFormat.getInstance(locale);
            value.setValueFormatter(formatter::format);
            return value;
        }
        return super.getLastValue(entity, query);
    }

    @Override
    public List<ReferenceValueOutput<QuantityValue>> getReferenceValues(DatasetEntity dataset, DbQuery query) {
        List<DatasetEntity> referenceValues =
                dataset.getReferenceValues().stream().filter(Objects::nonNull).filter(rv -> rv.isPublished())
                        .filter(rv -> rv.getValueType() == ValueType.quantity).collect(toList());

        List<ReferenceValueOutput<QuantityValue>> outputs = new ArrayList<>();
        for (DatasetEntity referenceDatasetEntity : referenceValues) {
            if (referenceDatasetEntity != null && referenceDatasetEntity.getValueType().equals(ValueType.quantity)) {
                ReferenceValueOutput<QuantityValue> refenceValueOutput = new ReferenceValueOutput<>();
                ProcedureEntity procedure = referenceDatasetEntity.getProcedure();
                refenceValueOutput.setLabel(procedure.getNameI18n(query.getLocale()));
                refenceValueOutput.setReferenceValueId(createReferenceDatasetId(query, referenceDatasetEntity));

                refenceValueOutput.setLastValue(getLastValue(referenceDatasetEntity, query));
                outputs.add(refenceValueOutput);
            }
        }
        return outputs;
    }

    @Override
    protected Data<QuantityValue> assembleExpandedData(Long datasetId, DbQuery query, Session session) {
        DatasetEntity dataset = session.get(DatasetEntity.class, datasetId);

        Map<Long, List<QuantityDataEntity>> dataIncludeReferences = getDataIncludeReferences(dataset, query, session);

        Data<QuantityValue> result = assembleData(dataIncludeReferences.get(datasetId), query);
        DatasetMetadata<QuantityValue> metadata = result.getMetadata();

        if (metadata == null) {
            result.setMetadata(metadata = new DatasetMetadata<>());
        }

        List<DatasetEntity> referenceValues = dataset.getReferenceValues();
        if ((referenceValues != null) && !referenceValues.isEmpty()) {
            metadata.setReferenceValues(assembleReferenceSeries(dataset, dataIncludeReferences, query, session));
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

    private Map<Long, List<QuantityDataEntity>> getDataIncludeReferences(DatasetEntity dataset, DbQuery dbQuery,
            Session session) {
        Map<Long, List<QuantityDataEntity>> map = new HashMap<>();
        Set<Long> series = new LinkedHashSet<>();
        series.add(dataset.getId());
        map.put(dataset.getId(), new LinkedList<QuantityDataEntity>());
        List<DatasetEntity> referenceValues = dataset.getReferenceValues();
        if ((referenceValues != null) && !referenceValues.isEmpty()) {
            for (DatasetEntity seriesEntity : referenceValues) {
                if (seriesEntity != null && seriesEntity.isPublished()
                        && seriesEntity.getValueType().equals(ValueType.quantity)) {
                    series.add(seriesEntity.getId());
                    map.put(seriesEntity.getId(), new LinkedList<QuantityDataEntity>());
                }
            }
        }
        DataDao obsDao = createDataDao(session);
        List<DataEntity> observations = obsDao.getAllInstancesFor(series, dbQuery);
        for (DataEntity dataEntity : observations) {
            QuantityDataEntity observationEntity = unproxy(dataEntity, session);
            if (map.containsKey(unproxy(observationEntity, session).getDatasetId())) {
                map.get(observationEntity.getDatasetId()).add(observationEntity);
            }
        }
        return map;
    }

    private Map<String, Data<QuantityValue>> assembleReferenceSeries(DatasetEntity dataset,
            Map<Long, List<QuantityDataEntity>> data, DbQuery query, Session session) {
        Map<String, Data<QuantityValue>> referencedDatasets = new HashMap<>();
        Interval timespan = query.getTimespan();
        DateTime lowerBound = timespan.getStart();
        DateTime upperBound = timespan.getEnd();
        for (DatasetEntity referenceDatasetEntity : dataset.getReferenceValues()) {
            if (referenceDatasetEntity != null && referenceDatasetEntity.isPublished()
                    && referenceDatasetEntity.getValueType().equals(ValueType.quantity)) {
                Data<QuantityValue> referencedDatasetData =
                        assembleData(data.get(referenceDatasetEntity.getId()), query);
                if (haveToExpandReferenceData(referencedDatasetData)) {
                    referencedDatasetData = expandReferenceDataIfNecessary(referenceDatasetEntity,
                            referencedDatasetData, query, session);
                }
                if (query.expandWithNextValuesBeyondInterval()) {
                    QuantityDataEntity previousValue =
                            unproxy(getClosestValueBeforeStart(referenceDatasetEntity, query, session), session);
                    QuantityDataEntity nextValue =
                            unproxy(getClosestValueAfterEnd(referenceDatasetEntity, query, session), session);
                    DatasetMetadata<QuantityValue> metadata = referencedDatasetData.getMetadata();
                    if (metadata == null) {
                        referencedDatasetData.setMetadata(metadata = new DatasetMetadata<>());
                    }
                    QuantityValue before =
                            previousValue != null ? createValue(previousValue, referenceDatasetEntity, query) : null;
                    if (before != null) {
                        metadata.setValueBeforeTimespan(before);
                    } else {
                        QuantityValue firstItem = getFirstItem(referencedDatasetData);
                        QuantityValue quantityValue = new QuantityValue();
                        quantityValue.setValue(firstItem.getValue());
                        quantityValue.setTimestamp(new TimeOutput(lowerBound.minus(getOverlappingTime(timespan)),
                                firstItem.getTimestamp().isUnixTime()));
                        metadata.setValueBeforeTimespan(quantityValue);
                    }
                    QuantityValue after =
                            nextValue != null ? createValue(nextValue, referenceDatasetEntity, query) : null;
                    if (after != null) {
                        metadata.setValueAfterTimespan(after);
                    } else {
                        QuantityValue lastItem = getLastItem(referencedDatasetData);
                        QuantityValue quantityValue = new QuantityValue();
                        quantityValue.setValue(lastItem.getValue());
                        quantityValue.setTimestamp(new TimeOutput(upperBound.plus(getOverlappingTime(timespan)),
                                lastItem.getTimestamp().isUnixTime()));
                        metadata.setValueAfterTimespan(quantityValue);
                    }
                }
                referencedDatasets.put(createReferenceDatasetId(query, referenceDatasetEntity), referencedDatasetData);
            }
        }
        return referencedDatasets;
    }

    private Long getOverlappingTime(Interval timespan) {
        return ((Double) (timespan.toDurationMillis() * 0.1)).longValue();
    }

    private QuantityValue getFirstItem(Data<QuantityValue> referenceSeriesData) {
        return referenceSeriesData.getValues() != null && !referenceSeriesData.getValues().isEmpty()
                && referenceSeriesData.getValues().size() > 0 ? referenceSeriesData.getValues().iterator().next()
                        : null;
    }

    private QuantityValue getLastItem(Data<QuantityValue> referenceSeriesData) {
        return referenceSeriesData.getValues() != null && !referenceSeriesData.getValues().isEmpty()
                && referenceSeriesData.getValues().size() > 0
                        ? referenceSeriesData.getValues().stream().reduce((prev, next) -> next).orElse(null)
                        : null;
    }

    protected String createReferenceDatasetId(DbQuery query, DatasetEntity referenceDataset) {
        DatasetOutput<?> dataset = new DatasetOutput();
        Long id = referenceDataset.getId();
        dataset.setId(id.toString());
        return dataset.getId();
    }

    private boolean haveToExpandReferenceData(Data<QuantityValue> referencedDatasetData) {
        return referencedDatasetData.getValues().size() <= 1;
    }

    private Data<QuantityValue> expandReferenceDataIfNecessary(DatasetEntity dataset, Data<QuantityValue> data,
            DbQuery query, Session session) {
        Data<QuantityValue> result = new Data<>();
        if (data == null || data.getValues().isEmpty()) {
            result.addValues(expandToInterval(dataset.getLastQuantityValue(), dataset, query));
        } else if (data.getValues().size() == 1) {
            result.addValues(expandToInterval(data.getValues().iterator().next().getValue(), dataset, query));
        }
        return result;
    }

    @Override
    protected Data<QuantityValue> assembleData(DatasetEntity dataset, DbQuery query, Session session) {
        return assembleData(dataset.getId(), query, session);
    }

    @Override
    protected Data<QuantityValue> assembleData(Long dataset, DbQuery query, Session session) {
        // TODO: How to handle observations with detection limit? Currentl, null
        // is returned a filtered
        return assembleData(createDataDao(session).getAllInstancesFor(dataset, query), query);
    }

    private Data<QuantityValue> assembleData(List<QuantityDataEntity> list, DbQuery query) {
        Data<QuantityValue> result = new Data<>();
        list.stream().filter(Objects::nonNull)
                .map(observation -> assembleDataValue(observation, observation.getDataset(), query))
                .filter(Objects::nonNull).forEachOrdered(result::addNewValue);
        return result;
    }

    private QuantityValue[] expandToInterval(BigDecimal value, DatasetEntity dataset, DbQuery query) {
        QuantityDataEntity referenceStart = new QuantityDataEntity();
        referenceStart.setDataset(dataset);
        Date startDate = query.getTimespan().getStart().toDate();
        referenceStart.setSamplingTimeStart(startDate);
        referenceStart.setSamplingTimeEnd(startDate);
        referenceStart.setValue(value);

        Date endDate = query.getTimespan().getEnd().toDate();
        QuantityDataEntity referenceEnd = new QuantityDataEntity();
        referenceEnd.setDataset(dataset);
        referenceEnd.setSamplingTimeStart(endDate);
        referenceEnd.setSamplingTimeEnd(endDate);
        referenceEnd.setValue(value);

        return new QuantityValue[] { assembleDataValue(referenceStart, dataset, query),
                assembleDataValue(referenceEnd, dataset, query), };
    }

    @Override
    public QuantityValue assembleDataValue(QuantityDataEntity observation, DatasetEntity dataset, DbQuery query) {
        QuantityValue value = createValue(observation, dataset, query);
        return addMetadatasIfNeeded(observation, value, dataset, query);
    }

    private QuantityValue createValue(QuantityDataEntity observation, DatasetEntity dataset, DbQuery query) {
        ServiceEntity service = getServiceEntity(dataset);
        return !service.isNoDataValue(observation) ? createValue(format(observation, dataset), observation, query)
                : null;
    }

    QuantityValue createValue(BigDecimal observationValue, QuantityDataEntity observation, DbQuery query) {
        QuantityValue value = prepareValue(observation, query);
        value.setValue(observationValue);
        value.setDetectionLimit(getDetectionLimit(observation));
        value.setValueFormatter(query.getNumberFormat()::format);
        return value;
    }

    private BigDecimal format(QuantityDataEntity observation, DatasetEntity dataset) {
        return format(observation.getValue(), dataset.getNumberOfDecimals());
    }

    @Override
    protected DataDao<QuantityDataEntity> createDataDao(Session session) {
        return new DataDao(session, QuantityDataEntity.class);
    }
}
