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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.n52.io.response.AbstractOutput;
import org.n52.sensorweb.server.db.assembler.mapper.OutputMapperFactory;
import org.n52.sensorweb.server.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.repositories.ParameterDataRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.sensorweb.server.srv.OutputAssembler;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.spi.search.SearchResult;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.domain.Specification;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public abstract class ParameterOutputAssembler<E extends DescribableEntity,
                                               O extends AbstractOutput, S extends SearchResult>
        extends ParameterAssembler<E>
        implements OutputAssembler<O> {


    private final DatasetRepository datasetRepository;

    @Lazy
    @Inject
    private OutputMapperFactory outputMapperFactory;

    public ParameterOutputAssembler(final ParameterDataRepository<E> parameterRepository,
            final DatasetRepository datasetRepository) {
       super(parameterRepository);
        this.datasetRepository = datasetRepository;
    }

    protected abstract O prepareEmptyOutput();

    protected abstract S prepareEmptySearchResult();

    protected abstract Specification<E> createPublicPredicate(String id, DbQuery query);

    protected abstract Specification<E> createSearchFilterPredicate(DbQuery query);


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
        final Optional<E> entity = getParameterRepository().findOne(publicEntity);
        return entity.map(it -> getMapper(query).createExpanded(it, prepareEmptyOutput())).orElse(null);
    }

    @Override
    public Collection<SearchResult> searchFor(final DbQuery query) {
        return findAllSearch(query).map(it -> getMapper(query).createSearchResult(it, prepareEmptySearchResult()))
                .collect(Collectors.toList());
    }

    public Stream<E> findAllSearch(final DbQuery query) {
        return findAll(createSearchFilterPredicate(query));
    }

    @Override
    public boolean exists(final String id, final DbQuery query) {
        return getParameterRepository().exists(createPublicPredicate(id, query));
    }


    public DatasetRepository getDatasetRepository() {
        return datasetRepository;
    }

    protected OutputMapperFactory getOutputMapperFactory() {
        return outputMapperFactory;
    }

    protected abstract ParameterOutputSearchResultMapper<E, O> getMapper(DbQuery query);

}
