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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.n52.io.handler.DefaultIoFactory;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.sensorweb.server.db.assembler.core.EntityCounter;
import org.n52.sensorweb.server.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.AbstractDao;
import org.n52.series.db.old.dao.SearchableDao;
import org.n52.series.db.old.dao.ServiceDao;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.spi.search.ServiceSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Component
public class ServiceAssembler extends ParameterAssembler<ServiceEntity, ServiceOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceAssembler.class);

    private static final String SERVICE_TYPE = "Restful series access layer.";

    private final EntityCounter counter;

    private final DefaultIoFactory<DatasetOutput<AbstractValue<?>>, AbstractValue<?>> ioFactoryCreator;

    public ServiceAssembler(EntityCounter counter,
            DefaultIoFactory<DatasetOutput<AbstractValue<?>>, AbstractValue<?>> ioFactoryCreator,
            HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
        this.counter = counter;
        this.ioFactoryCreator = ioFactoryCreator;
    }

    @Override
    protected ServiceOutput prepareEmptyParameterOutput() {
        return new ServiceOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new ServiceSearchResult().setId(id).setLabel(label).setBaseUrl(baseUrl);
    }

    @Override
    protected ServiceDao createDao(Session session) {
        return new ServiceDao(session);
    }

    @Override
    protected SearchableDao<ServiceEntity> createSearchableDao(Session session) {
        return new ServiceDao(session);
    }

    @Override
    public boolean exists(String id, DbQuery parameters) {
        Session session = getSession();
        try {
            Long rawId = parseId(id);
            ServiceDao dao = createDao(session);
            return isConfiguredServiceInstance(rawId) || dao.hasInstance(rawId, parameters);
        } finally {
            returnSession(session);
        }
    }

    private boolean isConfiguredServiceInstance(Long id) {
        return (getServiceEntity() != null) && getServiceEntity().getId().equals(id);
    }

    @Override
    public Collection<SearchResult> searchFor(DbQuery query) {
        /*
         * final ServiceSearchResult result = new ServiceSearchResult(serviceInfo.getServiceId(),
         * serviceInfo.getServiceDescription()); String queryString =
         * DbQuery.createFrom(parameters).getSearchTerm(); return
         * serviceInfo.getServiceDescription().contains(queryString) ?
         * Collections.<SearchResult>singletonList(result)ServiceRepository :
         * Collections.<SearchResult>emptyList(); Session session = getSession(); try { ServiceDao serviceDao
         * = createDao(session); DbQuery query = getDbQuery(parameters); List<ServiceEntity> found =
         * serviceDao.find(query); return convertToSearchResults(found, query); } finally {
         * returnSession(session); }
         */
        // TODO implement search
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    protected List<ServiceEntity> getAllInstances(DbQuery parameters, Session session) {
        return getServiceEntity() != null ? Collections.singletonList(getServiceEntity())
                : createDao(session).getAllInstances(parameters);
    }

    @Override
    protected Optional<ServiceEntity> getEntity(Long id, AbstractDao<ServiceEntity> dao, DbQuery query) {
        ServiceEntity result = !isConfiguredServiceInstance(id) ? dao.getInstance(id, query) : getServiceEntity();
        if (result == null) {
            LOGGER.debug("Resource with id '" + id + "' could not be found.");
        }
        return Optional.ofNullable(result);
    }

    @Override
    protected ServiceOutput createExpanded(ServiceEntity entity, DbQuery query, Session session) {
        return getMapperFactory().getServiceMapper(query).createExpanded(entity);
        // ServiceOutput result = getCondensedService(entity, query);
        // IoParameters parameters = query.getParameters();
        //
        // ParameterCount quantities = countParameters(result, query);
        // boolean supportsFirstLatest = entity.getSupportsFirstLast();
        //
        // String serviceUrl = entity.getUrl();
        // String type = getServiceType(entity);
        //
        // result.setValue(ServiceOutput.SERVICE_URL, serviceUrl, parameters, result::setServiceUrl);
        // result.setValue(ServiceOutput.TYPE, type, parameters, result::setType);
        //
        // // if (parameters.shallBehaveBackwardsCompatible()) {
        // // result.setValue(ServiceOutput.VERSION, "1.0.0", parameters,
        // // result::setVersion);
        // // result.setValue(ServiceOutput.QUANTITIES, quantities, parameters,
        // // result::setQuantities);
        // // result.setValue(ServiceOutput.SUPPORTS_FIRST_LATEST,
        // // supportsFirstLatest,
        // // parameters,
        // // result::setSupportsFirstLatest);
        // // } else {
        // Map<String, Object> features = new HashMap<>();
        // features.put(ServiceOutput.QUANTITIES, quantities);
        // features.put(ServiceOutput.SUPPORTS_FIRST_LATEST, supportsFirstLatest);
        // features.put(ServiceOutput.SUPPORTED_MIME_TYPES, getSupportedDatasets(result));
        //
        // String version = (entity.getVersion() != null) ? entity.getVersion() : "2.0";
        //
        // String hrefBase = query.getHrefBase();
        // result.setValue(ServiceOutput.VERSION, version, parameters, result::setVersion);
        // result.setValue(ServiceOutput.FEATURES, features, parameters, result::setFeatures);
        // result.setValue(ParameterOutput.HREF_BASE, hrefBase, parameters, result::setHrefBase);
        // // }
        // return result;
    }
    //
    // private String getServiceType(ServiceEntity entity) {
    // return entity.getType() != null ? entity.getType() : SERVICE_TYPE;
    // }
    //
    // private Map<String, Set<String>> getSupportedDatasets(ServiceOutput service) {
    // Map<String, Set<String>> mimeTypesByDatasetTypes = new HashMap<>();
    // for (String valueType : ioFactoryCreator.getKnownTypes()) {
    // try {
    // IoHandlerFactory<?, ?> factory = ioFactoryCreator.create(valueType);
    // mimeTypesByDatasetTypes.put(valueType, factory.getSupportedMimeTypes());
    // } catch (DatasetFactoryException e) {
    // LOGGER.error("IO Factory for type '{}' couldn't be created.", valueType);
    // }
    // }
    // return mimeTypesByDatasetTypes;
    // }
    //
    // private ParameterCount countParameters(ServiceOutput service, DbQuery query) {
    // try {
    // IoParameters parameters = query.getParameters();
    // ParameterCount quantities = new ServiceOutput.ParameterCount();
    // DbQuery serviceQuery = getDbQuery(parameters.extendWith(Parameters.SERVICES, service.getId())
    // .removeAllOf("offset").removeAllOf("limit"));
    // quantities.setOfferingsSize(counter.countOfferings(serviceQuery));
    // quantities.setProceduresSize(counter.countProcedures(serviceQuery));
    // quantities.setCategoriesSize(counter.countCategories(serviceQuery));
    // quantities.setPhenomenaSize(counter.countPhenomena(serviceQuery));
    // quantities.setFeaturesSize(counter.countFeatures(serviceQuery));
    //
    // // if (parameters.shallBehaveBackwardsCompatible()) {
    // // quantities.setTimeseriesSize(counter.countTimeseries());
    // // quantities.setStationsSize(counter.countStations());
    // // } else {
    // quantities.setPlatformsSize(counter.countPlatforms(serviceQuery));
    // quantities.setDatasets(createDatasetCount(counter, serviceQuery));
    //
    // // TODO
    // quantities.setSamplingsSize(counter.countSamplings(serviceQuery));
    // quantities.setMeasuringProgramsSize(counter.countMeasuringPrograms(serviceQuery));
    // // }
    // return quantities;
    // } catch (DataAccessException e) {
    // throw new InternalServerException("Could not count parameter entities.", e);
    // }
    // }
    //
    // private DatasetCount createDatasetCount(EntityCounter counter, DbQuery query) {
    // DatasetCount datasetCount = new DatasetCount();
    // datasetCount.setTotalAmount(counter.countDatasets(query));
    // datasetCount.setAmountTimeseries(counter.countTimeseries(query));
    // datasetCount.setAmountIndividualObservations(counter.countIndividualObservations(query));
    // datasetCount.setAmountProfiles(counter.countProfiles(query));
    // datasetCount.setAmountTrajectories(counter.countTrajectories(query));
    // return datasetCount;
    // }

    @Override
    protected ParameterOutputSearchResultMapper<ServiceEntity, ServiceOutput> getOutputMapper(DbQuery query) {
        // TODO Auto-generated method stub
        return null;
    }

}
