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
package org.n52.sensorweb.server.db.assembler.mapper;

import javax.inject.Inject;

import org.n52.io.handler.DefaultIoFactory;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.sensorweb.server.db.assembler.core.EntityCounter;
import org.n52.sensorweb.server.db.factory.DataRepositoryTypeFactory;
import org.n52.sensorweb.server.db.factory.ServiceEntityFactory;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.springframework.stereotype.Component;

@Component
public class OutputMapperFactory {

    @Inject
    private ServiceEntityFactory serviceEntityFactory;

    @Inject
    private DbQueryFactory dbQueryFactory;

    @Inject
    private EntityCounter counter;

    @Inject
    private DefaultIoFactory<DatasetOutput<AbstractValue<?>>, AbstractValue<?>> ioFactoryCreator;

    @Inject
    private DataRepositoryTypeFactory dataRepositoryFactory;

    public ServiceOutputMapper getServiceMapper() {
        return getServiceMapper(null);
    }

    public ServiceOutputMapper getServiceMapper(DbQuery query) {
        return getServiceMapper(query, false);
    }

    public ServiceOutputMapper getServiceMapper(DbQuery query, boolean subMapper) {
        return new ServiceOutputMapper(query, this, counter, ioFactoryCreator, subMapper);
    }

    public FeatureOutputMapper getFeatureMapper() {
        return getFeatureMapper(null);
    }

    public FeatureOutputMapper getFeatureMapper(DbQuery query) {
        return getFeatureMapper(query, false);
    }

    public FeatureOutputMapper getFeatureMapper(DbQuery query, boolean subMapper) {
        return new FeatureOutputMapper(query, this, subMapper);
    }

    public PlatformOutputMapper getPlatformMapper() {
        return getPlatformMapper(null);
    }

    public PlatformOutputMapper getPlatformMapper(DbQuery query) {
        return getPlatformMapper(query, false);
    }

    public PlatformOutputMapper getPlatformMapper(DbQuery query, boolean subMapper) {
        return new PlatformOutputMapper(query, this, subMapper);
    }

    public ProcedureOutputMapper getProcedureMapper() {
        return getProcedureMapper(null);
    }

    public ProcedureOutputMapper getProcedureMapper(DbQuery query) {
        return getProcedureMapper(query, false);
    }

    public ProcedureOutputMapper getProcedureMapper(DbQuery query, boolean subMapper) {
        return new ProcedureOutputMapper(query, this, subMapper);
    }

    public OfferingOutputMapper getOfferingMapper() {
        return getOfferingMapper(null);
    }

    public OfferingOutputMapper getOfferingMapper(DbQuery query) {
        return getOfferingMapper(query, false);
    }

    public OfferingOutputMapper getOfferingMapper(DbQuery query, boolean subMapper) {
        return new OfferingOutputMapper(query, this, subMapper);
    }

    public PhenomenonOutputMapper getPhenomenonMapper() {
        return getPhenomenonMapper(null);
    }

    public PhenomenonOutputMapper getPhenomenonMapper(DbQuery query) {
        return getPhenomenonMapper(query, false);
    }

    public PhenomenonOutputMapper getPhenomenonMapper(DbQuery query, boolean subMapper) {
        return new PhenomenonOutputMapper(query, this, subMapper);
    }

    public CategoryOutputMapper getCategoryMapper() {
        return getCategoryMapper(null);
    }

    public CategoryOutputMapper getCategoryMapper(DbQuery query) {
        return getCategoryMapper(query, false);
    }

    public CategoryOutputMapper getCategoryMapper(DbQuery query, boolean subMapper) {
        return new CategoryOutputMapper(query, this);
    }

    public <V extends AbstractValue<?>> ParameterOutputSearchResultMapper<DatasetEntity,
    DatasetOutput<V>> getDatasetMapper(
            DbQuery dbQuery) {
        return getDatasetMapper(null, false);
    }

    public <V extends AbstractValue<?>> ParameterOutputSearchResultMapper<DatasetEntity,
    DatasetOutput<V>> getDatasetMapper(
            DbQuery query, boolean subMapper) {
        return new DatasetOutputMapper(query, this, subMapper);
    }

    public MeasuringProgramOutputMapper getMeasuringProgramMapper() {
        return getMeasuringProgramMapper(null);
    }

    public MeasuringProgramOutputMapper getMeasuringProgramMapper(DbQuery query) {
        return getMeasuringProgramMapper(query, false);
    }

    public MeasuringProgramOutputMapper getMeasuringProgramMapper(DbQuery query, boolean subMapper) {
        return new MeasuringProgramOutputMapper(query, this, subMapper);
    }

    public SamplingOutputMapper getSamplingMapper() {
        return getSamplingMapper(null);
    }

    public SamplingOutputMapper getSamplingMapper(DbQuery query) {
        return getSamplingMapper(query, false);
    }

    public SamplingOutputMapper getSamplingMapper(DbQuery query, boolean subMapper) {
        return new SamplingOutputMapper(query, this, subMapper);
    }

    public TagOutputMapper getTagMapper() {
        return getTagMapper(null);
    }

    public TagOutputMapper getTagMapper(DbQuery query) {
        return getTagMapper(query, false);
    }

    public TagOutputMapper getTagMapper(DbQuery query, boolean subMapper) {
        return new TagOutputMapper(query, this, subMapper);
    }

    protected ServiceEntity getServiceEntity() {
        return serviceEntityFactory.getServiceEntity();
    }

    protected ServiceEntity getServiceEntity(DescribableEntity entity) {
        assertServiceAvailable(entity);
        return entity.getService() != null ? entity.getService() : getServiceEntity();
    }

    protected EntityCounter getCounter() {
        return counter;
    }

    protected DefaultIoFactory<DatasetOutput<AbstractValue<?>>, AbstractValue<?>> getIoFactoryCreator() {
        return ioFactoryCreator;
    }

    protected DbQuery getDbQuery(IoParameters parameters) {
        return dbQueryFactory.createFrom(parameters);
    }

    protected DataRepositoryTypeFactory getDataRepositoryFactory() {
        return dataRepositoryFactory;
    }

    private void assertServiceAvailable(DescribableEntity entity) throws IllegalStateException {
        if ((getServiceEntity() == null) && (entity == null)) {
            throw new IllegalStateException("No service instance available");
        }
    }

}
