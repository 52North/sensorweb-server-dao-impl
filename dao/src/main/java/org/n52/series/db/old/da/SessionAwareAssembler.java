/*
 * Copyright (C) 2015-2021 52°North Initiative for Geospatial Open Source
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

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
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
import org.n52.io.response.TagOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.sensorweb.server.db.TimeOutputCreator;
import org.n52.sensorweb.server.db.assembler.mapper.OutputMapperFactory;
import org.n52.sensorweb.server.db.factory.ServiceEntityFactory;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.sensorweb.server.db.old.dao.DefaultDbQueryFactory;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.TagEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class SessionAwareAssembler implements InitializingBean, TimeOutputCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionAwareAssembler.class);

    @Autowired
    protected ServiceEntityFactory serviceEntityFactory;

    protected final DbQueryFactory dbQueryFactory;

    @Autowired
    protected OutputMapperFactory mapperFactory;

    private final CRSUtils crsUtils = CRSUtils.createEpsgForcedXYAxisOrder();

    private final HibernateSessionStore sessionStore;

    @Autowired
    public SessionAwareAssembler(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        this.sessionStore = sessionStore;
        this.dbQueryFactory = dbQueryFactory == null ? new DefaultDbQueryFactory() : dbQueryFactory;
    }

    protected DbQuery getDbQuery(IoParameters parameters) {
        return dbQueryFactory.createFrom(parameters);
    }

    protected CRSUtils getCrsUtils() {
        return crsUtils;
    }

    protected OutputMapperFactory getMapperFactory() {
        return mapperFactory;
    }

    protected Geometry getGeometry(GeometryEntity geometryEntity, DbQuery query) {
        if (geometryEntity == null) {
            return null;
        } else {
            String srid = query.getDatabaseSridCode();
            geometryEntity.setGeometryFactory(createGeometryFactory(srid));
            return geometryEntity.getGeometry();
        }
    }

    private GeometryFactory createGeometryFactory(String srsId) {
        PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
        return srsId == null ? new GeometryFactory(pm) : new GeometryFactory(pm, CRSUtils.getSrsIdFrom(srsId));
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

    protected DatasetParameters createDatasetParameters(DatasetEntity dataset, DbQuery query, Session session) {
        DatasetParameters metadata = new DatasetParameters();
        ServiceEntity service = getServiceEntity(dataset);
        metadata.setService(getCondensedService(service, query));
        metadata.setOffering(getCondensedOffering(dataset.getOffering(), query));
        metadata.setProcedure(getCondensedProcedure(dataset.getProcedure(), query));
        metadata.setPhenomenon(getCondensedPhenomenon(dataset.getPhenomenon(), query));
        metadata.setCategory(getCondensedCategory(dataset.getCategory(), query));
        metadata.setPlatform(getCondensedPlatform(dataset.getPlatform(), query));
        if (dataset.hasTagss()) {
            metadata.setTags(getCondensedTags(dataset.getTags(), query));
        }
        return metadata;
    }

    protected PhenomenonOutput getCondensedPhenomenon(PhenomenonEntity entity, DbQuery parameters) {
        return getMapperFactory().getPhenomenonMapper(parameters).createCondensed(entity);
    }

    protected OfferingOutput getCondensedOffering(OfferingEntity entity, DbQuery parameters) {
        return getMapperFactory().getOfferingMapper(parameters).createCondensed(entity);
    }

    protected ProcedureOutput getCondensedProcedure(ProcedureEntity entity, DbQuery parameters) {
        return getMapperFactory().getProcedureMapper(parameters).createCondensed(entity);
    }

    protected ServiceOutput getCondensedService(ServiceEntity entity, DbQuery parameters) {
        return getMapperFactory().getServiceMapper(parameters).createCondensed(entity);
    }

    protected PlatformOutput getCondensedPlatform(PlatformEntity entity, DbQuery parameters) {
        return getMapperFactory().getPlatformMapper(parameters).createCondensed(entity);
    }

    protected FeatureOutput getCondensedFeature(AbstractFeatureEntity<?> entity, DbQuery parameters) {
        return getMapperFactory().getFeatureMapper(parameters).createCondensed(entity);
    }

    protected CategoryOutput getCondensedCategory(CategoryEntity entity, DbQuery parameters) {
        return getMapperFactory().getCategoryMapper(parameters).createCondensed(entity);
    }

    protected Collection<ParameterOutput> getCondensedTags(Set<TagEntity> tags, DbQuery parameters) {
        return tags.parallelStream().map(t -> getCondensedTag(t, parameters)).collect(Collectors.toSet());
    }

    protected TagOutput getCondensedTag(TagEntity entity, DbQuery parameters) {
        return getMapperFactory().getTagMapper(parameters).createCondensed(entity);
    }

    protected ServiceEntity getServiceEntity() {
        return serviceEntityFactory.getServiceEntity();
    }

    protected ServiceEntity getServiceEntity(DescribableEntity entity) {
        assertServiceAvailable(entity);
        return entity.getService() != null ? entity.getService() : getServiceEntity();
    }

    protected <T extends ParameterOutput> T createCondensed(T result, DescribableEntity entity, DbQuery query) {
        result.setId(Long.toString(entity.getId()));
        if (query.getParameters().isSelected(ParameterOutput.DOMAIN_ID)) {
            result.setValue(ParameterOutput.DOMAIN_ID, entity.getIdentifier(), query.getParameters(),
                    result::setDomainId);
        }
        if (query.getParameters().isSelected(ParameterOutput.LABEL)) {
            result.setValue(ParameterOutput.LABEL, entity.getLabelFrom(query.getLocaleForLabel()),
                    query.getParameters(), result::setLabel);
        }
        if (query.getParameters().isSelected(ParameterOutput.HREF_BASE)) {
            result.setValue(ParameterOutput.HREF_BASE, query.getHrefBase(), query.getParameters(),
                    result::setHrefBase);
        }
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

    @Override
    public void afterPropertiesSet() throws Exception {
        // if (getSession().getSessionFactory().getAllClassMetadata().values().isEmpty()) {
        // throw new IllegalStateException("No Entites mapped. Check series.database.mappings!");
        // }
    }

}
