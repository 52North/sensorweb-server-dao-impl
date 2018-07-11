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

package org.n52.series.db.old.da;

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
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.n52.series.db.old.dao.DefaultDbQueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Geometry;

public abstract class SessionAwareAssembler implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionAwareAssembler.class);

    // via xml or db
    @Autowired(required = false)
    protected ServiceEntity serviceEntity;

    protected final DbQueryFactory dbQueryFactory;

    private final CRSUtils internalCrsUtils = CRSUtils.createEpsgStrictAxisOrder();

    private final HibernateSessionStore sessionStore;

    @Autowired
    public SessionAwareAssembler(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        this.sessionStore = sessionStore;
        this.dbQueryFactory = dbQueryFactory == null
            ? new DefaultDbQueryFactory()
            : dbQueryFactory;
    }

    protected DbQuery getDbQuery(IoParameters parameters) {
        return dbQueryFactory.createFrom(parameters);
    }

    protected CRSUtils getCrsUtils() {
        return internalCrsUtils;
    }

    protected Geometry getGeometry(final GeometryEntity geometryEntity, final DbQuery query) {
        return geometryEntity != null
            ? geometryEntity.getGeometry(query.getGeometryFactory())
            : null;
    }

    // XXX a bit misplaced here
    protected static String getPlatformId(DatasetEntity dataset) {
        ProcedureEntity procedure = dataset.getProcedure();
        boolean mobile = procedure.isMobile();
        boolean insitu = procedure.isInsitu();
        PlatformType type = PlatformType.toInstance(mobile, insitu);
        DescribableEntity entity = type.isStationary()
            ? dataset.getFeature()
            : procedure;
        return type.createId(entity.getId());
    }

    protected Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            LOGGER.debug("Unable to parse '{}' to Long.", e);
            return null;
        }
    }

    protected HibernateSessionStore getSessionStore() {
        return sessionStore;
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

    protected Map<String, DatasetParameters> createTimeseriesList(List<QuantityDatasetEntity> series, DbQuery query) {
        Map<String, DatasetParameters> timeseriesOutputs = new HashMap<>();
        for (DatasetEntity timeseries : series) {
            if (!timeseries.getProcedure()
                           .isReference()) {
                String timeseriesId = Long.toString(timeseries.getId());
                timeseriesOutputs.put(timeseriesId, createTimeseriesOutput(timeseries, query));
            }
        }
        return timeseriesOutputs;
    }

    protected DatasetParameters createTimeseriesOutput(DatasetEntity dataset, DbQuery parameters) {
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

    protected DatasetParameters createDatasetParameters(DatasetEntity dataset, DbQuery query, Session session) {
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

    private PhenomenonOutput getCondensedPhenomenon(PhenomenonEntity entity, DbQuery parameters) {
        return createCondensed(new PhenomenonOutput(), entity, parameters);
    }

    private PhenomenonOutput getCondensedExtendedPhenomenon(PhenomenonEntity entity, DbQuery parameters) {
        return createCondensed(new PhenomenonOutput(), entity, parameters);
    }

    private OfferingOutput getCondensedOffering(OfferingEntity entity, DbQuery parameters) {
        return createCondensed(new OfferingOutput(), entity, parameters);
    }

    protected ServiceOutput getCondensedService(ServiceEntity entity, DbQuery parameters) {
        return entity != null
            ? createCondensed(new ServiceOutput(), entity, parameters)
            : createCondensed(new ServiceOutput(), serviceEntity, parameters);
    }

    private OfferingOutput getCondensedExtendedOffering(OfferingEntity entity, DbQuery parameters) {
        return createCondensed(new OfferingOutput(), entity, parameters);
    }

    protected ServiceEntity getServiceEntity(DescribableEntity entity) {
        assertServiceAvailable(entity);
        return entity.getService() != null
            ? entity.getService()
            : serviceEntity;
    }

    protected ServiceOutput getCondensedExtendedService(ServiceEntity entity, DbQuery query) {
        return createCondensed(new ServiceOutput(), entity, query);
    }

    protected <T extends ParameterOutput> T createCondensed(T result,
                                                            DescribableEntity entity,
                                                            DbQuery query) {
        String id = Long.toString(entity.getId());
        String label = entity.getLabelFrom(query.getLocale());
        String hrefBase = query.getHrefBase();
        result.setId(id);
        result.setValue(ParameterOutput.LABEL, label, query.getParameters(), result::setLabel);
        result.setValue(ParameterOutput.HREF, hrefBase, query.getParameters(), result::setHrefBase);
        return result;
    }

    private ProcedureOutput getCondensedProcedure(ProcedureEntity entity, DbQuery parameters) {
        return createCondensed(new ProcedureOutput(), entity, parameters);
    }

    private ProcedureOutput getCondensedExtendedProcedure(ProcedureEntity entity, DbQuery parameters) {
        return createCondensed(new ProcedureOutput(), entity, parameters);
    }

    private FeatureOutput getCondensedFeature(AbstractFeatureEntity<?> entity, DbQuery parameters) {
        return createCondensed(new FeatureOutput(), entity, parameters);
    }

    private FeatureOutput getCondensedExtendedFeature(AbstractFeatureEntity<?> entity, DbQuery parameters) {
        return createCondensed(new FeatureOutput(), entity, parameters);
    }

    private CategoryOutput getCondensedCategory(CategoryEntity entity, DbQuery parameters) {
        return createCondensed(new CategoryOutput(), entity, parameters);
    }

    private CategoryOutput getCondensedExtendedCategory(DescribableEntity entity, DbQuery parameters) {
        return createCondensed(new CategoryOutput(), entity, parameters);
    }

    private void assertServiceAvailable(DescribableEntity entity) throws IllegalStateException {
        if ((serviceEntity == null) && (entity == null)) {
            throw new IllegalStateException("No service instance available");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (getSession().getSessionFactory().getMetamodel().getEntities().isEmpty()) {
            throw new IllegalStateException("No Entites mapped. Check series.database.mappings!");
        }
    }

}
