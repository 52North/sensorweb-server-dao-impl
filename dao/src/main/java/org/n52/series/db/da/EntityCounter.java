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
package org.n52.series.db.da;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.HibernateSessionStore;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.dao.AbstractDao;
import org.n52.series.db.dao.CategoryDao;
import org.n52.series.db.dao.DatasetDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DbQueryFactory;
import org.n52.series.db.dao.FeatureDao;
import org.n52.series.db.dao.MeasuringProgramDao;
import org.n52.series.db.dao.OfferingDao;
import org.n52.series.db.dao.PhenomenonDao;
import org.n52.series.db.dao.PlatformDao;
import org.n52.series.db.dao.ProcedureDao;
import org.n52.series.db.dao.SamplingDao;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityCounter {

    private final HibernateSessionStore sessionStore;

    private final DataRepositoryTypeFactory dataRepositoryFactory;

    private final DbQueryFactory dbQueryFactory;

    @Autowired
    public EntityCounter(HibernateSessionStore sesionStore, DataRepositoryTypeFactory dataRepositoryFactory,
            DbQueryFactory dbQueryFactory) {
        this.sessionStore = sesionStore;
        this.dataRepositoryFactory = dataRepositoryFactory;
        this.dbQueryFactory = dbQueryFactory;
    }

    public Long countFeatures(DbQuery query) throws DataAccessException {
        Session session = sessionStore.getSession();
        try {
            return getCount(new FeatureDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Long countOfferings(DbQuery query) throws DataAccessException {
        Session session = sessionStore.getSession();
        try {
            return getCount(new OfferingDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Long countProcedures(DbQuery query) throws DataAccessException {
        Session session = sessionStore.getSession();
        try {
            return getCount(new ProcedureDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Long countPhenomena(DbQuery query) throws DataAccessException {
        Session session = sessionStore.getSession();
        try {
            return getCount(new PhenomenonDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Long countCategories(DbQuery query) throws DataAccessException {
        Session session = sessionStore.getSession();
        try {
            return getCount(new CategoryDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Long countPlatforms(DbQuery query) throws DataAccessException {
        Session session = sessionStore.getSession();
        try {
            return getCount(new PlatformDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Long countDatasets(DbQuery query) throws DataAccessException {
        Session session = sessionStore.getSession();
        try {
            IoParameters parameters = query.getParameters();
            if (parameters.getValueTypes().isEmpty()) {
                parameters = parameters.extendWith("valueTypes",
                        dataRepositoryFactory.getKnownTypes().toArray(new String[0]));
                return getCount(new DatasetDao<>(session, DatasetEntity.class), dbQueryFactory.createFrom(parameters));
            }
            return getCount(new DatasetDao<>(session, DatasetEntity.class), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Long countSamplings(DbQuery query) throws DataAccessException {
        Session session = sessionStore.getSession();
        try {
            return getCount(new SamplingDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Long countMeasuringPrograms(DbQuery query) throws DataAccessException {
        Session session = sessionStore.getSession();
        try {
            return getCount(new MeasuringProgramDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Long countStations() throws DataAccessException {
        Session session = sessionStore.getSession();
        try {
            DbQuery query = createBackwardsCompatibleQuery();
            return countFeatures(query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    @Deprecated
    public Long countTimeseries() throws DataAccessException {
        Session session = sessionStore.getSession();
        try {
            DbQuery query = createBackwardsCompatibleQuery();
            return countDatasets(query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Long countTimeseries(DbQuery query) throws DataAccessException {
        return countDataset(query, "timeseries");
    }

    public Long countIndividualObservations(DbQuery query) throws DataAccessException {
        return countDataset(query, "individualObservation");
    }

    public Long countTrajectories(DbQuery query) throws DataAccessException {
        return countDataset(query, "trajectory");
    }

    public Long countProfiles(DbQuery query) throws DataAccessException {
        return countDataset(query, "profile");
    }

    private Long countDataset(DbQuery query, String datasetType) throws DataAccessException {
        Session session = sessionStore.getSession();
        try {
            IoParameters parameters = query.getParameters();
            parameters = parameters.replaceWith("datasetTypes", datasetType);
            return getCount(new DatasetDao<>(session, DatasetEntity.class), dbQueryFactory.createFrom(parameters));
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Long getCount(AbstractDao<?> dao, DbQuery query) throws DataAccessException {
        return dao.getCount(query);
    }

    private DbQuery createBackwardsCompatibleQuery() {
        IoParameters parameters = IoParameters.createDefaults();
        // parameters = parameters.extendWith(Parameters.FILTER_PLATFORM_TYPES,
        // "stationary", "insitu")
        // .extendWith(Parameters.FILTER_VALUE_TYPES, ValueType.DEFAULT_VALUE_TYPE);
        return dbQueryFactory.createFrom(parameters);
    }

}
