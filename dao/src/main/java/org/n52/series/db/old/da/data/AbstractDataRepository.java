/*
 * Copyright (C) 2015-2020 52°North Initiative for Geospatial Open Source
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

package org.n52.series.db.old.da.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxy;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.n52.io.request.IoParameters;
import org.n52.io.response.DetectionLimitOutput;
import org.n52.io.response.TimeOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.Data;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.assembler.value.ValueConnector;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.dataset.DatasetType;
import org.n52.series.db.beans.parameter.ParameterEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.da.SessionAwareAssembler;
import org.n52.series.db.old.dao.DataDao;

public abstract class AbstractDataRepository<E extends DataEntity<T>, V extends AbstractValue<?>, T>
        extends SessionAwareAssembler implements DataRepository<E, V, T> {

    public AbstractDataRepository(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    @Override
    public Data<V> getData(String datasetId, DbQuery dbQuery) {
        Session session = getSession();
        try {
            return dbQuery.isExpanded() ? assembleExpandedData(Long.parseLong(datasetId), dbQuery, session)
                    : assembleData(Long.parseLong(datasetId), dbQuery, session);
        } finally {
            returnSession(session);
        }
    }

    protected Data<V> assembleExpandedData(DatasetEntity dataset, DbQuery dbQuery, Session session) {
        return assembleExpandedData(dataset.getId(), dbQuery, session);
    }

    protected Data<V> assembleExpandedData(Long dataset, DbQuery dbQuery, Session session) {
        return assembleData(dataset, dbQuery, session);
    }

    protected Data<V> assembleData(DatasetEntity dataset, DbQuery query, Session session) {
        return assembleData(dataset.getId(), query, session);
    }

    protected abstract Data<V> assembleData(Long dataset, DbQuery query, Session session);

    @Override
    public V assembleDataValueWithMetadata(E data, DatasetEntity dataset, DbQuery query) {
        V value = assembleDataValue(data, dataset, query);
        return addMetadatasIfNeeded(data, value, dataset, query);
    }

    @Override
    public V getFirstValue(DatasetEntity entity, DbQuery query) {
        DataEntity<?> value = entity.getFirstObservation() != null ? entity.getFirstObservation()
                : entity.isSetFirstValueAt() ? createDataDao(getSession()).getDataValueViaTimestart(entity, query)
                        : null;
        return value != null ? assembleDataValue(unproxy(value, getSession()), entity, query) : null;
    }

    @Override
    public V getLastValue(DatasetEntity entity, DbQuery query) {
        DataEntity<?> value = entity.getLastObservation() != null ? entity.getLastObservation()
                : entity.isSetLastValueAt() ? createDataDao(getSession()).getDataValueViaTimeend(entity, query) : null;
        return value != null ? assembleDataValue(unproxy(value, getSession()), entity, query) : null;
    }

    protected DataDao<E> createDataDao(Session session) {
        return new DataDao<>(session);
    }

    protected abstract V createEmptyValue();

    protected V prepareValue(E observation, DbQuery query) {
        V emptyValue = createEmptyValue();
        if (observation == null) {
            return emptyValue;
        }

        IoParameters parameters = query.getParameters();
        TimeOutput timeend = createTimeOutput(observation.getSamplingTimeEnd(),
                observation.getDataset().getOriginTimezone(), parameters);
        TimeOutput timestart = createTimeOutput(observation.getSamplingTimeStart(),
                observation.getDataset().getOriginTimezone(), parameters);
        if (parameters.isShowTimeIntervals() && (timestart != null)) {
            emptyValue.setTimestart(timestart);
        }
        emptyValue.setTimestamp(timeend);
        if (DatasetType.trajectory.equals(observation.getDataset().getDatasetType())
                && observation.isSetGeometryEntity()) {
            emptyValue.setGeometry(observation.getGeometryEntity().getGeometry());
        }
        return emptyValue;
    }

    protected boolean hasValidEntriesWithinRequestedTimespan(List<?> observations) {
        return observations.size() > 0;
    }

    protected boolean hasSingleValidReferenceValue(List<?> observations) {
        return observations.size() == 1;
    }

    protected V addMetadatasIfNeeded(E observation, V value, DatasetEntity dataset, DbQuery query) {
        // TODO how to handle NULL values, e.g. for detection limit
        if (value != null) {
            addResultTime(observation, value);

            if (query.isExpanded()) {
                addValidTime(observation, value, query.getParameters());
                addParameters(observation, value, query);
                addGeometry(observation, value, query);
            } else {
                if (dataset.isMobile()) {
                    addGeometry(observation, value, query);
                }
            }
        }
        return value;
    }

    protected void addGeometry(DataEntity<?> dataEntity, AbstractValue<?> value, DbQuery query) {
        if (dataEntity.isSetGeometryEntity()) {
            GeometryEntity geometry = dataEntity.getGeometryEntity();
            value.setGeometry(geometry.getGeometry());
        }
    }

    protected void addValidTime(DataEntity<?> observation, AbstractValue<?> value, IoParameters parameters) {
        if (observation.isSetValidStartTime() || observation.isSetValidEndTime()) {
            TimeOutput validFrom =
                    observation.isSetValidStartTime() ? createTimeOutput(observation.getValidTimeStart(), parameters)
                            : null;
            TimeOutput validUntil =
                    observation.isSetValidEndTime() ? createTimeOutput(observation.getValidTimeEnd(), parameters)
                            : null;
            value.setValidTime(validFrom, validUntil);
        }
    }

    protected void addResultTime(DataEntity<?> observation, AbstractValue<?> value) {
        if (observation.getResultTime() != null) {
            value.setResultTime(new DateTime(observation.getResultTime()));
        }
    }

    protected void addParameters(DataEntity<?> observation, AbstractValue<?> value, DbQuery query) {
        if (observation.hasParameters()) {
            for (ParameterEntity<?> parameter : observation.getParameters()) {
                value.addParameter(parameter.toValueMap(query.getLocale()));
            }
        }
    }

    @Override
    public E getClosestValueBeforeStart(DatasetEntity dataset, DbQuery query) {
        Session session = getSession();
        try {
            return getClosestValueBeforeStart(dataset, query, session);
        } finally {
            returnSession(session);
        }
    }

    protected E getClosestValueBeforeStart(DatasetEntity dataset, DbQuery query, Session session) {
        final DataDao<E> dao = createDataDao(session);
        final Interval timespan = query.getTimespan();
        final DateTime lowerBound = timespan.getStart();
        return dao.getClosestOuterPreviousValue(dataset, lowerBound, query);
    }

    @Override
    public E getClosestValueAfterEnd(DatasetEntity dataset, DbQuery query) {
        Session session = getSession();
        try {
            return getClosestValueAfterEnd(dataset, query, session);
        } finally {
            returnSession(session);
        }
    }

    protected E getClosestValueAfterEnd(DatasetEntity dataset, DbQuery query, Session session) {
        final DataDao<E> dao = createDataDao(session);
        final Interval timespan = query.getTimespan();
        final DateTime upperBound = timespan.getEnd();
        return dao.getClosestOuterNextValue(dataset, upperBound, query);
    }

    protected E unproxy(DataEntity<?> dataEntity, Session session) {
        if (dataEntity instanceof HibernateProxy
                && ((HibernateProxy) dataEntity).getHibernateLazyInitializer().getSession() == null) {
            return unproxy(session.load(DataEntity.class, dataEntity.getId()), session);
        }
        return (E) Hibernate.unproxy(dataEntity);
    }

    protected BigDecimal format(BigDecimal value, DatasetEntity dataset) {
        return format(value, dataset.getNumberOfDecimals());
    }

    protected BigDecimal format(BigDecimal value, int scale) {
        if (value == null) {
            return value;
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    protected DetectionLimitOutput getDetectionLimit(DataEntity<?> o) {
        if (o.hasDetectionLimit()) {
            DetectionLimitOutput result = new DetectionLimitOutput();
            result.setFlag(o.getDetectionLimit().getFlag());
            result.setDetectionLimit(o.getDetectionLimit().getDetectionLimit());
            return result;
        }
        return null;
    }

    protected Long getCount(DatasetEntity dataset, DbQuery query, Session session) {
        return createDataDao(session).getCount(dataset);
    }

    @Override
    public Map<String, ValueConnector> getConnectors() {
        return Collections.emptyMap();
    }
}
