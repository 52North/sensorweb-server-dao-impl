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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.n52.io.handler.DatasetFactoryException;
import org.n52.io.handler.DefaultIoFactory;
import org.n52.io.handler.IoHandlerFactory;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.ServiceOutput.DatasetCount;
import org.n52.io.response.ServiceOutput.ParameterCount;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.sensorweb.server.db.assembler.core.EntityCounter;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.ServiceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceOutputMapper extends ParameterOutputSearchResultMapper<ServiceEntity, ServiceOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceOutputMapper.class);

    private static final String SERVICE_TYPE = "Restful series access layer.";

    private final EntityCounter counter;
    private final DefaultIoFactory<DatasetOutput<AbstractValue<?>>, AbstractValue<?>> ioFactoryCreator;

    public ServiceOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory, EntityCounter counter,
            DefaultIoFactory<DatasetOutput<AbstractValue<?>>, AbstractValue<?>> ioFactoryCreator, boolean subMappper) {
        super(query, outputMapperFactory, subMappper);
        this.counter = counter;
        this.ioFactoryCreator = ioFactoryCreator;
    }

    @Override
    public ServiceOutput addExpandedValues(ServiceEntity entity, ServiceOutput output) {
        IoParameters parameters = getDbQuery().getParameters();
        ParameterCount quantities = countParameters(output);
        boolean supportsFirstLatest = entity.getSupportsFirstLast();

        String serviceUrl = entity.getUrl();
        String type = getServiceType(entity);

        output.setValue(ServiceOutput.SERVICE_URL, serviceUrl, parameters, output::setServiceUrl);
        output.setValue(ServiceOutput.TYPE, type, parameters, output::setType);

        Map<String, Object> features = new HashMap<>();
        features.put(ServiceOutput.QUANTITIES, quantities);
        features.put(ServiceOutput.SUPPORTS_FIRST_LATEST, supportsFirstLatest);
        features.put(ServiceOutput.SUPPORTED_MIME_TYPES, getSupportedDatasets(output));
        String version = (entity.getVersion() != null) ? entity.getVersion() : "3.0";
        output.setValue(ServiceOutput.VERSION, version, parameters, output::setVersion);
        output.setValue(ServiceOutput.FEATURES, features, parameters, output::setFeatures);
        output.setValue(ParameterOutput.HREF_BASE, getHrefBase(), parameters, output::setHrefBase);
        return output;
    }

    private ParameterCount countParameters(ServiceOutput service) {
        IoParameters parameters = getDbQuery().getParameters();
        ParameterCount quantities = new ServiceOutput.ParameterCount();
        DbQuery serviceQuery = getOutputMapperFactory().getDbQuery(parameters
                .extendWith(IoParameters.SERVICES, service.getId()).removeAllOf("offset").removeAllOf("limit"));
        quantities.setOfferingsSize(counter.countOfferings(serviceQuery));
        quantities.setProceduresSize(counter.countProcedures(serviceQuery));
        quantities.setCategoriesSize(counter.countCategories(serviceQuery));
        quantities.setPhenomenaSize(counter.countPhenomena(serviceQuery));
        quantities.setFeaturesSize(counter.countFeatures(serviceQuery));
        quantities.setPlatformsSize(counter.countPlatforms(serviceQuery));
        quantities.setTagsSize(counter.countTags(serviceQuery));
        quantities.setDatasets(createDatasetCount(counter, serviceQuery));

        quantities.setSamplingsSize(counter.countSamplings(serviceQuery));
        quantities.setMeasuringProgramsSize(counter.countMeasuringPrograms(serviceQuery));
        return quantities;
    }

    private DatasetCount createDatasetCount(EntityCounter counter, DbQuery query) {
        DatasetCount datasetCount = new DatasetCount();
        datasetCount.setTotalAmount(counter.countDatasets(query));
        datasetCount.setAmountTimeseries(counter.countTimeseries(query));
        datasetCount.setAmountIndividualObservations(counter.countIndividualObservations(query));
        datasetCount.setAmountProfiles(counter.countProfiles(query));
        datasetCount.setAmountTrajectories(counter.countTrajectories(query));
        return datasetCount;
    }

    private String getServiceType(ServiceEntity entity) {
        return entity.getType() != null ? entity.getType() : SERVICE_TYPE;
    }

    private Map<String, Set<String>> getSupportedDatasets(ServiceOutput service) {
        Map<String, Set<String>> mimeTypesByDatasetTypes = new HashMap<>();
        for (String valueType : ioFactoryCreator.getKnownTypes()) {
            try {
                IoHandlerFactory<?, ?> factory = ioFactoryCreator.create(valueType);
                mimeTypesByDatasetTypes.put(valueType, factory.getSupportedMimeTypes());
            } catch (DatasetFactoryException e) {
                LOGGER.error("IO Factory for type '{}' couldn't be created.", valueType);
            }
        }
        return mimeTypesByDatasetTypes;
    }

    @Override
    public ServiceOutput getParameterOuput() {
        return new ServiceOutput();
    }

}
