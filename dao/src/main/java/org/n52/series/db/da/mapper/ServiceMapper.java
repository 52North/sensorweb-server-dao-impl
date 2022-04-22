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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.n52.io.handler.DatasetFactoryException;
import org.n52.io.handler.IoHandlerFactory;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.ServiceOutput.DatasetCount;
import org.n52.io.response.ServiceOutput.ParameterCount;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.da.EntityCounter;
import org.n52.series.db.dao.DbQuery;
import org.n52.web.exception.InternalServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceMapper extends AbstractOuputMapper<ServiceOutput, ServiceEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceMapper.class);

    private static final String SERVICE_TYPE = "Restful series access layer.";

    public ServiceMapper(MapperFactory mapperFactory, IoParameters params) {
        super(mapperFactory, params, true);
    }

    public ServiceMapper(MapperFactory mapperFactory, IoParameters params, boolean subMapper) {
        super(mapperFactory, params, subMapper);
    }

    @Override
    public ServiceOutput createCondensed(ServiceEntity entity, DbQuery query) {
        return entity != null ? createCondensed(new ServiceOutput(), entity, query)
                : createCondensed(new ServiceOutput(), getMapperFactory().getServiceEntity(), query);
    }

    @Override
    public ServiceOutput createExpanded(ServiceEntity entity, DbQuery query, Session session) {
        try {
            ServiceOutput result = createCondensed(entity, query);
            IoParameters parameters = query.getParameters();

            ParameterCount quantities = countParameters(result, query);
            boolean supportsFirstLatest = entity.getSupportsFirstLast();

            String serviceUrl = entity.getUrl();
            String type = getServiceType(entity);

            result.setValue(ServiceOutput.SERVICE_URL, serviceUrl, parameters, result::setServiceUrl);
            result.setValue(ServiceOutput.TYPE, type, parameters, result::setType);

            Map<String, Object> features = new HashMap<>();
            features.put(ServiceOutput.QUANTITIES, quantities);
            features.put(ServiceOutput.SUPPORTS_FIRST_LATEST, supportsFirstLatest);
            features.put(ServiceOutput.SUPPORTED_MIME_TYPES, getSupportedDatasets(result));

            String version = (entity.getVersion() != null) ? entity.getVersion() : "2.0";

            String hrefBase = query.getHrefBase();
            result.setValue(ServiceOutput.VERSION, version, parameters, result::setVersion);
            result.setValue(ServiceOutput.FEATURES, features, parameters, result::setFeatures);
            result.setValue(ParameterOutput.HREF_BASE, hrefBase, parameters, result::setHrefBase);
            return result;
        } catch (Exception e) {
            log(entity, e);
        }
        return null;
    }

    private String getServiceType(ServiceEntity entity) {
        return entity.getType() != null ? entity.getType() : SERVICE_TYPE;
    }

    private ParameterCount countParameters(ServiceOutput service, DbQuery query) {
        try {
            IoParameters parameters = query.getParameters();
            ParameterCount quantities = new ServiceOutput.ParameterCount();
            DbQuery serviceQuery = getMapperFactory().getDbQuery(parameters
                    .extendWith(Parameters.SERVICES, service.getId()).removeAllOf("offset").removeAllOf("limit"));
            quantities.setOfferingsSize(getMapperFactory().getCounter().countOfferings(serviceQuery));
            quantities.setProceduresSize(getMapperFactory().getCounter().countProcedures(serviceQuery));
            quantities.setCategoriesSize(getMapperFactory().getCounter().countCategories(serviceQuery));
            quantities.setPhenomenaSize(getMapperFactory().getCounter().countPhenomena(serviceQuery));
            quantities.setFeaturesSize(getMapperFactory().getCounter().countFeatures(serviceQuery));

            // if (parameters.shallBehaveBackwardsCompatible()) {
            // quantities.setTimeseriesSize(counter.countTimeseries());
            // quantities.setStationsSize(counter.countStations());
            // } else {
            quantities.setPlatformsSize(getMapperFactory().getCounter().countPlatforms(serviceQuery));
            quantities.setDatasets(createDatasetCount(getMapperFactory().getCounter(), serviceQuery));

            // TODO
            quantities.setSamplingsSize(getMapperFactory().getCounter().countSamplings(serviceQuery));
            quantities.setMeasuringProgramsSize(getMapperFactory().getCounter().countMeasuringPrograms(serviceQuery));
            // }
            return quantities;
        } catch (DataAccessException e) {
            throw new InternalServerException("Could not count parameter entities.", e);
        }
    }

    private Map<String, Set<String>> getSupportedDatasets(ServiceOutput service) {
        Map<String, Set<String>> mimeTypesByDatasetTypes = new HashMap<>();
        for (String valueType : getMapperFactory().getIoFactoryCreator().getKnownTypes()) {
            try {
                IoHandlerFactory<?, ?> factory = getMapperFactory().getIoFactoryCreator().create(valueType);
                mimeTypesByDatasetTypes.put(valueType, factory.getSupportedMimeTypes());
            } catch (DatasetFactoryException e) {
                LOGGER.error("IO Factory for type '{}' couldn't be created.", valueType);
            }
        }
        return mimeTypesByDatasetTypes;
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

}
