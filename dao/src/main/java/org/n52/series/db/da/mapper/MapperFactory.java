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
package org.n52.series.db.da.mapper;

import org.n52.io.handler.DefaultIoFactory;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.ServiceEntityFactory;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.da.EntityCounter;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DbQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MapperFactory {

    @Autowired
    private ServiceEntityFactory serviceEntityFactory;

    @Autowired
    private DbQueryFactory dbQueryFactory;

    @Autowired
    private EntityCounter counter;

    @Autowired
    private DefaultIoFactory<DatasetOutput<AbstractValue<?>>, AbstractValue<?>> ioFactoryCreator;

    @Autowired
    private DataRepositoryTypeFactory dataRepositoryFactory;

    public ServiceMapper getServiceMapper(IoParameters params) {
        return getServiceMapper(params, false);
    }

    public ServiceMapper getServiceMapper(IoParameters params, boolean subMapper) {
        return new ServiceMapper(this, params, subMapper);
    }

    public FeatureMapper getFeatureMapper(IoParameters params) {
        return getFeatureMapper(params, false);
    }

    public FeatureMapper getFeatureMapper(IoParameters params, boolean subMapper) {
        return new FeatureMapper(this, params, subMapper);
    }

    public PlatformMapper getPlatformMapper(IoParameters params) {
        return getPlatformMapper(params, false);
    }

    public PlatformMapper getPlatformMapper(IoParameters params, boolean subMapper) {
        return new PlatformMapper(this, params, subMapper);
    }

    public ProcedureMapper getProcedureMapper(IoParameters params) {
        return getProcedureMapper(params, false);
    }

    public ProcedureMapper getProcedureMapper(IoParameters params, boolean subMapper) {
        return new ProcedureMapper(this, params, subMapper);
    }

    public OfferingMapper getOfferingMapper(IoParameters params) {
        return getOfferingMapper(params, false);
    }

    public OfferingMapper getOfferingMapper(IoParameters params, boolean subMapper) {
        return new OfferingMapper(this, params, subMapper);
    }

    public PhenomenonMapper getPhenomenonMapper(IoParameters params) {
        return getPhenomenonMapper(params, false);
    }

    public PhenomenonMapper getPhenomenonMapper(IoParameters params, boolean subMapper) {
        return new PhenomenonMapper(this, params, subMapper);
    }

    public CategoryMapper getCategoryMapper(IoParameters params) {
        return getCategoryMapper(params, false);
    }

    public CategoryMapper getCategoryMapper(IoParameters params, boolean subMapper) {
        return new CategoryMapper(this, params, subMapper);
    }

    public <V extends AbstractValue<?>> DatasetMapper<V> getDatasetMapper(IoParameters params) {
        return getDatasetMapper(params, false);
    }

    public <V extends AbstractValue<?>> DatasetMapper<V> getDatasetMapper(IoParameters params, boolean subMapper) {
        return new DatasetMapper<>(this, params, subMapper);
    }

    public SamplingMapper getSamplingMapper(IoParameters params) {
        return new SamplingMapper(this, params);
    }

    public MeasuringProgramMapper getMeasuringProgramMapper(IoParameters params) {
        return new MeasuringProgramMapper(this, params);
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
        if (getServiceEntity() == null && entity == null) {
            throw new IllegalStateException("No service instance available");
        }
    }

}
