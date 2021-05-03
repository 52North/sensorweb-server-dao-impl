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

    public FeatureMapper getFeatureMapper() {
        return new FeatureMapper(this);
    }

    public ServiceMapper getServiceMapper() {
        return new ServiceMapper(this);
    }

    public PlatformMapper getPlatformMapper() {
        return new PlatformMapper(this);
    }

    public ProcedureMapper getProcedureMapper() {
        return new ProcedureMapper(this);
    }

    public OfferingMapper getOfferingMapper() {
        return new OfferingMapper(this);
    }

    public PhenomenonMapper getPhenomenonMapper() {
        return new PhenomenonMapper(this);
    }

    public CategoryMapper getCategoryMapper() {
        return new CategoryMapper(this);
    }

    public <V extends AbstractValue<?>> DatasetMapper<V> getDatasetMapper() {
        return new DatasetMapper<>(this);

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
        if ((getServiceEntity() == null) && (entity == null)) {
            throw new IllegalStateException("No service instance available");
        }
    }

}
