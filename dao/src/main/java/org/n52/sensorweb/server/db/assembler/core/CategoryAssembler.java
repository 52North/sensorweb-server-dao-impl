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
package org.n52.sensorweb.server.db.assembler.core;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.n52.io.response.CategoryOutput;
import org.n52.sensorweb.server.db.assembler.ParameterOutputAssembler;
import org.n52.sensorweb.server.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.CategoryQuerySpecifications;
import org.n52.sensorweb.server.db.query.DatasetQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.CategoryRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.spi.search.CategorySearchResult;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class CategoryAssembler extends ParameterOutputAssembler<CategoryEntity, CategoryOutput, CategorySearchResult> {

    @PersistenceContext
    private EntityManager entityManager;

    public CategoryAssembler(CategoryRepository categoryRepository, DatasetRepository datasetRepository) {
        super(categoryRepository, datasetRepository);
    }

    @Override
    protected CategoryOutput prepareEmptyOutput() {
        return new CategoryOutput();
    }

    @Override
    protected CategorySearchResult prepareEmptySearchResult() {
        return new CategorySearchResult();
    }

    @Override
    public Specification<CategoryEntity> createFilterPredicate(DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = getDatasetQuerySpecification(query);
        CategoryQuerySpecifications cFilterSpec = CategoryQuerySpecifications.of(query);
        return cFilterSpec.selectFrom(dsFilterSpec.matchFilters());
    }

    @Override
    protected Specification<CategoryEntity> createPublicPredicate(String id, DbQuery query) {
        final DatasetQuerySpecifications dsFilterSpec = getDatasetQuerySpecification(query);
        final Specification<DatasetEntity> datasetPredicate =
                dsFilterSpec.matchCategory(id).and(dsFilterSpec.isPublic());
        CategoryQuerySpecifications filterSpec = CategoryQuerySpecifications.of(query);
        return filterSpec.selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }

    @Override
    protected ParameterOutputSearchResultMapper<CategoryEntity, CategoryOutput> getMapper(DbQuery query) {
        return getOutputMapperFactory().getCategoryMapper(query);
    }

}
