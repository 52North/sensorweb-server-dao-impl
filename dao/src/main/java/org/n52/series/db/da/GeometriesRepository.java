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
import java.util.Collections;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.n52.io.request.FilterResolver;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.response.GeometryInfo;
import org.n52.io.response.GeometryType;
import org.n52.io.response.PlatformOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataModelUtil;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.FeatureDao;
import org.n52.series.spi.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class GeometriesRepository extends SessionAwareRepository implements OutputAssembler<GeometryInfo> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeometriesRepository.class);

    private static final String NAMED_QUERY_GET_SAMPLING_GEOMETRIES_FOR_FEATURE = "getSampleLatLonGeometries";

    private static final String NAMED_QUERY_PARAMETER_FEATURE_ID = "featureid";

    @Autowired
    private PlatformRepository platformRepository;

    private FeatureDao createFeatureDao(Session session) {
        return new FeatureDao(getDbQueryFactory(), session);
    }

    @Override
    public boolean exists(String id, DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            if (GeometryType.isPlatformGeometryId(id)) {
                String dbId = GeometryType.extractId(id);
                final FeatureDao dao = createFeatureDao(session);
                // XXX must be FALSE if 'site/2' matches an id of a feature from a mobile platform
                return dao.hasInstance(parseId(dbId), parameters, FeatureEntity.class);
            } else if (GeometryType.isObservedGeometryId(id)) {
                LOGGER.warn("ObservedGeometries not fully supported right now!");
                // id = GeometryType.extractId(id);
                // TODO class of observed geometries
                // return new FeatureDao(session).hasInstance(parseId(id), clazz);
            }

            return false;
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<GeometryInfo> getAllCondensed(DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            return getAllCondensed(parameters, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<GeometryInfo> getAllCondensed(DbQuery parameters, Session session) throws DataAccessException {
        return getAllInstances(parameters, session, false);
    }

    @Override
    public List<GeometryInfo> getAllExpanded(DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            return getAllExpanded(parameters, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<GeometryInfo> getAllExpanded(DbQuery parameters, Session session) throws DataAccessException {
        return getAllInstances(parameters, session, true);
    }

    @Override
    public GeometryInfo getInstance(String id, DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            return getInstance(id, parameters, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public GeometryInfo getInstance(String id, DbQuery parameters, Session session) throws DataAccessException {
        parameters.setDatabaseAuthorityCode(getDatabaseSrid());
        if (GeometryType.isPlatformGeometryId(id)) {
            return getPlatformLocationGeometry(id, parameters, session);
        } else {
            // TODO observed Geometry tpyes
            return null;
        }
    }

    @Override
    public Collection<SearchResult> searchFor(IoParameters parameters) {
        return Collections.emptyList();
    }

    @Override
    public List<SearchResult> convertToSearchResults(List< ? extends DescribableEntity> found, DbQuery query) {
        return Collections.emptyList();
    }

    private List<GeometryInfo> getAllInstances(DbQuery query, Session session, boolean expanded)
            throws DataAccessException {
        List<GeometryInfo> geometries = new ArrayList<>();
        query.setDatabaseAuthorityCode(getDatabaseSrid());
        final FilterResolver filterResolver = query.getFilterResolver();
        if (filterResolver.shallIncludeInsituPlatformTypes()) {
            if (filterResolver.shallIncludePlatformGeometriesSite()) {
                geometries.addAll(getAllSites(query, session, expanded));
            }
            if (filterResolver.shallIncludePlatformGeometriesTrack()) {
                geometries.addAll(getAllTracks(query, session, expanded));
            }
        }
        if (filterResolver.shallIncludeRemotePlatformTypes()) {
            if (filterResolver.shallIncludeObservedGeometriesStatic()) {
                geometries.addAll(getAllObservedGeometriesStatic(query, session, expanded));
            }
            if (filterResolver.shallIncludeObservedGeometriesDynamic()) {
                geometries.addAll(getAllObservedGeometriesDynamic(query, session, expanded));
            }
        }
        return geometries;
    }

    private GeometryInfo getPlatformLocationGeometry(String id, DbQuery parameters, Session session)
            throws DataAccessException {
        String geometryId = GeometryType.extractId(id);
        FeatureEntity featureEntity = getFeatureEntity(geometryId, parameters, session);
        if (featureEntity != null) {
            if (GeometryType.isSiteId(id)) {
                return createSite(featureEntity, parameters, true);
            } else if (GeometryType.isTrackId(id)) {
                return createTrack(featureEntity, parameters, true, session);
            }
        }
        return null;
    }

    private FeatureEntity getFeatureEntity(String id, DbQuery parameters, Session session) throws DataAccessException {
        FeatureDao dao = createFeatureDao(session);
        long geometryId = Long.parseLong(GeometryType.extractId(id));
        return dao.getInstance(geometryId, parameters);
    }

    private List<GeometryInfo> getAllSites(DbQuery parameters, Session session, boolean expanded)
            throws DataAccessException {
        List<GeometryInfo> geometryInfoList = new ArrayList<>();
        FeatureDao dao = createFeatureDao(session);
        DbQuery siteQuery = dbQueryFactory.createFrom(parameters.getParameters()
                                                                .removeAllOf(Parameters.FILTER_PLATFORM_TYPES)
                                                                .extendWith(Parameters.FILTER_PLATFORM_TYPES,
                                                                            "stationary"));
        for (FeatureEntity featureEntity : dao.getAllInstances(siteQuery)) {
            GeometryInfo geometryInfo = createSite(featureEntity, parameters, expanded);
            if (geometryInfo != null) {
                geometryInfoList.add(geometryInfo);
            }
        }
        return geometryInfoList;
    }

    private GeometryInfo createSite(FeatureEntity featureEntity, DbQuery parameters, boolean expanded)
            throws DataAccessException {
        final GeometryInfo geomInfo = new GeometryInfo(GeometryType.PLATFORM_SITE);
        GeometryInfo geometryInfo = addCondensedValues(geomInfo, featureEntity, parameters);
        if (expanded) {
            Geometry geometry = featureEntity.getGeometry(getDatabaseSrid());
            if (geometry != null) {
                geometryInfo.setGeometry(geometry);
            }
        }
        return geometryInfo;
    }

    private Collection<GeometryInfo> getAllTracks(DbQuery parameters, Session session, boolean expanded)
            throws DataAccessException {
        List<GeometryInfo> geometryInfoList = new ArrayList<>();
        FeatureDao featureDao = createFeatureDao(session);
        DbQuery trackQuery = dbQueryFactory.createFrom(parameters.getParameters()
                                                                 .removeAllOf(Parameters.FILTER_PLATFORM_TYPES)
                                                                 .extendWith(Parameters.FILTER_PLATFORM_TYPES,
                                                                             "mobile"));
        for (FeatureEntity featureEntity : featureDao.getAllInstances(trackQuery)) {
            geometryInfoList.add(createTrack(featureEntity, parameters, expanded, session));
        }
        return geometryInfoList;
    }

    private GeometryInfo createTrack(FeatureEntity featureEntity, DbQuery parameters, boolean expanded, Session session)
            throws DataAccessException {
        final GeometryInfo geomInfo = new GeometryInfo(GeometryType.PLATFORM_TRACK);
        GeometryInfo geometryInfo = addCondensedValues(geomInfo, featureEntity, parameters);
        if (expanded) {
            if (featureEntity.isSetGeometry()) {
                // track available from feature table
                geometryInfo.setGeometry(featureEntity.getGeometry(getDatabaseSrid()));
                return geometryInfo;
            } else {
                Geometry lineString = createTrajectory(featureEntity, session);
                geometryInfo.setGeometry(lineString);
                return geometryInfo;
            }
        }
        return geometryInfo;
    }

    private Geometry createTrajectory(FeatureEntity featureEntity, Session session) {
        try {
            // track available as points from observation table
            final List<Coordinate> coordinates = new ArrayList<>();
            if (DataModelUtil.isNamedQuerySupported(NAMED_QUERY_GET_SAMPLING_GEOMETRIES_FOR_FEATURE, session)) {
                Query query = session.getNamedQuery(NAMED_QUERY_GET_SAMPLING_GEOMETRIES_FOR_FEATURE);
                query.setLong(NAMED_QUERY_PARAMETER_FEATURE_ID, featureEntity.getPkid());
                for (Object entity : query.list()) {
                    Object[] row = (Object[]) entity;
                    // time is needed for ordering only
                    // Date phenomenonTime = (Date) row[0];
                    if (row.length > 2) {
                        coordinates.add(new Coordinate((double) row[1], (double) row[2]));
                    } else {
                        Geometry geom = (Geometry) row[1];
                        coordinates.add(geom.getCoordinate());
                    }
                }
            } else {
                LOGGER.debug("Could not create trajectory! Named query '{}' not defined",
                             NAMED_QUERY_GET_SAMPLING_GEOMETRIES_FOR_FEATURE);
            }

            Coordinate[] points = coordinates.toArray(new Coordinate[0]);
            return getCrsUtils().createLineString(points, getDatabaseSrid());
        } catch (org.springframework.dao.DataAccessException e) {
            LOGGER.error("Could not get trajectory for feature '{}'", featureEntity.getPkid());
            return null;
        }
    }

    private Collection<GeometryInfo> getAllObservedGeometriesStatic(DbQuery parameters,
                                                                    Session session,
                                                                    boolean expanded) {
        LOGGER.warn("Static ObservedGeometries not yet supported!");
        // TODO implement
        return new ArrayList<>();
    }

    private Collection<GeometryInfo> getAllObservedGeometriesDynamic(DbQuery parameters,
                                                                     Session session,
                                                                     boolean expanded) {
        LOGGER.warn("Dynamic ObservedGeometries not yet supported!");
        // TODO implement
        return new ArrayList<>();
    }

    private List<GeometryEntity> getAllObservedGeometries(DbQuery parameters, Session session) {
        LOGGER.warn("ObservedGeometries not yet supported!");
        // TODO Auto-generated method stub
        return null;
    }

    private GeometryInfo addCondensedValues(GeometryInfo geometryInfo, FeatureEntity featureEntity, DbQuery parameters)
            throws DataAccessException {
        geometryInfo.setId(Long.toString(featureEntity.getPkid()));
        geometryInfo.setHrefBase(urlHelper.getGeometriesHrefBaseUrl(parameters.getHrefBase()));
        geometryInfo.setPlatform(getPlatfom(featureEntity, parameters));
        return geometryInfo;
    }

    private PlatformOutput getPlatfom(FeatureEntity entity, DbQuery parameters) throws DataAccessException {
        DbQuery platformQuery = dbQueryFactory.createFrom(parameters.getParameters()
                                                                    .extendWith(Parameters.FEATURES,
                                                                                String.valueOf(entity.getPkid()))
                                                                    .extendWith(Parameters.FILTER_PLATFORM_TYPES,
                                                                                "all"));

        List<PlatformOutput> platforms = platformRepository.getAllCondensed(platformQuery);
        return platforms.iterator()
                        .next();
    }

}
