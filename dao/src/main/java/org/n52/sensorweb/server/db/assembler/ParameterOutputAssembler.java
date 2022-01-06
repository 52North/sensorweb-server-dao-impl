/*
 * Copyright (C) 2015-2022 52°North Spatial Information Research GmbH
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
package org.n52.sensorweb.server.db.assembler;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.n52.io.response.AbstractOutput;
import org.n52.sensorweb.server.db.assembler.mapper.OutputMapperFactory;
import org.n52.sensorweb.server.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DatasetQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.ParameterDataRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.sensorweb.server.db.repositories.core.UnitRepository;
import org.n52.sensorweb.server.srv.OutputAssembler;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.HibernateRelations;
import org.n52.series.db.beans.UnitEntity;
import org.n52.series.db.beans.parameter.ComplexParameterEntity;
import org.n52.series.db.beans.parameter.ParameterEntity;
import org.n52.series.spi.search.SearchResult;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.StreamUtils;

public abstract class ParameterOutputAssembler<E extends DescribableEntity,
                                               O extends AbstractOutput, S extends SearchResult>
        implements OutputAssembler<O>, InsertAssembler<E>, ClearAssembler<E> {

    @PersistenceContext
    private EntityManager entityManager;

    private final ParameterDataRepository<E> parameterRepository;

    private final DatasetRepository datasetRepository;

    @Inject
    private UnitRepository unitRepository;

    @Inject
    private OutputMapperFactory outputMapperFactory;

    public ParameterOutputAssembler(final ParameterDataRepository<E> parameterRepository,
            final DatasetRepository datasetRepository) {
        this.parameterRepository = parameterRepository;
        this.datasetRepository = datasetRepository;
    }

    protected abstract O prepareEmptyOutput();

    protected abstract S prepareEmptySearchResult();

    protected abstract Specification<E> createPublicPredicate(String id, DbQuery query);

    protected abstract Specification<E> createFilterPredicate(DbQuery query);

    public Long count(DbQuery query) {
        return getParameterRepository().count(createFilterPredicate(query));
    }

    @Override
    public List<O> getAllCondensed(final DbQuery query) {
        return findAll(query).parallel().map(it -> getMapper(query).createCondensed(it, prepareEmptyOutput()))
                .collect(Collectors.toList());
    }

    @Override
    public List<O> getAllExpanded(final DbQuery query) {
        return findAll(query).parallel().map(it -> getMapper(query).createExpanded(it, prepareEmptyOutput()))
                .collect(Collectors.toList());
    }

    @Override
    public O getInstance(final String id, final DbQuery query) {
        final Specification<E> publicEntity = createPublicPredicate(id, query);
        final Optional<E> entity = parameterRepository.findOne(publicEntity);
        return entity.map(it -> getMapper(query).createExpanded(it, prepareEmptyOutput())).orElse(null);
    }

    @Override
    public Collection<SearchResult> searchFor(final DbQuery query) {
        return findAll(query).map(it -> getMapper(query).createSearchResult(it, prepareEmptySearchResult()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(final String id, final DbQuery query) {
        return parameterRepository.exists(createPublicPredicate(id, query));
    }

    protected DatasetQuerySpecifications getDatasetQuerySpecification(DbQuery query) {
        return DatasetQuerySpecifications.of(query, entityManager);
    }

    public Stream<E> findAll(final DbQuery query) {
        final Specification<E> predicate = createFilterPredicate(query);
        final Iterable<E> entities = parameterRepository.findAll(predicate);
        return StreamUtils.createStreamFromIterator(entities.iterator());
    }

    @Override
    public E checkParameterUnits(E entity) {
        if (entity.hasParameters()) {
            for (ParameterEntity<?> parameter : entity.getParameters()) {
                checkUnit(parameter);
            }
        }
        return entity;
    }

    protected void checkUnit(ParameterEntity<?> parameter) {
        if (parameter instanceof HibernateRelations.HasUnit) {
            UnitEntity unit = ((HibernateRelations.HasUnit) parameter).getUnit();
            ((HibernateRelations.HasUnit) parameter).setUnit(getOrInsertUnit(unit));
        } else if (parameter instanceof ComplexParameterEntity) {
            Set<?> value = (Set<?>) ((ComplexParameterEntity) parameter).getValue();
            for (Object v : value) {
                if (v instanceof ParameterEntity) {
                    checkUnit((ParameterEntity) v);
                }
            }
        }
    }

    private UnitEntity getOrInsertUnit(UnitEntity unit) {
        if (unit != null && unit.isSetIdentifier()) {
            UnitEntity instance = unitRepository.getInstance(unit);
            if (instance != null) {
                return instance;
            }
            return unitRepository.saveAndFlush(unit);
        }
        return null;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public ParameterDataRepository<E> getParameterRepository() {
        return parameterRepository;
    }

    public DatasetRepository getDatasetRepository() {
        return datasetRepository;
    }

    protected OutputMapperFactory getOutputMapperFactory() {
        return outputMapperFactory;
    }

    protected abstract ParameterOutputSearchResultMapper<E, O> getMapper(DbQuery query);

}
