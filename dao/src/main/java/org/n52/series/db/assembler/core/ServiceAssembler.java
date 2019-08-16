/*
 * Copyright (C) 2015-2019 52°North Initiative for Geospatial Open Source
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
package org.n52.series.db.assembler.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
import org.n52.series.db.assembler.ClearAssembler;
import org.n52.series.db.assembler.InsertAssembler;
import org.n52.series.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.n52.series.db.old.dao.DefaultDbQueryFactory;
import org.n52.series.db.query.DatasetQuerySpecifications;
import org.n52.series.db.query.ServiceQuerySpecifications;
import org.n52.series.db.repositories.ParameterDataRepository;
import org.n52.series.db.repositories.core.DatasetRepository;
import org.n52.series.db.repositories.core.ServiceRepository;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.spi.search.ServiceSearchResult;
import org.n52.series.srv.OutputAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.StreamUtils;
import org.springframework.stereotype.Component;

@Component
public class ServiceAssembler
        implements OutputAssembler<ServiceOutput>, InsertAssembler<ServiceEntity>, ClearAssembler<ServiceEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceAssembler.class);

    private static final String SERVICE_TYPE = "Restful series access layer.";

    @PersistenceContext
    private EntityManager entityManager;

    private final ServiceRepository serviceRepository;

    private final DatasetRepository datasetRepository;

    private final EntityCounter counter;

    private final DbQueryFactory dbQueryFactory;

    private final DefaultIoFactory<DatasetOutput<AbstractValue<?>>, AbstractValue<?>> ioFactoryCreator;

    // via config
    @Autowired(required = false)
    private ServiceEntity serviceEntity;

    public ServiceAssembler(final ServiceRepository serviceRepository, final DatasetRepository datasetRepository,
            final EntityCounter entityCounter, final DbQueryFactory dbQueryFactory,
            DefaultIoFactory<DatasetOutput<AbstractValue<?>>, AbstractValue<?>> ioFactoryCreator) {
        this.serviceRepository = serviceRepository;
        this.datasetRepository = datasetRepository;
        this.counter = entityCounter;
        this.dbQueryFactory = dbQueryFactory == null ? new DefaultDbQueryFactory() : dbQueryFactory;
        this.ioFactoryCreator = ioFactoryCreator;
    }

    @Override
    public ParameterDataRepository<ServiceEntity> getParameterRepository() {
        return serviceRepository;
    }

    @Override
    public List<ServiceOutput> getAllCondensed(final DbQuery query) {
        final ParameterOutputSearchResultMapper mapper = new ParameterOutputSearchResultMapper(query);
        return findAll(query).map(it -> mapper.createCondensed(it, prepareEmptyOutput())).collect(Collectors.toList());
    }

    @Override
    public List<ServiceOutput> getAllExpanded(final DbQuery query) {
        return findAll(query).map(it -> createExpanded(it, query)).collect(Collectors.toList());
    }

    @Override
    public ServiceOutput getInstance(final String id, final DbQuery query) {
        final Specification<ServiceEntity> publicEntity = createPublicPredicate(id, query);
        final Optional<ServiceEntity> entity = getParameterRepository().findOne(publicEntity);
        return entity.map(it -> createExpanded(it, query)).orElse(createExpanded(serviceEntity, query));
    }

    @Override
    public Collection<SearchResult> searchFor(final DbQuery query) {
        final ParameterOutputSearchResultMapper mapper = new ParameterOutputSearchResultMapper(query);
        return findAll(query).map(it -> mapper.createSearchResult(it, new ServiceSearchResult()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(final String id, final DbQuery query) {
        return getParameterRepository().exists(createPublicPredicate(id, query)) || serviceEntity != null;
    }

    @Override
    public void clearUnusedForService(ServiceEntity service) {
        serviceRepository.delete(service);
    }

    public Specification<ServiceEntity> createPublicPredicate(final String id, DbQuery query) {
        final DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query, entityManager);
        final Specification<DatasetEntity> datasetPredicate =
                dsFilterSpec.matchFeatures(id).and(dsFilterSpec.isPublic());
        ServiceQuerySpecifications filterSpec = ServiceQuerySpecifications.of(query);
        return filterSpec.selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }

    protected Specification<ServiceEntity> createFilterPredicate(DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query, entityManager);
        ServiceQuerySpecifications fFilterSpec = ServiceQuerySpecifications.of(query);
        return fFilterSpec.selectFrom(dsFilterSpec.matchFilters());
    }

    private ServiceOutput createExpanded(ServiceEntity entity, DbQuery query) {
        if (entity != null) {
            final ParameterOutputSearchResultMapper mapper = new ParameterOutputSearchResultMapper(query);
            ServiceOutput result = mapper.createCondensed(entity, prepareEmptyOutput());
            IoParameters parameters = query.getParameters();
            ParameterCount quantities = countParameters(result, query);
            boolean supportsFirstLatest = entity.getSupportsFirstLast();

            String serviceUrl = entity.getUrl();
            String type = getServiceType(entity);

            result.setValue(ServiceOutput.SERVICE_URL, serviceUrl, parameters, result::setServiceUrl);
            result.setValue(ServiceOutput.TYPE, type, parameters, result::setType);

            // if (parameters.shallBehaveBackwardsCompatible()) {
            // result.setValue(ServiceOutput.VERSION, "1.0.0", parameters,
            // result::setVersion);
            // result.setValue(ServiceOutput.QUANTITIES, quantities, parameters,
            // result::setQuantities);
            // result.setValue(ServiceOutput.SUPPORTS_FIRST_LATEST,
            // supportsFirstLatest,
            // parameters,
            // result::setSupportsFirstLatest);
            // } else {
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
        }
        return null;
    }

    private Stream<ServiceEntity> findAll(final DbQuery query) {
        final Specification<ServiceEntity> predicate = createFilterPredicate(query);
        Iterable<ServiceEntity> entities = getParameterRepository().findAll(predicate);
        if ((entities == null || !entities.iterator().hasNext()) && serviceEntity != null) {
            LinkedHashSet<ServiceEntity> set = new LinkedHashSet<>();
            set.add(serviceEntity);
            entities = set;
        }
        return StreamUtils.createStreamFromIterator(entities.iterator());
    }

    private ServiceOutput prepareEmptyOutput() {
        return new ServiceOutput();
    }

    private String getServiceType(ServiceEntity entity) {
        return entity.getType() != null ? entity.getType() : SERVICE_TYPE;
    }

    private ParameterCount countParameters(ServiceOutput service, DbQuery query) {
        IoParameters parameters = query.getParameters();
        ParameterCount quantities = new ServiceOutput.ParameterCount();
        DbQuery serviceQuery = getDbQuery(parameters.extendWith(IoParameters.SERVICES, service.getId())
                .removeAllOf("offset").removeAllOf("limit"));
        quantities.setOfferingsSize(counter.countOfferings(serviceQuery));
        quantities.setProceduresSize(counter.countProcedures(serviceQuery));
        quantities.setCategoriesSize(counter.countCategories(serviceQuery));
        quantities.setPhenomenaSize(counter.countPhenomena(serviceQuery));
        quantities.setFeaturesSize(counter.countFeatures(serviceQuery));

        // if (parameters.shallBehaveBackwardsCompatible()) {
        // quantities.setTimeseriesSize(counter.countTimeseries());
        // quantities.setStationsSize(counter.countStations());
        // } else {
        quantities.setPlatformsSize(counter.countPlatforms(serviceQuery));
        quantities.setDatasets(createDatasetCount(counter, serviceQuery));

        // TODO
        quantities.setSamplingsSize(counter.countSamplings(serviceQuery));
        quantities.setMeasuringProgramsSize(counter.countMeasuringPrograms(serviceQuery));
        // }
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

    private DbQuery getDbQuery(IoParameters parameters) {
        return dbQueryFactory.createFrom(parameters);
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

}
