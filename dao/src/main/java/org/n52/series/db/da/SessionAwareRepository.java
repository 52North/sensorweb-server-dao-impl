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
package org.n52.series.db.da;

import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import org.hibernate.Session;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.IoParameters;
import org.n52.io.response.CategoryOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.OfferingOutput;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.TimeOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.HibernateSessionStore;
import org.n52.series.db.ServiceEntityFactory;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.da.mapper.MapperFactory;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DbQueryFactory;
import org.n52.series.db.dao.DefaultDbQueryFactory;
import org.n52.web.exception.BadRequestException;
import org.n52.web.exception.ResourceNotFoundException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class SessionAwareRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionAwareRepository.class);
    private static final String OFFSET_REGEX = "([+-](?:2[0-3]|[01][0-9]):[0-5][0-9])";

    @Autowired
    protected ServiceEntityFactory serviceEntityFactory;

    @Autowired
    protected DbQueryFactory dbQueryFactory;

    @Autowired
    protected MapperFactory mapperFactory;

    private final CRSUtils crsUtils = CRSUtils.createEpsgForcedXYAxisOrder();

    @Autowired
    private HibernateSessionStore sessionStore;

    public DbQueryFactory getDbQueryFactory() {
        return dbQueryFactory != null ? dbQueryFactory : new DefaultDbQueryFactory();
    }

    public void setDbQueryFactory(DbQueryFactory dbQueryFactory) {
        this.dbQueryFactory = dbQueryFactory;
    }

    protected DbQuery getDbQuery(IoParameters parameters) {
        return dbQueryFactory.createFrom(parameters);
    }

    public HibernateSessionStore getSessionStore() {
        return sessionStore;
    }

    public void setSessionStore(HibernateSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    protected CRSUtils getCrsUtils() {
        return crsUtils;
    }

    protected MapperFactory getMapperFactory() {
        return mapperFactory;
    }

    protected Geometry getGeometry(GeometryEntity geometryEntity, DbQuery query) {
        if (geometryEntity == null) {
            return null;
        } else {
            String srid = query.getDatabaseSridCode();
            geometryEntity.setGeometryFactory(createGeometryFactory(srid));
            try {
                return getCrsUtils().transformOuterToInner(geometryEntity.getGeometry(), srid);
            } catch (FactoryException | TransformException e) {
                throw new DataAccessException("Error while creating geometry!", e);
            }
        }
    }

    private GeometryFactory createGeometryFactory(String srsId) {
        PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
        return srsId == null ? new GeometryFactory(pm) : new GeometryFactory(pm, CRSUtils.getSrsIdFrom(srsId));
    }

    protected Long parseId(String id) throws BadRequestException {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            LOGGER.debug("Unable to parse {} to Long.", e);
            throw new ResourceNotFoundException("Resource with id '" + id + "' could not be found.");
        }
    }

    public void returnSession(Session session) {
        sessionStore.returnSession(session);
    }

    public Session getSession() {
        try {
            return sessionStore.getSession();
        } catch (Throwable e) {
            throw new IllegalStateException("Could not get hibernate session.", e);
        }
    }

    // protected Map<String, DatasetParameters> createTimeseriesList(Collection<DatasetEntity> series,
    // DbQuery parameters)
    // throws DataAccessException {
    // Map<String, DatasetParameters> timeseriesOutputs = new HashMap<>();
    // for (DatasetEntity timeseries : series) {
    // if (!timeseries.getProcedure()
    // .isReference()) {
    // String timeseriesId = Long.toString(timeseries.getId());
    // timeseriesOutputs.put(timeseriesId, createTimeseriesOutput(timeseries, parameters));
    // }
    // }
    // return timeseriesOutputs;
    // }
    //
    // protected DatasetParameters createTimeseriesOutput(DatasetEntity dataset, DbQuery parameters)
    // throws DataAccessException {
    // DatasetParameters metadata = new DatasetParameters();
    // ServiceEntity service = getServiceEntity(dataset);
    // metadata.setService(getCondensedService(service, parameters));
    // metadata.setOffering(getCondensedOffering(dataset.getOffering(), parameters));
    // metadata.setProcedure(getCondensedProcedure(dataset.getProcedure(), parameters));
    // metadata.setPhenomenon(getCondensedPhenomenon(dataset.getPhenomenon(), parameters));
    // metadata.setCategory(getCondensedCategory(dataset.getCategory(), parameters));
    // metadata.setPlatform(getCondensedPlatform(dataset.getPlatform(), parameters));
    // return metadata;
    // }

    protected DatasetParameters createDatasetParameters(DatasetEntity dataset, DbQuery query, Session session)
            throws DataAccessException {
        DatasetParameters metadata = new DatasetParameters();
        ServiceEntity service = getServiceEntity(dataset);
        metadata.setService(getCondensedExtendedService(service, query));
        metadata.setOffering(getCondensedExtendedOffering(dataset.getOffering(), query));
        metadata.setProcedure(getCondensedExtendedProcedure(dataset.getProcedure(), query));
        metadata.setPhenomenon(getCondensedExtendedPhenomenon(dataset.getPhenomenon(), query));
        metadata.setCategory(getCondensedExtendedCategory(dataset.getCategory(), query));
        metadata.setPlatform(getCondensedPlatform(dataset.getPlatform(), query));
        return metadata;
    }

    protected PhenomenonOutput getCondensedPhenomenon(PhenomenonEntity entity, DbQuery parameters) {
        return getMapperFactory().getPhenomenonMapper().createCondensed(entity, parameters);
    }

    protected PhenomenonOutput getCondensedExtendedPhenomenon(PhenomenonEntity entity, DbQuery parameters) {
        return getCondensedPhenomenon(entity, parameters);
    }

    protected OfferingOutput getCondensedOffering(OfferingEntity entity, DbQuery parameters) {
        return getMapperFactory().getOfferingMapper().createCondensed(entity, parameters);
    }

    protected OfferingOutput getCondensedExtendedOffering(OfferingEntity entity, DbQuery parameters) {
        return getCondensedOffering(entity, parameters);
    }

    protected ProcedureOutput getCondensedProcedure(ProcedureEntity entity, DbQuery parameters) {
        return getMapperFactory().getProcedureMapper().createCondensed(entity, parameters);
    }

    protected ProcedureOutput getCondensedExtendedProcedure(ProcedureEntity entity, DbQuery parameters) {
        return getCondensedProcedure(entity, parameters);
    }

    protected ServiceOutput getCondensedService(ServiceEntity entity, DbQuery query) {
        return getMapperFactory().getServiceMapper().createCondensed(entity, query);
    }

    protected ServiceOutput getCondensedExtendedService(ServiceEntity entity, DbQuery parameters) {
        return getCondensedService(entity, parameters);
    }


    protected PlatformOutput getCondensedPlatform(PlatformEntity entity, DbQuery parameters) {
        return getMapperFactory().getPlatformMapper().createCondensed(entity, parameters);
    }

    protected FeatureOutput getCondensedFeature(AbstractFeatureEntity<?> entity, DbQuery parameters) {
        return getMapperFactory().getFeatureMapper().createCondensed((FeatureEntity) entity, parameters);
    }

    protected FeatureOutput getCondensedExtendedFeature(AbstractFeatureEntity<?> entity, DbQuery parameters) {
        return getCondensedFeature(entity, parameters);
    }

    protected CategoryOutput getCondensedCategory(CategoryEntity entity, DbQuery parameters) {
        return getMapperFactory().getCategoryMapper().createCondensed(entity, parameters);
    }

    protected CategoryOutput getCondensedExtendedCategory(CategoryEntity entity, DbQuery parameters) {
        return getCondensedCategory(entity, parameters);
    }

    protected ServiceEntity getServiceEntity() {
        return serviceEntityFactory.getServiceEntity();
    }

    protected ServiceEntity getServiceEntity(DescribableEntity entity) {
        assertServiceAvailable(entity);
        return entity.getService() != null
            ? entity.getService()
            : getServiceEntity();
    }

    protected <T extends ParameterOutput> T createCondensed(T result, DescribableEntity entity, DbQuery query) {
        String id = Long.toString(entity.getId());
        String label = entity.getLabelFrom(query.getLocale());
        String domainId = entity.getIdentifier();
        String hrefBase = query.getHrefBase();

        result.setId(id);
        result.setValue(ParameterOutput.DOMAIN_ID, domainId, query.getParameters(), result::setDomainId);
        result.setValue(ParameterOutput.LABEL, label, query.getParameters(), result::setLabel);
        result.setValue(ParameterOutput.HREF_BASE, hrefBase, query.getParameters(), result::setHrefBase);
        return result;
    }

    protected Geometry createGeometry(AbstractFeatureEntity<?> featureEntity, DbQuery query) {
        return featureEntity.isSetGeometry() ? getGeometry(featureEntity.getGeometryEntity(), query) : null;
    }

    private void assertServiceAvailable(DescribableEntity entity) throws IllegalStateException {
        if ((getServiceEntity() == null) && (entity == null)) {
            throw new IllegalStateException("No service instance available");
        }
    }

    protected TimeOutput createTimeOutput(Date date, IoParameters parameters) {
        if (date != null) {
            return new TimeOutput(new DateTime(date), parameters.formatToUnixTime());
        }
        return null;
    }

    protected TimeOutput createTimeOutput(Date date, String originTimezone, IoParameters parameters) {
        if (date != null) {
            DateTimeZone zone = getOriginTimeZone(originTimezone);
            return new TimeOutput(new DateTime(date).withZone(zone), parameters.formatToUnixTime());
        }
        return null;
    }

    protected DateTimeZone getOriginTimeZone(String originTimezone) {
        if (originTimezone != null && !originTimezone.isEmpty()) {
            if (originTimezone.matches(OFFSET_REGEX)) {
                return DateTimeZone.forTimeZone(TimeZone.getTimeZone(ZoneOffset.of(originTimezone).normalized()));
            } else {
                return DateTimeZone.forID(originTimezone.trim());
            }
        }
        return DateTimeZone.UTC;
    }

}
