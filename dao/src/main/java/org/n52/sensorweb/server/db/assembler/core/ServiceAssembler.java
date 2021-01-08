/*
 * Copyright (C) 2015-2021 52°North Initiative for Geospatial Open Source
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
package org.n52.sensorweb.server.db.assembler.core;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.n52.io.response.ServiceOutput;
import org.n52.sensorweb.server.db.assembler.ClearAssembler;
import org.n52.sensorweb.server.db.assembler.InsertAssembler;
import org.n52.sensorweb.server.db.assembler.mapper.OutputMapperFactory;
import org.n52.sensorweb.server.db.assembler.mapper.ServiceOutputMapper;
import org.n52.sensorweb.server.db.factory.ServiceEntityFactory;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DatasetQuerySpecifications;
import org.n52.sensorweb.server.db.query.ServiceQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.ParameterDataRepository;
import org.n52.sensorweb.server.srv.OutputAssembler;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.spi.search.ServiceSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.StreamUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ServiceAssembler
        implements OutputAssembler<ServiceOutput>, InsertAssembler<ServiceEntity>, ClearAssembler<ServiceEntity> {

    @PersistenceContext
    private EntityManager entityManager;

    private final Optional<ParameterDataRepository<ServiceEntity>> serviceRepository;

    @Autowired
    private ServiceEntityFactory serviceEntityFactory;

    @Autowired
    private OutputMapperFactory outputMapperFactory;

    public ServiceAssembler(final Optional<ParameterDataRepository<ServiceEntity>> serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    public ParameterDataRepository<ServiceEntity> getParameterRepository() {
        return isSetServiceRepository() ? serviceRepository.get() : null;
    }

    @Override
    public List<ServiceOutput> getAllCondensed(final DbQuery query) {
        return findAll(query).map(it -> getMapper(query).createCondensed(it, prepareEmptyOutput()))
                .collect(Collectors.toList());
    }

    private boolean isSetServiceRepository() {
        return serviceRepository.isPresent();
    }

    private ServiceOutputMapper getMapper(DbQuery query) {
        return outputMapperFactory.getServiceMapper();
    }

    @Override
    public List<ServiceOutput> getAllExpanded(final DbQuery query) {
        return findAll(query).map(it -> getMapper(query).createExpanded(it, prepareEmptyOutput()))
                .collect(Collectors.toList());
    }

    @Override
    public ServiceOutput getInstance(final String id, final DbQuery query) {
        Optional<ServiceEntity> entity = Optional.empty();
        if (isSetServiceRepository()) {
            final Specification<ServiceEntity> publicEntity = createPublicPredicate(id, query);
            entity = getParameterRepository().findOne(publicEntity);
        }
        return entity.map(it -> getMapper(query).createExpanded(it, prepareEmptyOutput())).orElse(
                getMapper(query).createExpanded(serviceEntityFactory.getServiceEntity(), prepareEmptyOutput()));
    }

    @Override
    public Collection<SearchResult> searchFor(final DbQuery query) {
        return findAll(query).parallel().map(it -> getMapper(query).createSearchResult(it, new ServiceSearchResult()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(final String id, final DbQuery query) {
        return (getParameterRepository() != null && getParameterRepository().exists(createPublicPredicate(id, query)))
                || serviceEntityFactory.getServiceEntity() != null;
    }

    public Specification<ServiceEntity> createPublicPredicate(final String id, DbQuery query) {
        final DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query, entityManager);
        final Specification<DatasetEntity> datasetPredicate =
                dsFilterSpec.matchFeatures(id).and(dsFilterSpec.isPublic());
        ServiceQuerySpecifications filterSpec = ServiceQuerySpecifications.of(query);
        return filterSpec.selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }

    protected Specification<ServiceEntity> createFilterPredicate(DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query, entityManager);
        ServiceQuerySpecifications fFilterSpec = ServiceQuerySpecifications.of(query);
        return fFilterSpec.selectFrom(dsFilterSpec.matchFilters());
    }

    private Stream<ServiceEntity> findAll(final DbQuery query) {
        Iterable<ServiceEntity> entities = null;
        if (isSetServiceRepository()) {
            final Specification<ServiceEntity> predicate = createFilterPredicate(query);
            entities =
                    getParameterRepository() != null ? getParameterRepository().findAll(predicate) : null;
        }
        if ((entities == null || !entities.iterator().hasNext()) && serviceEntityFactory.getServiceEntity() != null) {
            LinkedHashSet<ServiceEntity> set = new LinkedHashSet<>();
            set.add(serviceEntityFactory.getServiceEntity());
            entities = set;
        }
        return StreamUtils.createStreamFromIterator(entities.iterator());
    }

    private ServiceOutput prepareEmptyOutput() {
        return new ServiceOutput();
    }

    @Override
    public void clearUnusedForService(ServiceEntity service) {
        getParameterRepository().delete(service);
    }

}
