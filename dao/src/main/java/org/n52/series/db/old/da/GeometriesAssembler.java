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
package org.n52.series.db.old.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.response.GeometryOutput;
import org.n52.io.response.GeometryType;
import org.n52.io.response.PlatformOutput;
import org.n52.sensorweb.server.db.old.DataModelUtil;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.sensorweb.server.srv.OutputAssembler;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.FeatureDao;
import org.n52.series.db.old.dao.SamplingGeometryDao;
import org.n52.series.spi.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GeometriesAssembler extends SessionAwareAssembler implements OutputAssembler<GeometryOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeometriesAssembler.class);

    private static final String NAMED_QUERY_GET_SAMPLING_GEOMETRIES_FOR_FEATURE = "getSampleLatLonGeometries";

    private static final String NAMED_QUERY_PARAMETER_FEATURE_ID = "featureid";

    private final org.n52.sensorweb.server.db.assembler.core.PlatformAssembler platformRepository;

    public GeometriesAssembler(org.n52.sensorweb.server.db.assembler.core.PlatformAssembler platformRepository,
            HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
        this.platformRepository = platformRepository;
    }

    private FeatureDao createFeatureDao(Session session) {
        return new FeatureDao(session);
    }

    @Override
    public boolean exists(String id, DbQuery parameters) {
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
    public List<GeometryOutput> getAllCondensed(DbQuery parameters) {
        Session session = getSession();
        try {
            return getAllCondensed(parameters, session);
        } finally {
            returnSession(session);
        }
    }

    private List<GeometryOutput> getAllCondensed(DbQuery parameters, Session session) {
        return getAllInstances(parameters, session, false);
    }

    @Override
    public List<GeometryOutput> getAllExpanded(DbQuery parameters) {
        Session session = getSession();
        try {
            return getAllExpanded(parameters, session);
        } finally {
            returnSession(session);
        }
    }

    private List<GeometryOutput> getAllExpanded(DbQuery parameters, Session session) {
        return getAllInstances(parameters, session, true);
    }

    @Override
    public GeometryOutput getInstance(String id, DbQuery parameters) {
        Session session = getSession();
        try {
            return getInstance(id, parameters, session);
        } finally {
            returnSession(session);
        }
    }

    private GeometryOutput getInstance(String id, DbQuery parameters, Session session) {
        if (GeometryType.isPlatformGeometryId(id)) {
            return getPlatformLocationGeometry(id, parameters, session);
        } else {
            // TODO observed Geometry tpyes
            return null;
        }
    }

    @Override
    public Collection<SearchResult> searchFor(DbQuery parameters) {
        return Collections.emptyList();
    }

    private List<GeometryOutput> getAllInstances(DbQuery query, Session session, boolean expanded) {
        List<GeometryOutput> geometries = new ArrayList<>();
        // final FilterResolver filterResolver = query.getFilterResolver();
        // if (filterResolver.shallIncludeInsituDatasets()) {
        // if (filterResolver.shallIncludePlatformGeometriesSite()) {
        // geometries.addAll(getAllSites(query, session, expanded));
        // }
        // if (filterResolver.shallIncludePlatformGeometriesTrack()) {
        // geometries.addAll(getAllTracks(query, session, expanded));
        // }
        // }
        // if (filterResolver.shallIncludeRemoteDatasets()) {
        // if (filterResolver.shallIncludeObservedGeometriesStatic()) {
        // geometries.addAll(getAllObservedGeometriesStatic(query, session,
        // expanded));
        // }
        // if (filterResolver.shallIncludeObservedGeometriesDynamic()) {
        // geometries.addAll(getAllObservedGeometriesDynamic(query, session,
        // expanded));
        // }
        // }
        return geometries;
    }

    private GeometryOutput getPlatformLocationGeometry(String id, DbQuery parameters, Session session) {
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

    private FeatureEntity getFeatureEntity(String id, DbQuery parameters, Session session) {
        FeatureDao dao = createFeatureDao(session);
        long geometryId = Long.parseLong(GeometryType.extractId(id));
        return dao.getInstance(geometryId, parameters);
    }

    private List<GeometryOutput> getAllSites(DbQuery query, Session session, boolean expanded) {
        List<GeometryOutput> geometryInfoList = new ArrayList<>();
        FeatureDao dao = createFeatureDao(session);
        DbQuery siteQuery =
                dbQueryFactory.createFrom(query.getParameters().replaceWith(Parameters.FILTER_MOBILE, "false"));
        for (FeatureEntity featureEntity : dao.getAllInstances(siteQuery)) {
            GeometryOutput geometryInfo = createSite(featureEntity, query, expanded);
            if (geometryInfo != null) {
                geometryInfoList.add(geometryInfo);
            }
        }
        return geometryInfoList;
    }

    private GeometryOutput createSite(FeatureEntity entity, DbQuery query, boolean expanded) {
        GeometryOutput geometryInfo = createGeometryInfo(GeometryType.PLATFORM_SITE, entity, query);
        return expanded ? addGeometry(geometryInfo, entity, query) : geometryInfo;
    }

    private Collection<GeometryOutput> getAllTracks(DbQuery query, Session session, boolean expanded) {
        List<GeometryOutput> geometryInfoList = new ArrayList<>();
        FeatureDao featureDao = createFeatureDao(session);
        DbQuery trackQuery =
                dbQueryFactory.createFrom(query.getParameters().replaceWith(Parameters.FILTER_MOBILE, "true"));
        for (FeatureEntity featureEntity : featureDao.getAllInstances(trackQuery)) {
            geometryInfoList.add(createTrack(featureEntity, query, expanded, session));
        }
        return geometryInfoList;
    }

    private GeometryOutput createTrack(FeatureEntity entity, DbQuery query, boolean expanded, Session session) {
        GeometryOutput geometryInfo = createGeometryInfo(GeometryType.PLATFORM_TRACK, entity, query);
        if (expanded) {
            if (entity.isSetGeometry()) {
                // track available from feature table
                return addGeometry(geometryInfo, entity, query);
            } else {
                IoParameters parameters = query.getParameters();
                Geometry lineString = createTrajectory(entity, query, session);
                geometryInfo.setValue(GeometryOutput.GEOMETRY, lineString, parameters, geometryInfo::setGeometry);
                return geometryInfo;
            }
        }
        return geometryInfo;
    }

    private GeometryOutput addGeometry(GeometryOutput geometryInfo, FeatureEntity entity, DbQuery query) {
        IoParameters parameters = query.getParameters();
        Geometry geometry = getGeometry(entity.getGeometryEntity(), query);
        geometryInfo.setValue(GeometryOutput.GEOMETRY, geometry, parameters, geometryInfo::setGeometry);
        return geometryInfo;
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
            // when named query not configured --> bad performance
            final SamplingGeometryDao dao = new SamplingGeometryDao(session);
            IoParameters parameters =
                    dbQuery.getParameters().extendWith(Parameters.FEATURES, Long.toString(featureEntity.getId()));
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
        return getCrsUtils().createLineString(coordinates.toArray(new Coordinate[0]), query.getDatabaseSridCode());
    }

    private Collection<GeometryOutput> getAllObservedGeometriesStatic(DbQuery parameters, Session session,
            boolean expanded) {
        LOGGER.warn("Static ObservedGeometries not yet supported!");
        // TODO implement
        return new ArrayList<>();
    }

    private Collection<GeometryOutput> getAllObservedGeometriesDynamic(DbQuery parameters, Session session,
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

    private GeometryOutput createGeometryInfo(GeometryType type, FeatureEntity featureEntity, DbQuery query) {
        GeometryOutput geometryInfo = new GeometryOutput();
        IoParameters parameters = query.getParameters();
        // String hrefBase = urlHelper.getGeometriesHrefBaseUrl(query.getHrefBase());
        PlatformOutput platform = getPlatfom(featureEntity, query);

        geometryInfo.setId(Long.toString(featureEntity.getId()));
        geometryInfo.setValue(GeometryOutput.PROPERTIES, type, parameters, geometryInfo::setGeometryType);
        // geometryInfo.setValue(GeometryInfo.PROPERTIES, hrefBase, parameters, geometryInfo::setHrefBase);
        geometryInfo.setValue(GeometryOutput.PROPERTIES, platform, parameters, geometryInfo::setPlatform);
        return geometryInfo;
    }

    private PlatformOutput getPlatfom(FeatureEntity entity, DbQuery parameters) {
        DbQuery platformQuery = dbQueryFactory
                .createFrom(parameters.getParameters().extendWith(Parameters.FEATURES, String.valueOf(entity.getId()))
                        .removeAllOf(Parameters.FILTER_FIELDS));

        List<PlatformOutput> platforms = platformRepository.getAllCondensed(platformQuery);
        return platforms.iterator().next();
    }

}
