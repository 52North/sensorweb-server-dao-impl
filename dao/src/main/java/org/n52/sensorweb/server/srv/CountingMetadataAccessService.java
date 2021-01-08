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
package org.n52.sensorweb.server.srv;

import org.n52.io.request.IoParameters;
import org.n52.sensorweb.server.db.assembler.core.EntityCounter;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.sensorweb.server.db.old.dao.DefaultDbQueryFactory;
import org.n52.series.spi.srv.CountingMetadataService;
import org.springframework.stereotype.Component;

@Component
public class CountingMetadataAccessService implements CountingMetadataService {

    private final EntityCounter counter;

    private final DbQueryFactory dbQueryFactory;

    public CountingMetadataAccessService(EntityCounter counter, DbQueryFactory dbQueryFactory) {
        this.counter = counter;
        this.dbQueryFactory = dbQueryFactory == null
                ? new DefaultDbQueryFactory()
                : dbQueryFactory;
    }

    @Override
    public Long getServiceCount(IoParameters parameters) {
        // Spring configuration has only 1 service
        return 1L;
    }

    @Override
    public Long getOfferingCount(IoParameters parameters) {
        DbQuery query = dbQueryFactory.createFrom(parameters);
        return counter.countOfferings(query);
    }

    @Override
    public Long getCategoryCount(IoParameters parameters) {
        DbQuery query = dbQueryFactory.createFrom(parameters);
        return counter.countCategories(query);
    }

    @Override
    public Long getFeatureCount(IoParameters parameters) {
        DbQuery query = dbQueryFactory.createFrom(parameters);
        return counter.countFeatures(query);
    }

    @Override
    public Long getProcedureCount(IoParameters parameters) {
        DbQuery query = dbQueryFactory.createFrom(parameters);
        return counter.countProcedures(query);
    }

    @Override
    public Long getPhenomenaCount(IoParameters parameters) {
        DbQuery query = dbQueryFactory.createFrom(parameters);
        return counter.countPhenomena(query);
    }

    @Override
    public Long getPlatformCount(IoParameters parameters) {
        DbQuery query = dbQueryFactory.createFrom(parameters);
        return counter.countPlatforms(query);
    }

    @Override
    public Long getDatasetCount(IoParameters parameters) {
        DbQuery query = dbQueryFactory.createFrom(parameters);
        return counter.countDatasets(query);
    }

    @Override
    public Long getSamplingCounter(IoParameters parameters) {
        DbQuery query = dbQueryFactory.createFrom(parameters);
        return counter.countSamplings(query);
    }

    @Override
    public Long getMeasuringProgramCounter(IoParameters parameters) {
        DbQuery query = dbQueryFactory.createFrom(parameters);
        return counter.countMeasuringPrograms(query);
    }

    @Override
    public Long getTagCounter(IoParameters parameters) {
        DbQuery query = dbQueryFactory.createFrom(parameters);
        return counter.countTags(query);
    }

    @Override
    @Deprecated
    public Long getStationCount() {
        return 0L;
    }

    @Override
    public Long getTimeseriesCount() {
        return 0L;
    }

}
