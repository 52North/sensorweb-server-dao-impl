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
package org.n52.series.db.assembler.core;

import org.n52.io.response.OfferingOutput;
import org.n52.series.db.assembler.ParameterOutputAssembler;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.query.DatasetQuerySpecifications;
import org.n52.series.db.query.OfferingQuerySpecifications;
import org.n52.series.db.repositories.core.DatasetRepository;
import org.n52.series.db.repositories.core.OfferingRepository;
import org.n52.series.spi.search.OfferingSearchResult;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class OfferingAssembler extends ParameterOutputAssembler<OfferingEntity, OfferingOutput, OfferingSearchResult> {

    public OfferingAssembler(OfferingRepository offeringRepository, DatasetRepository datasetRepository) {
        super(offeringRepository, datasetRepository);
    }

    @Override
    protected OfferingOutput prepareEmptyOutput() {
        return new OfferingOutput();
    }

    @Override
    protected OfferingSearchResult prepareEmptySearchResult() {
        return new OfferingSearchResult();
    }

    @Override
    protected Specification<OfferingEntity> createFilterPredicate(DbQuery query) {
        OfferingQuerySpecifications oFilterSpec = OfferingQuerySpecifications.of(query);
        return oFilterSpec.selectFrom(getDatasetQuerySpecification(query).matchFilters());
    }

    @Override
    protected Specification<OfferingEntity> createPublicPredicate(String id, DbQuery query) {
        final DatasetQuerySpecifications dsFilterSpec = getDatasetQuerySpecification(query);
        final Specification<DatasetEntity> datasetPredicate =
                dsFilterSpec.matchOfferings(id).and(dsFilterSpec.isPublic());
        OfferingQuerySpecifications filterSpec = OfferingQuerySpecifications.of(query);
        return filterSpec.selectFrom(datasetPredicate);
    }

}
