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

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.n52.io.response.PlatformOutput;
import org.n52.sensorweb.server.db.assembler.ParameterOutputAssembler;
import org.n52.sensorweb.server.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DatasetQuerySpecifications;
import org.n52.sensorweb.server.db.query.PlatformQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.sensorweb.server.db.repositories.core.PlatformRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.beans.sta.HistoricalLocationEntity;
import org.n52.series.db.beans.sta.LocationEntity;
import org.n52.series.spi.search.PlatformSearchResult;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
@Transactional
public class PlatformAssembler extends ParameterOutputAssembler<PlatformEntity, PlatformOutput, PlatformSearchResult> {

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private LocationAssembler locationAssembler;

    @Inject
    private HistoricalLocationAssembler historicalLocationAssembler;

    @SuppressFBWarnings({ "EI_EXPOSE_REP2" })
    public PlatformAssembler(PlatformRepository platformRepository, DatasetRepository datasetRepository) {
        super(platformRepository, datasetRepository);
    }

    @Override
    protected PlatformOutput prepareEmptyOutput() {
        return new PlatformOutput();
    }

    @Override
    protected PlatformSearchResult prepareEmptySearchResult() {
        return new PlatformSearchResult();
    }

    @Override
    protected Specification<PlatformEntity> createSearchFilterPredicate(DbQuery query) {
        PlatformQuerySpecifications filterSpec = PlatformQuerySpecifications.of(query);
        return createFilterPredicate(query, filterSpec).and(filterSpec.matchsLike());
    }

    @Override
    protected Specification<PlatformEntity> createFilterPredicate(DbQuery query) {
        return createFilterPredicate(query, PlatformQuerySpecifications.of(query));
    }

    private Specification<PlatformEntity> createFilterPredicate(DbQuery query,
            PlatformQuerySpecifications filterSpec) {
        DatasetQuerySpecifications dsFilterSpec = getDatasetQuerySpecification(query);
        return filterSpec.selectFrom(dsFilterSpec.matchFilters());
    }

    @Override
    public Specification<PlatformEntity> createPublicPredicate(final String id, DbQuery query) {
        final DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query, entityManager);
        final Specification<DatasetEntity> datasetPredicate =
                dsFilterSpec.matchPlatforms(id).and(dsFilterSpec.isPublic());
        PlatformQuerySpecifications filterSpec = PlatformQuerySpecifications.of(query);
        return filterSpec.selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }

    @Override
    protected ParameterOutputSearchResultMapper<PlatformEntity, PlatformOutput> getMapper(DbQuery query) {
        return getOutputMapperFactory().getPlatformMapper(query);
    }

    @Override
    public PlatformEntity checkReferencedEntities(PlatformEntity entity) {
        if (entity.hasLocations()) {
            Set<LocationEntity> newLocations = new LinkedHashSet<>();
            for (LocationEntity location : entity.getLocations()) {
                newLocations.add(locationAssembler.getOrInsertInstance(location));
            }
            entity.setLocations(newLocations);
        }
        if (entity.hasHistoricalLocations()) {
            Set<HistoricalLocationEntity> newHistoricalLocations = new LinkedHashSet<>();
            for (HistoricalLocationEntity historicalLocation : entity.getHistoricalLocations()) {
                newHistoricalLocations.add(historicalLocationAssembler.getOrInsertInstance(historicalLocation));
            }
            entity.setHistoricalLocations(newHistoricalLocations);
        }
        return super.checkReferencedEntities(entity);
    }

}
