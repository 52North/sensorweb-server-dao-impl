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
package org.n52.series.db.old.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.locationtech.jts.geom.Geometry;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.StationOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.sensorweb.server.srv.OutputAssembler;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.FeatureDao;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.spi.search.StationSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 * @deprecated sdffasn
 */
// @Component
@Deprecated
public class StationAssembler extends SessionAwareAssembler
        implements OutputAssembler<StationOutput>, SearchableAssembler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StationAssembler.class);

    public StationAssembler(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    private FeatureDao createDao(Session session) {
        return new FeatureDao(session);
    }

    @Override
    public boolean exists(String id, DbQuery parameters) {
        Session session = getSession();
        try {
            FeatureDao dao = createDao(session);
            return dao.hasInstance(parseId(id), parameters);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public Collection<SearchResult> searchFor(DbQuery query) {
        Session session = getSession();
        try {
            FeatureDao stationDao = createDao(session);
            List<FeatureEntity> found = stationDao.find(query);
            return convertToSearchResults(found, query);
        } finally {
            returnSession(session);
        }
    }

    private List<SearchResult> convertToSearchResults(List<? extends DescribableEntity> found, DbQuery query) {
        String locale = query.getLocaleForLabel();
        List<SearchResult> results = new ArrayList<>();
        for (DescribableEntity searchResult : found) {
            String pkid = Long.toString(searchResult.getId());
            String label = searchResult.getLabelFrom(locale);
            results.add(new StationSearchResult().setId(pkid).setLabel(label));
        }
        return results;
    }

    @Override
    public List<StationOutput> getAllCondensed(DbQuery parameters) {
        Session session = getSession();
        try {
            return getAllCondensed(parameters, session);
        } finally {
            returnSession(session);
        }
    }

    private List<StationOutput> getAllCondensed(DbQuery parameters, Session session) {
        List<FeatureEntity> allFeatures = getAllInstances(parameters, session);
        List<StationOutput> results = new ArrayList<>();
        for (FeatureEntity featureEntity : allFeatures) {
            results.add(createCondensed(featureEntity, parameters));
        }
        return results;
    }

    @Override
    public List<StationOutput> getAllExpanded(DbQuery parameters) {
        Session session = getSession();
        try {
            return getAllExpanded(parameters, session);
        } finally {
            returnSession(session);
        }
    }

    private List<StationOutput> getAllExpanded(DbQuery parameters, Session session) {
        List<FeatureEntity> allFeatures = getAllInstances(parameters, session);

        List<StationOutput> results = new ArrayList<>();
        for (FeatureEntity featureEntity : allFeatures) {
            results.add(createExpanded(featureEntity, parameters, session));
        }
        return results;
    }

    private List<FeatureEntity> getAllInstances(DbQuery query, Session session) {
        FeatureDao featureDao = createDao(session);
        return featureDao.getAllInstances(query);
    }

    @Override
    public StationOutput getInstance(String id, DbQuery parameters) {
        Session session = getSession();
        try {
            return getInstance(id, parameters, session);
        } finally {
            returnSession(session);
        }
    }

    private StationOutput getInstance(String id, DbQuery parameters, Session session) {
        FeatureEntity result = getFeatureEntity(id, parameters, session);
        if (result == null) {
            LOGGER.debug("Resource with id '" + id + "' could not be found.");
            return null;
        }
        return createExpanded(result, parameters, session);
    }

    private FeatureEntity getFeatureEntity(String id, DbQuery query, Session session) {
        return createDao(session).getInstance(parseId(id), query);
    }

    public StationOutput getCondensedInstance(String id, DbQuery parameters, Session session) {
        FeatureDao featureDao = createDao(session);
        FeatureEntity result = featureDao.getInstance(parseId(id), getDbQuery(IoParameters.createDefaults()));
        return createCondensed(result, parameters);
    }

    private StationOutput createExpanded(FeatureEntity feature, DbQuery query, Session session) {
        StationOutput result = createCondensed(feature, query);

        // Class<DatasetEntity> clazz = DatasetEntity.class;
        // DatasetDao<DatasetEntity> seriesDao = new DatasetDao<>(session, clazz);
        // List<DatasetEntity> series = seriesDao.getInstancesWith(feature, query);

        // Map<String, DatasetParameters> timeseriesList = createTimeseriesList(series, query);
        // result.setValue(StationOutput.PROPERTIES, timeseriesList, query.getParameters(), result
        // ::setTimeseries);

        return result;
    }

    private StationOutput createCondensed(FeatureEntity entity, DbQuery query) {
        StationOutput result = new StationOutput();
        IoParameters parameters = query.getParameters();

        String id = Long.toString(entity.getId());
        String label = entity.getLabelFrom(query.getLocaleForLabel());
        Geometry geometry = getGeometry(entity, query);
        result.setId(id);
        result.setValue(StationOutput.PROPERTIES, label, parameters, result::setLabel);
        result.setValue(StationOutput.GEOMETRY, geometry, parameters, result::setGeometry);
        return result;
    }

    private Geometry getGeometry(FeatureEntity featureEntity, DbQuery query) {
        return featureEntity.isSetGeometry() ? getGeometry(featureEntity.getGeometryEntity(), query) : null;
    }

}
