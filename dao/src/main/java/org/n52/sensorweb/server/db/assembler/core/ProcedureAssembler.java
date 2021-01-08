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

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;

import org.n52.io.response.ProcedureOutput;
import org.n52.sensorweb.server.db.assembler.ParameterOutputAssembler;
import org.n52.sensorweb.server.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DatasetQuerySpecifications;
import org.n52.sensorweb.server.db.query.ProcedureQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.sensorweb.server.db.repositories.core.ProcedureRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FormatEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.spi.search.ProcedureSearchResult;
import org.n52.shetland.ogc.OGCConstants;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ProcedureAssembler
        extends ParameterOutputAssembler<ProcedureEntity, ProcedureOutput, ProcedureSearchResult> {

    @Inject
    private FormatAssembler formatAssembler;

    public ProcedureAssembler(ProcedureRepository procedureRepository, DatasetRepository datasetRepository) {
        super(procedureRepository, datasetRepository);
    }

    @Override
    protected ProcedureOutput prepareEmptyOutput() {
        return new ProcedureOutput();
    }

    @Override
    protected ProcedureSearchResult prepareEmptySearchResult() {
        return new ProcedureSearchResult();
    }

    @Override
    protected Specification<ProcedureEntity> createFilterPredicate(DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = getDatasetQuerySpecification(query);
        ProcedureQuerySpecifications pFilterSpec = ProcedureQuerySpecifications.of(query);
        return pFilterSpec.selectFrom(dsFilterSpec.matchFilters());
    }

    @Override
    protected Specification<ProcedureEntity> createPublicPredicate(String id, DbQuery query) {
        final DatasetQuerySpecifications dsFilterSpec = getDatasetQuerySpecification(query);
        final Specification<DatasetEntity> datasetPredicate =
                dsFilterSpec.matchProcedures(id).and(dsFilterSpec.isPublic());
        ProcedureQuerySpecifications filterSpec = ProcedureQuerySpecifications.of(query);
        return filterSpec.selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }

    @Override
    public ProcedureEntity getOrInsertInstance(ProcedureEntity entity) {
        ProcedureEntity instance = getParameterRepository().getInstance(entity);
        if (instance != null) {
            return instance;
        }
        if (entity.hasParents()) {
            Set<ProcedureEntity> parents = new LinkedHashSet<>();
            for (ProcedureEntity parent : entity.getParents()) {
                parents.add(getOrInsertInstance(parent));
            }
            entity.setParents(parents);
        }
        entity.setFormat(formatAssembler.getOrInsertInstance(
                entity.isSetFormat() ? entity.getFormat() : new FormatEntity().setFormat(OGCConstants.UNKNOWN)));
        return getParameterRepository().saveAndFlush(entity);
    }

    @Override
    protected ParameterOutputSearchResultMapper<ProcedureEntity, ProcedureOutput> getMapper(DbQuery query) {
        return getOutputMapperFactory().getProcedureMapper(query);
    }

}
