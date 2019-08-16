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
package org.n52.series.db.assembler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Hibernate;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.Data;
import org.n52.series.db.TimeOutputCreator;
import org.n52.series.db.ValueAssembler;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.parameter.ParameterEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.query.DataQuerySpecifications;
import org.n52.series.db.repositories.DataRepository;
import org.n52.series.db.repositories.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.StreamUtils;

public abstract class AbstractValueAssembler<E extends DataEntity<T>, V extends AbstractValue<?>, T>
        implements ValueAssembler<E, V, T>, TimeOutputCreator {

    /**
     * Preconfigured service entity. Alternative to accessing service entities
     * from a database (in case there data model and mappings supports it).
     *
     * @see #assertServiceAvailable(DescribableEntity)
     */
    @Autowired(required = false)
    protected ServiceEntity serviceEntity;

    private final DataRepository<E> dataRepository;

    private final DatasetRepository datasetRepository;

    @PersistenceContext
    private EntityManager entityManager;

    protected AbstractValueAssembler(DataRepository<E> dataRepository, DatasetRepository datasetRepository) {
        this.dataRepository = dataRepository;
        this.datasetRepository = datasetRepository;
    }

    protected boolean isNoDataValue(DataEntity<?> data, DatasetEntity dataset) {
        final ServiceEntity service = getServiceEntity(dataset);
        return service.isNoDataValue(data);
    }

    private ServiceEntity getServiceEntity(final DescribableEntity entity) {
        assertServiceAvailable(entity);
        return entity.getService() != null ? entity.getService() : serviceEntity;
    }

    private void assertServiceAvailable(final DescribableEntity entity) throws IllegalStateException {
        if ((serviceEntity == null) && (entity == null)) {
            throw new IllegalStateException("No service instance available");
        }
    }

    @Override
    public Data<V> getData(String datasetId, DbQuery dbQuery) {
        // XXX make unproxying unnecessary!
        DatasetEntity dataset = (DatasetEntity) Hibernate.unproxy(getDataset(dbQuery, datasetId));
        return dbQuery.isExpanded() ? assembleExpandedDataValues(dataset, dbQuery)
                : assembleDataValues(dataset, dbQuery);
    }

    @Override
    public V getFirstValue(DatasetEntity entity, DbQuery query) {
        E data = (E) entity.getFirstObservation();
        return assembleDataValueWithMetadata(data, entity, query);
    }

    @Override
    public V getLastValue(DatasetEntity entity, DbQuery query) {
        E data = (E) entity.getLastObservation();
        return assembleDataValueWithMetadata(data, entity, query);
    }

    private DatasetEntity getDataset(DbQuery dbQuery, String id) {
        return !dbQuery.isMatchDomainIds() ? datasetRepository.getOne(Long.parseLong(id))
                : datasetRepository.getOneByIdentifier(id);
    }

    /**
     * Assembles an expanded view of data values. An expanded view may include
     * for example
     * <ul>
     * <li>Reference values</li>
     * <li>First values beyond requested timespan interval</li>
     * <li>Further output for each data value</li>
     * </ul>
     *
     * By default this returns the output of
     * {@link #assembleDataValues(DatasetEntity, DbQuery)}. Implementations may
     * override this method to include all metadata necessary for an expanded
     * output.
     *
     * @param dataset
     *            the dataset
     * @param query
     *            the query
     * @return an expanded view of assembled data
     */
    Data<V> assembleExpandedDataValues(DatasetEntity dataset, DbQuery query) {
        return assembleDataValues(dataset, query);
    }

    /**
     * Assembles data values.
     *
     * @param dataset
     *            the dataset
     * @param query
     *            the query
     * @return the assembled data
     */
    protected Data<V> assembleDataValues(DatasetEntity dataset, DbQuery query) {
        return findAll(dataset, query).filter(it -> it != null)
                .map(it -> assembleDataValueWithMetadata(it, dataset, query))
                .collect(Collectors.reducing(new Data<V>(), this::toData, Data::addData));
    }

    private Data<V> toData(V value) {
        Data<V> data = new Data<>();
        return data.addNewValue(value);
    }

    @Override
    public V assembleDataValueWithMetadata(E data, DatasetEntity dataset, DbQuery query) {
        V value = assembleDataValue(data, dataset, query);
        return addMetadatasIfNeeded(data, value, dataset, query);
    }

    protected Stream<E> findAll(DatasetEntity dataset, DbQuery query) {
        DataQuerySpecifications dataFilterSpec = DataQuerySpecifications.<E>of(query);
        Specification<E> predicate = dataFilterSpec.matchFilters(dataset);
        Iterable<E> entities = dataRepository.findAll(predicate);
        return StreamUtils.createStreamFromIterator(entities.iterator());
    }

    /**
     * Prepares data value by setting time/time interval depending on actual
     * query.
     *
     * @param <O>
     *            the type of the assembled output value
     * @param value
     *            the actual (empty) value
     * @param observation
     *            the observation entity
     * @param query
     *            the query
     * @return the value with time
     */
    <O extends AbstractValue<?>> O prepareValue(O value, DataEntity<?> observation, DbQuery query) {
        if (observation == null) {
            return value;
        }

        final IoParameters parameters = query.getParameters();
        final Date timeend = observation.getSamplingTimeEnd();
        final Date timestart = observation.getSamplingTimeStart();
        if (parameters.isShowTimeIntervals() && (timestart != null)) {
            value.setTimestart(createTimeOutput(timestart, parameters));
        }
        value.setTimestamp(createTimeOutput(timeend, parameters));
        return value;
    }

    protected V addMetadatasIfNeeded(final E observation, final V value, final DatasetEntity dataset,
            final DbQuery query) {
        addResultTime(observation, value);

        if (query.isExpanded()) {
            addValidTime(observation, value, query);
            addParameters(observation, value, query);
            addGeometry(observation, value, query);
        } else {
            if (dataset.isMobile()) {
                addGeometry(observation, value, query);
            }
        }
        return value;
    }

    protected void addGeometry(final DataEntity<?> dataEntity, final AbstractValue<?> value, final DbQuery query) {
        if (dataEntity.isSetGeometryEntity()) {
            final GeometryEntity geometryEntity = dataEntity.getGeometryEntity();
            final Geometry geometry = getGeometry(geometryEntity, query);
            value.setGeometry(geometry);
        }
    }

    protected Geometry getGeometry(GeometryEntity geometryEntity, DbQuery query) {
        if (geometryEntity == null) {
            return null;
        } else {
            String srid = query.getDatabaseSridCode();
            geometryEntity.setGeometryFactory(createGeometryFactory(srid));
            return geometryEntity.getGeometry();
        }
    }

    private GeometryFactory createGeometryFactory(String srsId) {
        PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
        return srsId == null ? new GeometryFactory(pm) : new GeometryFactory(pm, CRSUtils.getSrsIdFrom(srsId));
    }

    protected void addValidTime(final DataEntity<?> observation, final AbstractValue<?> value, DbQuery query) {
        if (observation.isSetValidStartTime() || observation.isSetValidEndTime()) {
            final Date validFrom = observation.isSetValidStartTime() ? observation.getValidTimeStart() : null;
            final Date validUntil = observation.isSetValidEndTime() ? observation.getValidTimeEnd() : null;
            value.setValidTime(createTimeOutput(validFrom, query.getParameters()),
                    createTimeOutput(validUntil, query.getParameters()));
        }
    }

    protected void addResultTime(final DataEntity<?> observation, final AbstractValue<?> value) {
        if (observation.getResultTime() != null) {
            value.setResultTime(new DateTime(observation.getResultTime()));
        }
    }

    protected void addParameters(final DataEntity<?> observation, final AbstractValue<?> value, final DbQuery query) {
        if (observation.hasParameters()) {
            for (final ParameterEntity<?> parameter : observation.getParameters()) {
                value.addParameter(parameter.toValueMap(query.getLocale()));
            }
        }
    }

    @Override
    public E getClosestValueBeforeStart(DatasetEntity dataset, DbQuery query) {
        // TODO check if data filters should be mutual exclusive
        // e.g. filter by bbox and get closest data point

        DataQuerySpecifications dataFilterSpec = DataQuerySpecifications.of(query);
        return (E) dataFilterSpec.matchClosestBeforeStart(dataset, entityManager).orElse(null);
    }

    @Override
    public E getClosestValueAfterEnd(DatasetEntity dataset, DbQuery query) {
        // TODO check if data filters should be mutual exclusive
        // e.g. filter by bbox and get closest data point^

        DataQuerySpecifications dataFilterSpec = DataQuerySpecifications.of(query);
        return (E) dataFilterSpec.matchClosestAfterEnd(dataset, entityManager).orElse(null);
    }

    protected boolean hasValidEntriesWithinRequestedTimespan(List<?> observations) {
        return observations.size() > 0;
    }

    protected boolean hasSingleValidReferenceValue(List<?> observations) {
        return observations.size() == 1;
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

}
