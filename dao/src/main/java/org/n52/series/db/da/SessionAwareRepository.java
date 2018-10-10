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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.IoParameters;
import org.n52.io.response.CategoryOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.OfferingOutput;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.PlatformType;
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.HibernateSessionStore;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.Describable;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.dataset.Dataset;
import org.n52.series.db.beans.dataset.QuantityDataset;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DbQueryFactory;
import org.n52.series.db.dao.DefaultDbQueryFactory;
import org.n52.web.ctrl.UrlSettings;
import org.n52.web.exception.BadRequestException;
import org.n52.web.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Geometry;

public abstract class SessionAwareRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionAwareRepository.class);

    // via xml or db
    @Autowired(required = false)
    protected ServiceEntity serviceEntity;

    @Autowired
    protected DbQueryFactory dbQueryFactory;

    private final CRSUtils internalCrsUtils = CRSUtils.createEpsgStrictAxisOrder();

    @Autowired
    private HibernateSessionStore sessionStore;

    public DbQueryFactory getDbQueryFactory() {
        return dbQueryFactory != null
                ? dbQueryFactory
                : new DefaultDbQueryFactory();
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
        return internalCrsUtils;
    }

    protected Geometry getGeometry(GeometryEntity geometryEntity, DbQuery query) {
        if (geometryEntity == null) {
            return null;
        } else {
            String srid = query.getDatabaseSridCode();
            geometryEntity.setGeometryFactory(getCrsUtils().createGeometryFactory(srid));
            return geometryEntity.getGeometry();
        }
    }

    // XXX a bit misplaced here
    protected String getPlatformId(Dataset dataset) {
        ProcedureEntity procedure = dataset.getProcedure();
        boolean mobile = procedure.isMobile();
        boolean insitu = procedure.isInsitu();
        PlatformType type = PlatformType.toInstance(mobile, insitu);
        DescribableEntity entity = type.isStationary()
                ? dataset.getFeature()
                : procedure;
        return type.createId(entity.getId());
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

    protected Map<String, DatasetParameters> createTimeseriesList(List<QuantityDatasetEntity> series,
                                                                  DbQuery parameters)
            throws DataAccessException {
        Map<String, DatasetParameters> timeseriesOutputs = new HashMap<>();
        for (QuantityDatasetEntity timeseries : series) {
            if (!timeseries.getProcedure()
                           .isReference()) {
                String timeseriesId = Long.toString(timeseries.getId());
                timeseriesOutputs.put(timeseriesId, createTimeseriesOutput(timeseries, parameters));
            }
        }
        return timeseriesOutputs;
    }

    protected DatasetParameters createTimeseriesOutput(QuantityDataset dataset, DbQuery parameters)
            throws DataAccessException {
        DatasetParameters metadata = new DatasetParameters();
        ServiceEntity service = getServiceEntity(dataset);
        metadata.setService(getCondensedService(service, parameters));
        metadata.setOffering(getCondensedOffering(dataset.getOffering(), parameters));
        metadata.setProcedure(getCondensedProcedure(dataset.getProcedure(), parameters));
        metadata.setPhenomenon(getCondensedPhenomenon(dataset.getPhenomenon(), parameters));
        metadata.setFeature(getCondensedFeature(dataset.getFeature(), parameters));
        metadata.setCategory(getCondensedCategory(dataset.getCategory(), parameters));
        return metadata;
    }

    protected DatasetParameters createDatasetParameters(DatasetEntity dataset, DbQuery query, Session session)
            throws DataAccessException {
        DatasetParameters metadata = new DatasetParameters();
        ServiceEntity service = getServiceEntity(dataset);
        metadata.setService(getCondensedExtendedService(service, query));
        metadata.setOffering(getCondensedExtendedOffering(dataset.getOffering(), query));
        metadata.setProcedure(getCondensedExtendedProcedure(dataset.getProcedure(), query));
        metadata.setPhenomenon(getCondensedExtendedPhenomenon(dataset.getPhenomenon(), query));
        metadata.setFeature(getCondensedExtendedFeature(dataset.getFeature(), query));

        DescribableEntity category = dataset.getCategory() == null
                ? dataset.getPhenomenon()
                : dataset.getCategory();
        metadata.setCategory(getCondensedExtendedCategory(category, query));
        // seriesParameter.setPlatform(getCondensedPlatform(series, parameters, session)); // #309
        return metadata;
    }

    protected PhenomenonOutput getCondensedPhenomenon(PhenomenonEntity entity, DbQuery parameters) {
        return createCondensed(new PhenomenonOutput(), entity, parameters);
    }

    protected PhenomenonOutput getCondensedExtendedPhenomenon(PhenomenonEntity entity, DbQuery parameters) {
        return createCondensed(new PhenomenonOutput(),
                               entity,
                               parameters,
                               createHref(parameters.getHrefBase(), UrlSettings.COLLECTION_PHENOMENA));
    }

    protected OfferingOutput getCondensedOffering(OfferingEntity entity, DbQuery parameters) {
        return createCondensed(new OfferingOutput(), entity, parameters);
    }

    protected ServiceOutput getCondensedService(ServiceEntity entity, DbQuery parameters) {
        return entity != null
                ? createCondensed(new ServiceOutput(), entity, parameters)
                : createCondensed(new ServiceOutput(), getServiceEntity(), parameters);
    }

    protected OfferingOutput getCondensedExtendedOffering(OfferingEntity entity, DbQuery parameters) {
        return createCondensed(new OfferingOutput(),
                               entity,
                               parameters);
    }

    public void setServiceEntity(ServiceEntity serviceEntity) {
        this.serviceEntity = serviceEntity;
    }

    protected ServiceEntity getServiceEntity() {
        return serviceEntity;
    }

    protected ServiceEntity getServiceEntity(Describable entity) {
        assertServiceAvailable(entity);
        return entity.getService() != null
                ? entity.getService()
                : serviceEntity;
    }

    protected ServiceOutput getCondensedExtendedService(ServiceEntity entity, DbQuery parameters) {
        final String hrefBase = createHref(parameters.getHrefBase(), UrlSettings.COLLECTION_SERVICES);
        return createCondensed(new ServiceOutput(), entity, parameters, hrefBase);
    }

    protected <T extends ParameterOutput> T createCondensed(T result,
                                                            DescribableEntity entity,
                                                            DbQuery parameters) {
        String id = Long.toString(entity.getId());
        String label = entity.getLabelFrom(parameters.getLocale());
        result.setId(id);
        result.setValue(ParameterOutput.LABEL, label, parameters.getParameters(), result::setLabel);
        return result;
    }

    protected <T extends ParameterOutput> T createCondensed(T result, DescribableEntity entity, DbQuery parameters,
            String hrefBase) {
        createCondensed(result, entity, parameters);
        String href = hrefBase + "/" + result.getId();
        result.setValue(ParameterOutput.HREF, href, parameters.getParameters(), result::setHref);
        return result;
    }

    protected ProcedureOutput getCondensedProcedure(ProcedureEntity entity, DbQuery parameters) {
        return createCondensed(new ProcedureOutput(), entity, parameters);
    }

    protected ProcedureOutput getCondensedExtendedProcedure(ProcedureEntity entity, DbQuery parameters) {
        return createCondensed(new ProcedureOutput(),
                               entity,
                               parameters,
                               createHref(parameters.getHrefBase(), UrlSettings.COLLECTION_PROCEDURES));
    }

    protected FeatureOutput getCondensedFeature(AbstractFeatureEntity entity, DbQuery parameters) {
        return createCondensed(new FeatureOutput(), entity, parameters);
    }

    protected FeatureOutput getCondensedExtendedFeature(AbstractFeatureEntity entity, DbQuery parameters) {
        return createCondensed(new FeatureOutput(),
                               entity,
                               parameters,
                               createHref(parameters.getHrefBase(), UrlSettings.COLLECTION_FEATURES));
    }

    protected CategoryOutput getCondensedCategory(CategoryEntity entity, DbQuery parameters) {
        return createCondensed(new CategoryOutput(), entity, parameters);
    }

    protected CategoryOutput getCondensedExtendedCategory(DescribableEntity entity, DbQuery parameters) {
        return createCondensed(new CategoryOutput(),
                               entity,
                               parameters,
                               createHref(parameters.getHrefBase(), UrlSettings.COLLECTION_CATEGORIES));
    }

    private void assertServiceAvailable(Describable entity) throws IllegalStateException {
        if (serviceEntity == null && entity == null) {
            throw new IllegalStateException("No service instance available");
        }
    }

    protected String createHref(String hrefBase, String collection) {
        return new StringBuilder(hrefBase).append("/").append(collection).toString();
    }

}
