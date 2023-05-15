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
package org.n52.sensorweb.server.db.assembler.core;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.n52.io.response.FeatureOutput;
import org.n52.sensorweb.server.db.assembler.ParameterOutputAssembler;
import org.n52.sensorweb.server.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DatasetQuerySpecifications;
import org.n52.sensorweb.server.db.query.FeatureQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.sensorweb.server.db.repositories.core.FeatureRepository;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.spi.search.FeatureSearchResult;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class FeatureAssembler
        extends ParameterOutputAssembler<AbstractFeatureEntity, FeatureOutput, FeatureSearchResult> {

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private DatasetAssembler<?> datasetAssembler;

    public FeatureAssembler(FeatureRepository featureRepository, DatasetRepository datasetRepository) {
        super(featureRepository, datasetRepository);
    }

    @Override
    protected FeatureOutput prepareEmptyOutput() {
        return new FeatureOutput();
    }

    @Override
    protected FeatureSearchResult prepareEmptySearchResult() {
        return new FeatureSearchResult();
    }

    @Override
    protected Specification<AbstractFeatureEntity> createSearchFilterPredicate(DbQuery query) {
        FeatureQuerySpecifications filterSpec = FeatureQuerySpecifications.of(query, entityManager);
        return createFilterPredicate(query, filterSpec).and(filterSpec.matchsLike());
    }

    @Override
    protected Specification<AbstractFeatureEntity> createFilterPredicate(DbQuery query) {
        return createFilterPredicate(query, FeatureQuerySpecifications.of(query, entityManager));
    }

    private Specification<AbstractFeatureEntity> createFilterPredicate(DbQuery query,
            FeatureQuerySpecifications filterSpec) {
        DatasetQuerySpecifications dsFilterSpec = getDatasetQuerySpecification(query);
        return filterSpec.selectFrom(dsFilterSpec.matchFilters());
    }

    @Override
    public Specification<AbstractFeatureEntity> createPublicPredicate(final String id, DbQuery query) {
        final DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query, entityManager);
        final Specification<DatasetEntity> datasetPredicate =
                dsFilterSpec.matchFeatures(id).and(dsFilterSpec.isPublic());
        FeatureQuerySpecifications filterSpec = FeatureQuerySpecifications.of(query, entityManager);
        return filterSpec.selectFrom(dsFilterSpec.toSubquery(datasetPredicate), id);
    }

    @Override
    public AbstractFeatureEntity getOrInsertInstance(AbstractFeatureEntity entity) {
        AbstractFeatureEntity<?> instance = getParameterRepository().getInstance(entity);
        if (instance != null) {
            return instance;
        }
        if (entity.hasParents()) {
            Set<AbstractFeatureEntity> parents = new LinkedHashSet<>();
            for (Object parent : entity.getParents()) {
                if (parent instanceof AbstractFeatureEntity) {
                    parents.add(getOrInsertInstance((AbstractFeatureEntity) parent));
                }
            }
            entity.setParents(parents);
        }
        entity.setFeatureType(getFormat(entity.getFeatureType()));
        return super.getOrInsertInstance(entity);
    }

    @Override
    public AbstractFeatureEntity checkReferencedEntities(AbstractFeatureEntity entity) {
        entity.setFeatureType(getFormat(entity.getFeatureType()));
        return super.checkReferencedEntities(entity);
    }

    @Override
    protected ParameterOutputSearchResultMapper<AbstractFeatureEntity, FeatureOutput> getMapper(DbQuery query) {
        return getOutputMapperFactory().getFeatureMapper(query);
    }

}
