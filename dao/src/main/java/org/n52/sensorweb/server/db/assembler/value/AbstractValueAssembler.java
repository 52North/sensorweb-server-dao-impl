/*
 * Copyright (C) 2015-2023 52°North Spatial Information Research GmbH
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
package org.n52.sensorweb.server.db.assembler.value;

import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.ProfileOutput;
import org.n52.sensorweb.server.db.TimeOutputCreator;
import org.n52.sensorweb.server.db.ValueAssembler;
import org.n52.sensorweb.server.db.factory.ServiceEntityFactory;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DataQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.DataRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.parameter.ParameterEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.StreamUtils;

public abstract class AbstractValueAssembler<E extends DataEntity<T>, V extends AbstractValue<?>, T>
        implements ValueAssembler<E, V, T>, TimeOutputCreator {

    /**
     * Preconfigured service entity. Alternative to accessing service entities from a database (in case there
     * data model and mappings supports it).
     *
     * @see #assertServiceAvailable(DescribableEntity)
     */
    @Inject
    protected ServiceEntityFactory serviceEntityFactory;

    private final DataRepository<E> dataRepository;

    private final DatasetRepository datasetRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Map<String, ValueConnector> connectors;

    protected AbstractValueAssembler(DataRepository<E> dataRepository, DatasetRepository datasetRepository) {
        this.dataRepository = dataRepository;
        this.datasetRepository = datasetRepository;
    }

    @Inject
    public void setConnectors(Optional<List<ValueConnector>> connectors) {
        this.connectors = connectors.isPresent()
                ? connectors.get().stream().collect(toMap(ValueConnector::getName, Function.identity()))
                : new LinkedHashMap<>();
    }

    protected boolean isNoDataValue(DataEntity<?> data, DatasetEntity dataset) {
        final ServiceEntity service = getServiceEntity(dataset);
        return service.isNoDataValue(data);
    }

    private ServiceEntity getServiceEntity(final DescribableEntity entity) {
        assertServiceAvailable(entity);
        return serviceEntityFactory.getServiceEntity(entity.getService());
    }

    private void assertServiceAvailable(final DescribableEntity entity) throws IllegalStateException {
        if (serviceEntityFactory == null && entity == null) {
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
        DataEntity<?> value = entity.getFirstObservation() != null ? entity.getFirstObservation()
                : entity.isSetFirstValueAt() ? getDataValueViaTimestart(entity, query) : null;
        return value != null ? assembleDataValueWithMetadata(unproxy(value), entity, query) : null;
    }

    @Override
    public V getLastValue(DatasetEntity entity, DbQuery query) {
        DataEntity<?> value = entity.getLastObservation() != null ? entity.getLastObservation()
                : entity.isSetLastValueAt() ? getDataValueViaTimeend(entity, query) : null;
        return value != null ? assembleDataValueWithMetadata(unproxy(value), entity, query) : null;
    }

    private DatasetEntity getDataset(DbQuery dbQuery, String id) {
        return !dbQuery.isMatchDomainIds() ? getDataset(Long.parseLong(id)) : datasetRepository.getOneByIdentifier(id);
    }

    private DatasetEntity getDataset(long id) {
        Optional<DatasetEntity> dataset = datasetRepository.findById(id);
        return dataset.orElse(null);
    }

    /**
     * Assembles an expanded view of data values. An expanded view may include for example
     * <ul>
     * <li>Reference values</li>
     * <li>First values beyond requested timespan interval</li>
     * <li>Further output for each data value</li>
     * </ul>
     *
     * By default this returns the output of {@link #assembleDataValues(DatasetEntity, DbQuery)}.
     * Implementations may override this method to include all metadata necessary for an expanded output.
     *
     * @param dataset
     *            the dataset
     * @param query
     *            the query
     * @return an expanded view of assembled data
     */
    protected Data<V> assembleExpandedDataValues(DatasetEntity dataset, DbQuery query) {
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
        V value = assembleDataValue((E) Hibernate.unproxy(data), dataset, query);
        return addMetadatasIfNeeded(data, value, dataset, query);
    }

    protected Stream<E> findAll(DatasetEntity dataset, DbQuery query) {
        DataQuerySpecifications dataFilterSpec = DataQuerySpecifications.<E> of(
                dataset != null ? query.replaceWith(Parameters.DATASETS, Long.toString(dataset.getId())) : query);
        Specification<E> predicate = dataFilterSpec.matchFilters();
        Iterable<E> entities = dataRepository.findAll(predicate);
        return StreamUtils.createStreamFromIterator(entities.iterator());
    }

    /**
     * Prepares data value by setting time/time interval depending on actual query.
     *
     * @param <O>
     *            the type of the assembled output value
     * @param value
     *            the actual (empty) value
     * @param observation
     *            the observation entity
     * @param dataset
     *            the dataset entity
     * @param query
     *            the query
     * @return the value with time
     */
    protected <O extends AbstractValue<?>> O prepareValue(O value, DataEntity<?> observation, DatasetEntity dataset,
            DbQuery query) {
        if (observation == null) {
            return value;
        }

        final IoParameters parameters = query.getParameters();
        final Date timeend = observation.getSamplingTimeEnd();
        final Date timestart = observation.getSamplingTimeStart();
        if (parameters.isSelected("timestamp") && parameters.isShowTimeIntervals() && timestart != null) {
            value.setTimestart(createTimeOutput(timestart, dataset.getOriginTimezone(), parameters));
        }
        if (parameters.isSelected("parameters") && observation.hasParameters()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (ParameterEntity<?> param : observation.getParameters()) {
                map.put(param.getName(), param.getValue());
            }
            value.addParameter(map);
        }
        value.setTimestamp(createTimeOutput(timeend, dataset.getOriginTimezone(), parameters));
        return value;
    }

    protected V addMetadatasIfNeeded(final E observation, final V value, final DatasetEntity dataset,
            final DbQuery query) {
        addResultTime(observation, value, dataset, query);

        if (query.isExpanded()) {
            addValidTime(observation, value, dataset, query);
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

    protected void addValidTime(final DataEntity<?> observation, final AbstractValue<?> value, DatasetEntity dataset,
            DbQuery query) {
        if (observation.isSetValidStartTime() || observation.isSetValidEndTime()) {
            final Date validFrom = observation.isSetValidStartTime() ? observation.getValidTimeStart() : null;
            final Date validUntil = observation.isSetValidEndTime() ? observation.getValidTimeEnd() : null;
            value.setValidTime(createTimeOutput(validFrom, dataset.getOriginTimezone(), query.getParameters()),
                    createTimeOutput(validUntil, dataset.getOriginTimezone(), query.getParameters()));
        }
    }

    protected void addResultTime(final DataEntity<?> observation, final AbstractValue<?> value, DatasetEntity dataset,
            DbQuery query) {
        if (observation.hasResultTime()) {
            value.setResultTime(
                    createTimeOutput(observation.getResultTime(), dataset.getOriginTimezone(), query.getParameters()));
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
        return (E) dataFilterSpec.matchClosestAfterEnd(dataset, getEntityManager()).orElse(null);
    }

    private E getDataValueViaTimestart(DatasetEntity entity, DbQuery query) {
        DataQuerySpecifications dataFilterSpec = DataQuerySpecifications.of(query);
        return (E) dataFilterSpec.matchStart(entity, entityManager).orElse(null);
    }

    private E getDataValueViaTimeend(DatasetEntity entity, DbQuery query) {
        DataQuerySpecifications dataFilterSpec = DataQuerySpecifications.of(query);
        return (E) dataFilterSpec.matchEnd(entity, entityManager).orElse(null);
    }

    protected E unproxy(DataEntity<?> dataEntity) {
        if (dataEntity instanceof HibernateProxy
                && ((HibernateProxy) dataEntity).getHibernateLazyInitializer().getSession() == null) {
            return unproxy(entityManager.find(DataEntity.class, dataEntity.getId()));
        }
        return (E) Hibernate.unproxy(dataEntity);
    }

    public Long getCount(DatasetEntity dataset, DbQuery query) {
        DataQuerySpecifications dataFilterSpec = DataQuerySpecifications.of(query);
        return dataFilterSpec.count(dataset, getEntityManager());
    }

    protected EntityManager getEntityManager() {
        return entityManager;
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

    protected BigDecimal format(BigDecimal value, Integer scale) {
        if (value == null || scale == null) {
            return value;
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    @Override
    public Map<String, ValueConnector> getConnectors() {
        return Collections.unmodifiableMap(connectors);
    }

}
