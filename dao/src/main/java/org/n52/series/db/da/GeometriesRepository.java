/*
 * Copyright (C) 2015-2018 52°North Initiative for Geospatial Open Source
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
import org.n52.io.response.GeometryOutput;
import org.n52.io.response.GeometryType;
import org.n52.io.response.PlatformOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataModelUtil;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.FeatureDao;
import org.n52.series.db.dao.SamplingGeometryDao;
import org.n52.series.spi.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class GeometriesRepository extends SessionAwareRepository implements OutputAssembler<GeometryOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeometriesRepository.class);

    private static final String NAMED_QUERY_GET_SAMPLING_GEOMETRIES_FOR_FEATURE = "getSampleLatLonGeometries";

    private static final String NAMED_QUERY_PARAMETER_FEATURE_ID = "featureid";

    @Autowired
    private PlatformRepository platformRepository;

    private FeatureDao createFeatureDao(Session session) {
        return new FeatureDao(session);
    }

    @Override
    public boolean exists(String id, DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            if (GeometryType.isPlatformGeometryId(id)) {
                String dbId = GeometryType.extractId(id);
                final FeatureDao dao = createFeatureDao(session);
                // XXX must be FALSE if 'site/2' matches an id of a feature from a mobile platform
                return dao.hasInstance(parseId(dbId), parameters);
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
    public List<GeometryOutput> getAllCondensed(DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            return getAllCondensed(parameters, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<GeometryOutput> getAllCondensed(DbQuery parameters, Session session) throws DataAccessException {
        return getAllInstances(parameters, session, false);
    }

    @Override
    public List<GeometryOutput> getAllExpanded(DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            return getAllExpanded(parameters, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<GeometryOutput> getAllExpanded(DbQuery parameters, Session session) throws DataAccessException {
        return getAllInstances(parameters, session, true);
    }

    @Override
    public GeometryOutput getInstance(String id, DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            return getInstance(id, parameters, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public GeometryOutput getInstance(String id, DbQuery parameters, Session session) throws DataAccessException {
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

    private List<GeometryOutput> getAllInstances(DbQuery query, Session session, boolean expanded)
            throws DataAccessException {
        List<GeometryOutput> geometries = new ArrayList<>();
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

    private GeometryOutput getPlatformLocationGeometry(String id, DbQuery parameters, Session session)
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

    private List<GeometryOutput> getAllSites(DbQuery query, Session session, boolean expanded)
            throws DataAccessException {
        IoParameters parameters = query.getParameters();
        List<GeometryOutput> GeometryOutputList = new ArrayList<>();
        DbQuery siteQuery = getDbQuery(parameters.replaceWith(Parameters.FILTER_PLATFORM_TYPES, "stationary"));

        FeatureDao dao = createFeatureDao(session);
        for (FeatureEntity featureEntity : dao.getAllInstances(siteQuery)) {
            GeometryOutput GeometryOutput = createSite(featureEntity, query, expanded);
            if (GeometryOutput != null) {
                GeometryOutputList.add(GeometryOutput);
            }
        }
        return GeometryOutputList;
    }

    private GeometryOutput createSite(FeatureEntity entity, DbQuery query, boolean expanded)
            throws DataAccessException {
        GeometryOutput GeometryOutput = createGeometryOutput(GeometryType.PLATFORM_SITE, entity, query);
        return expanded
                ? addGeometry(GeometryOutput, entity, query)
                : GeometryOutput;
    }

    private Collection<GeometryOutput> getAllTracks(DbQuery query, Session session, boolean expanded)
            throws DataAccessException {
        List<GeometryOutput> GeometryOutputList = new ArrayList<>();
        FeatureDao featureDao = createFeatureDao(session);
        DbQuery mobileQuery = query.replaceWith(Parameters.FILTER_PLATFORM_TYPES, "mobile");
        if (isFilterViaSamplingGeometries()) {
            // filter via sampling_geometry will hit performance!
            // if possible, keep a LINESTRING geometry updated in feature-Table for each trackv
            DbQuery trackQuery = mobileQuery.removeSpatialFilter();
            for (FeatureEntity featureEntity : featureDao.getAllInstances(trackQuery)) {
                GeometryOutput track = createTrack(featureEntity, trackQuery, expanded, session);
                Geometry spatialFilter = query.getSpatialFilter();
                if (spatialFilter == null || spatialFilter.intersects(track.getGeometry())) {
                    GeometryOutputList.add(track);
                }
            }
        } else {
            for (FeatureEntity featureEntity : featureDao.getAllInstances(mobileQuery)) {
                GeometryOutputList.add(createTrack(featureEntity, query, expanded, session));
            }
        }
        return GeometryOutputList;
    }

    private boolean isFilterViaSamplingGeometries() {
        // TODO update test data to support LINESTRING tracks in feature-Table
        // TODO apply config switch via HibernateSessionHolderImpl
        return true;
    }

    private GeometryOutput createTrack(FeatureEntity entity, DbQuery query, boolean expanded, Session session)
            throws DataAccessException {
        GeometryOutput GeometryOutput = createGeometryOutput(GeometryType.PLATFORM_TRACK, entity, query);
        if (expanded) {
            if (entity.isSetGeometry()) {
                // track available from feature table
                return addGeometry(GeometryOutput, entity, query);
            } else {
                IoParameters parameters = query.getParameters();
                Geometry lineString = createTrajectory(entity, query, session);
                GeometryOutput.setValue(GeometryOutput.GEOMETRY, lineString, parameters, GeometryOutput::setGeometry);
                return GeometryOutput;
            }
        }
        return GeometryOutput;
    }

    private GeometryOutput addGeometry(GeometryOutput GeometryOutput, FeatureEntity entity, DbQuery query) {
        IoParameters parameters = query.getParameters();
        Geometry geometry = getGeometry(entity.getGeometryEntity(), query);
        GeometryOutput.setValue(GeometryOutput.GEOMETRY, geometry, parameters, GeometryOutput::setGeometry);
        return GeometryOutput;
    }

    private Geometry createTrajectory(FeatureEntity featureEntity, DbQuery dbQuery, Session session) {
        String srid = dbQuery.getDatabaseSridCode();
        // track available as points from observation table
        final List<Coordinate> coordinates = new ArrayList<>();
        if (DataModelUtil.isNamedQuerySupported(NAMED_QUERY_GET_SAMPLING_GEOMETRIES_FOR_FEATURE, session)) {
            Query query = session.getNamedQuery(NAMED_QUERY_GET_SAMPLING_GEOMETRIES_FOR_FEATURE);
            query.setLong(NAMED_QUERY_PARAMETER_FEATURE_ID, featureEntity.getId());
            for (Object entity : query.list()) {
                Object[] row = (Object[]) entity;
                // phenomenonTime is needed for ordering only
                // Date phenomenonTime = (Date) row[0];
                if (row.length > 2) {
                    coordinates.add(new Coordinate((double) row[1], (double) row[2]));
                } else {
                    Geometry geom = (Geometry) row[1];
                    coordinates.add(geom.getCoordinate());
                }
            }
            Coordinate[] points = coordinates.toArray(new Coordinate[0]);
            return getCrsUtils().createLineString(points, srid);
        } else {
            // when named query not configured --> may be a performance issue
            final SamplingGeometryDao dao = new SamplingGeometryDao(session);
            IoParameters parameters = dbQuery.getParameters()
                                             .extendWith(Parameters.FEATURES, Long.toString(featureEntity.getId()));
            List<GeometryEntity> samplingGeometries = dao.getGeometriesOrderedByTimestamp(getDbQuery(parameters));
            return createLineString(samplingGeometries, dbQuery);
        }
    }

    private Geometry createLineString(List<GeometryEntity> samplingGeometries, DbQuery query) {
        List<Coordinate> coordinates = new ArrayList<>();
        for (GeometryEntity geometryEntity : samplingGeometries) {
            Point geometry = (Point) getGeometry(geometryEntity, query);
            coordinates.add(geometry.getCoordinate());
        }

        Coordinate[] points = coordinates.toArray(new Coordinate[0]);
        return getCrsUtils().createLineString(points, query.getDatabaseSridCode());
    }

    private Collection<GeometryOutput> getAllObservedGeometriesStatic(DbQuery parameters,
                                                                    Session session,
                                                                    boolean expanded) {
        LOGGER.warn("Static ObservedGeometries not yet supported!");
        // TODO implement
        return new ArrayList<>();
    }

    private Collection<GeometryOutput> getAllObservedGeometriesDynamic(DbQuery parameters,
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

    private GeometryOutput createGeometryOutput(GeometryType type, FeatureEntity featureEntity, DbQuery query)
            throws DataAccessException {
        GeometryOutput GeometryOutput = new GeometryOutput();
        IoParameters parameters = query.getParameters();
        String hrefBase = query.getHrefBase();
        PlatformOutput platform = getPlatfom(featureEntity, query);

        GeometryOutput.setId(Long.toString(featureEntity.getId()));
        GeometryOutput.setValue(GeometryOutput.PROPERTIES, type, parameters, GeometryOutput::setGeometryType);
        GeometryOutput.setValue(GeometryOutput.PROPERTIES, hrefBase, parameters, GeometryOutput::setHrefBase);
        GeometryOutput.setValue(GeometryOutput.PROPERTIES, platform, parameters, GeometryOutput::setPlatform);
        return GeometryOutput;
    }

    private PlatformOutput getPlatfom(FeatureEntity entity, DbQuery parameters) throws DataAccessException {
        DbQuery platformQuery = getDbQuery(parameters.getParameters()
                                                     .extendWith(Parameters.FEATURES, String.valueOf(entity.getId()))
                                                     .extendWith(Parameters.FILTER_PLATFORM_TYPES, "all")
                                                     .removeAllOf(Parameters.FILTER_FIELDS));
        List<PlatformOutput> platforms = platformRepository.getAllCondensed(platformQuery);
        if (platforms.size() != 1) {
            LOGGER.warn("expected unique platform (but was: #{}) for feature {}", platforms.size(), entity.getId());
        }
        return platforms.iterator()
                        .next();
    }

}
