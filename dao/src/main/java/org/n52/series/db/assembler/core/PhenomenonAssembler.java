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
package org.n52.series.db.assembler.core;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import org.n52.io.response.PhenomenonOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DatasetQuerySpecifications;
import org.n52.sensorweb.server.db.query.PhenomenonQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.sensorweb.server.db.repositories.core.PhenomenonRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.spi.search.PhenomenonSearchResult;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class PhenomenonAssembler
        extends HierarchicalAssembler<PhenomenonEntity, PhenomenonOutput, PhenomenonSearchResult> {

    public PhenomenonAssembler(PhenomenonRepository phenomenonRepository, DatasetRepository datasetRepository) {
        super(phenomenonRepository, datasetRepository);
    }

    @Override
    protected PhenomenonOutput prepareEmptyOutput() {
        return new PhenomenonOutput();
    }

    @Override
    protected PhenomenonSearchResult prepareEmptySearchResult() {
        return new PhenomenonSearchResult();
    }

    @Override
    protected Specification<PhenomenonEntity> createFilterPredicate(DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = getDatasetQuerySpecification(query);
        PhenomenonQuerySpecifications pFilterSpec = PhenomenonQuerySpecifications.of(query);
        return pFilterSpec.selectFrom(dsFilterSpec.matchFilters());
    }

    @Override
    protected Specification<PhenomenonEntity> createPublicPredicate(String id, DbQuery query) {
        final DatasetQuerySpecifications dsFilterSpec = getDatasetQuerySpecification(query);
        final Specification<DatasetEntity> datasetPredicate =
                dsFilterSpec.matchPhenomena(id).and(dsFilterSpec.isPublic());
        PhenomenonQuerySpecifications filterSpec = PhenomenonQuerySpecifications.of(query);
        return filterSpec.selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }

    @Override
    public PhenomenonEntity getOrInsertInstance(PhenomenonEntity entity) {
        PhenomenonEntity instance = getParameterRepository().getInstance(entity);
        if (instance != null) {
            return instance;
        }
        if (entity.hasParents()) {
            Set<PhenomenonEntity> parents = new LinkedHashSet<>();
            for (PhenomenonEntity parent : entity.getParents()) {
                parents.add(getOrInsertInstance(parent));
            }
            entity.setParents(parents);
        }
        return getParameterRepository().saveAndFlush(entity);
    }
}
