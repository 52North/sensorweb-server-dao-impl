/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
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

package org.n52.series.db.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.StationOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.dao.DatasetDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.FeatureDao;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.spi.search.StationSearchResult;
import org.n52.web.exception.BadRequestException;
import org.n52.web.exception.ResourceNotFoundException;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 * @deprecated since 2.0.0.
 */
@Deprecated
public class StationRepository extends SessionAwareRepository
        implements OutputAssembler<StationOutput>, SearchableRepository {

    private FeatureDao createDao(Session session) {
        return new FeatureDao(session);
    }

    @Override
    public boolean exists(String id, DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            FeatureDao dao = createDao(session);
            return dao.hasInstance(parseId(id), parameters);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public Collection<SearchResult> searchFor(IoParameters parameters) {
        Session session = getSession();
        try {
            FeatureDao stationDao = createDao(session);
            DbQuery query = addPointLocationOnlyRestriction(getDbQuery(parameters));
            List<FeatureEntity> found = stationDao.find(query);
            return convertToSearchResults(found, query);
        } finally {
            returnSession(session);
        }
    }

    private List<SearchResult> convertToSearchResults(List< ? extends DescribableEntity> found, DbQuery query) {
        String locale = query.getLocale();
        List<SearchResult> results = new ArrayList<>();
        for (DescribableEntity searchResult : found) {
            String pkid = Long.toString(searchResult.getId());
            String label = searchResult.getLabelFrom(locale);
            results.add(new StationSearchResult(pkid, label));
        }
        return results;
    }

    @Override
    public List<StationOutput> getAllCondensed(DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            return getAllCondensed(parameters, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<StationOutput> getAllCondensed(DbQuery parameters, Session session) throws DataAccessException {
        List<FeatureEntity> allFeatures = getAllInstances(parameters, session);
        List<StationOutput> results = new ArrayList<>();
        for (FeatureEntity featureEntity : allFeatures) {
            results.add(createCondensed(featureEntity, parameters));
        }
        return results;
    }

    @Override
    public List<StationOutput> getAllExpanded(DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            return getAllExpanded(parameters, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<StationOutput> getAllExpanded(DbQuery parameters, Session session) throws DataAccessException {
        List<FeatureEntity> allFeatures = getAllInstances(parameters, session);

        List<StationOutput> results = new ArrayList<>();
        for (FeatureEntity featureEntity : allFeatures) {
            results.add(createExpanded(featureEntity, parameters, session));
        }
        return results;
    }

    private List<FeatureEntity> getAllInstances(DbQuery parameters, Session session) throws DataAccessException {
        FeatureDao featureDao = createDao(session);
        return featureDao.getAllInstances(addPointLocationOnlyRestriction(parameters));
    }

    @Override
    public StationOutput getInstance(String id, DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            return getInstance(id, parameters, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public StationOutput getInstance(String id, DbQuery parameters, Session session) throws DataAccessException {
        FeatureEntity result = getFeatureEntity(id, parameters, session);
        if (result == null) {
            throw new ResourceNotFoundException("Resource with id '" + id + "' could not be found.");
        }
        return createExpanded(result, parameters, session);
    }

    private FeatureEntity getFeatureEntity(String id, DbQuery parameters, Session session)
            throws DataAccessException, BadRequestException {
        DbQuery query = addPointLocationOnlyRestriction(parameters);
        return createDao(session).getInstance(parseId(id), query);
    }

    public StationOutput getCondensedInstance(String id, DbQuery parameters, Session session)
            throws DataAccessException {
        FeatureDao featureDao = createDao(session);
        FeatureEntity result = featureDao.getInstance(parseId(id), getDbQuery(IoParameters.createDefaults()));
        return createCondensed(result, parameters);
    }

    private StationOutput createExpanded(FeatureEntity feature, DbQuery query, Session session)
            throws DataAccessException {
        IoParameters parameters = query.getParameters();
        StationOutput result = createCondensed(feature, query);

        Class<QuantityDatasetEntity> clazz = QuantityDatasetEntity.class;
        DatasetDao<QuantityDatasetEntity> seriesDao = new DatasetDao<>(session, clazz);
        List<QuantityDatasetEntity> series = seriesDao.getInstancesWith(feature, query);

        Map<String, DatasetParameters> timeseriesList = createTimeseriesList(series, query);
        result.setValue(StationOutput.TIMESERIES, timeseriesList, parameters, result ::setTimeseries);

        return result;
    }

    private StationOutput createCondensed(FeatureEntity entity, DbQuery query) {
        StationOutput result = new StationOutput();
        IoParameters parameters = query.getParameters();

        String id = Long.toString(entity.getId());
        String label = entity.getLabelFrom(query.getLocale());
        Geometry geometry = createPoint(entity, query);
        result.setId(id);
        result.setValue(StationOutput.LABEL, label, parameters, result::setLabel);
        result.setValue(StationOutput.GEOMETRY, geometry, parameters, result::setGeometry);
        return result;
    }

    private Geometry createPoint(FeatureEntity featureEntity, DbQuery query) {
        return featureEntity.isSetGeometry()
                ? getGeometry(featureEntity.getGeometryEntity(), query)
                : null;
    }

    private DbQuery addPointLocationOnlyRestriction(DbQuery query) {
        return dbQueryFactory.createFrom(query.getParameters()
                                              .extendWith("geometryTypes", "Point"));
    }

}
