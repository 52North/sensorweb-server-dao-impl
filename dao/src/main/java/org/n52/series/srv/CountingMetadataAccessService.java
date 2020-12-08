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
package org.n52.series.srv;

import org.n52.io.request.IoParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.da.EntityCounter;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DbQueryFactory;
import org.n52.series.spi.srv.CountingMetadataService;
import org.n52.web.exception.InternalServerException;
import org.springframework.beans.factory.annotation.Autowired;

public class CountingMetadataAccessService implements CountingMetadataService {

    @Autowired
    private EntityCounter counter;

    @Autowired
    private DbQueryFactory dbQueryFactory;

    @Override
    public int getServiceCount(IoParameters parameters) {
        // Spring configuration has only 1 service
        return 1;
    }

    @Override
    public int getOfferingCount(IoParameters parameters) {
        try {
            DbQuery query = dbQueryFactory.createFrom(parameters);
            return counter.countOfferings(query);
        } catch (DataAccessException e) {
            throwCouldNotCountEntityException("offering", e);
            return -1;
        }
    }

    @Override
    public int getCategoryCount(IoParameters parameters) {
        try {
            DbQuery query = dbQueryFactory.createFrom(parameters);
            return counter.countCategories(query);
        } catch (DataAccessException e) {
            throwCouldNotCountEntityException("category", e);
            return -1;
        }
    }

    @Override
    public int getFeatureCount(IoParameters parameters) {
        try {
            DbQuery query = dbQueryFactory.createFrom(parameters);
            return counter.countFeatures(query);
        } catch (DataAccessException e) {
            throwCouldNotCountEntityException("feature", e);
            return -1;
        }
    }

    @Override
    public int getProcedureCount(IoParameters parameters) {
        try {
            DbQuery query = dbQueryFactory.createFrom(parameters);
            return counter.countProcedures(query);
        } catch (DataAccessException e) {
            throwCouldNotCountEntityException("procedure", e);
            return -1;
        }
    }

    @Override
    public int getPhenomenaCount(IoParameters parameters) {
        try {
            DbQuery query = dbQueryFactory.createFrom(parameters);
            return counter.countPhenomena(query);
        } catch (DataAccessException e) {
            throwCouldNotCountEntityException("phenomena", e);
            return -1;
        }
    }

    @Override
    public int getPlatformCount(IoParameters parameters) {
        try {
            DbQuery query = dbQueryFactory.createFrom(parameters);
            return counter.countPlatforms(query);
        } catch (DataAccessException e) {
            throwCouldNotCountEntityException("platform", e);
            return -1;
        }
    }

    @Override
    public int getDatasetCount(IoParameters parameters) {
        try {
            DbQuery query = dbQueryFactory.createFrom(parameters);
            return counter.countDatasets(query);
        } catch (DataAccessException e) {
            throwCouldNotCountEntityException("dataset", e);
            return -1;
        }
    }

    @Override
    @Deprecated
    public int getStationCount() {
        try {
            return counter.countStations();
        } catch (DataAccessException e) {
            throwCouldNotCountEntityException("station", e);
            return -1;
        }
    }

    @Override
    @Deprecated
    public int getTimeseriesCount() {
        try {
            return counter.countTimeseries();
        } catch (DataAccessException e) {
            throwCouldNotCountEntityException("timeseries", e);
            return -1;
        }
    }

    private void throwCouldNotCountEntityException(String entity, DataAccessException e)
            throws InternalServerException {
        throw new InternalServerException("Could not count " + entity + " entities.", e);
    }

}
