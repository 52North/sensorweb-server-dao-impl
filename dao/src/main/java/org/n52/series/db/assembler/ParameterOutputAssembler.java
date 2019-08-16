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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.n52.io.response.AbstractOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.series.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.query.DatasetQuerySpecifications;
import org.n52.series.db.repositories.ParameterDataRepository;
import org.n52.series.db.repositories.core.DatasetRepository;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.srv.OutputAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.StreamUtils;

public abstract class ParameterOutputAssembler<E extends DescribableEntity,
                                               O extends AbstractOutput,
                                               S extends SearchResult>
        implements OutputAssembler<O>, InsertAssembler<E>, ClearAssembler<E> {

    @PersistenceContext
    private EntityManager entityManager;

    private final ParameterDataRepository<E> parameterRepository;

    private final DatasetRepository datasetRepository;

    // via database or config
    @Autowired(required = false)
    private ServiceEntity serviceEntity;

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

    protected O createExpanded(final E entity, final DbQuery query) {
        final ServiceEntity service = getServiceEntity(entity);
        final ParameterOutputSearchResultMapper mapper = getMapper(query);
        final O output = mapper.createCondensed(entity, prepareEmptyOutput());
        final ServiceOutput serviceOutput = mapper.createCondensed(service, new ServiceOutput());
        output.setValue(AbstractOutput.SERVICE, serviceOutput, query.getParameters(), output::setService);
        return output;
    }

    @Override
    public List<O> getAllCondensed(final DbQuery query) {
        return findAll(query).map(it -> getMapper(query).createCondensed(it, prepareEmptyOutput()))
                .collect(Collectors.toList());
    }

    @Override
    public List<O> getAllExpanded(final DbQuery query) {
        return findAll(query).map(it -> createExpanded(it, query)).collect(Collectors.toList());
    }

    @Override
    public O getInstance(final String id, final DbQuery query) {
        final Specification<E> publicEntity = createPublicPredicate(id, query);
        final Optional<E> entity = parameterRepository.findOne(publicEntity);
        return entity.map(it -> createExpanded(it, query)).orElse(null);
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

    private Stream<E> findAll(final DbQuery query) {
        final Specification<E> predicate = createFilterPredicate(query);
        final Iterable<E> entities = parameterRepository.findAll(predicate);
        return StreamUtils.createStreamFromIterator(entities.iterator());
    }

    protected ServiceEntity getServiceEntity(final DescribableEntity entity) {
        assertServiceAvailable(entity);
        return entity.getService() != null ? entity.getService() : serviceEntity;
    }

    private void assertServiceAvailable(final DescribableEntity entity) throws IllegalStateException {
        if ((serviceEntity == null) && (entity == null)) {
            throw new IllegalStateException("No service instance available");
        }
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

    protected ParameterOutputSearchResultMapper getMapper(final DbQuery query) {
        return new ParameterOutputSearchResultMapper(query);
    }

}
