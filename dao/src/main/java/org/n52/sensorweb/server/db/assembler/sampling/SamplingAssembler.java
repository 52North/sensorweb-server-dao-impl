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
package org.n52.sensorweb.server.db.assembler.sampling;

import org.n52.io.response.sampling.SamplingOutput;
import org.n52.sensorweb.server.db.assembler.ParameterOutputAssembler;
import org.n52.sensorweb.server.db.assembler.mapper.SamplingOutputMapper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.sensorweb.server.db.repositories.sampling.SamplingRepository;
import org.n52.series.db.beans.sampling.SamplingEntity;
import org.n52.series.spi.search.SamplingSearchResult;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Profile("sampling")
@Transactional
public class SamplingAssembler extends ParameterOutputAssembler<SamplingEntity, SamplingOutput, SamplingSearchResult> {

    public SamplingAssembler(SamplingRepository parameterRepository, DatasetRepository datasetRepository) {
        super(parameterRepository, datasetRepository);
    }

    @Override
    protected SamplingOutput prepareEmptyOutput() {
        return new SamplingOutput();
    }

    @Override
    protected SamplingSearchResult prepareEmptySearchResult() {
        return new SamplingSearchResult();
    }

    @Override
    protected Specification<SamplingEntity> createPublicPredicate(String id, DbQuery query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Specification<SamplingEntity> createFilterPredicate(DbQuery query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Specification<SamplingEntity> createSearchFilterPredicate(DbQuery query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected SamplingOutputMapper getMapper(DbQuery query) {
        return getOutputMapperFactory().getSamplingMapper(query);
    }

}
