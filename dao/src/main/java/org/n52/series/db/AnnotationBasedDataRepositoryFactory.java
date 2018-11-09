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

package org.n52.series.db;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.n52.io.response.dataset.AbstractValue;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.da.DataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class AnnotationBasedDataRepositoryFactory implements DataRepositoryTypeFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationBasedDataRepositoryFactory.class);

    private final Map<String, ? super DataRepository< ?, ?, ?, ? >> cache;

    private final ApplicationContext appContext;

    @Autowired
    public AnnotationBasedDataRepositoryFactory(ApplicationContext appContext) {
        this.cache = new HashMap<>();
        this.appContext = appContext;
    }

    private Stream<DataRepository<? extends DatasetEntity,
                                  ? extends DataEntity<?>,
                                  ? extends AbstractValue<?>,
                                  ?>> getAllDataAssemblers() {
        Map<String, Object> beansWithAnnotation = appContext.getBeansWithAnnotation(DataRepositoryComponent.class);
        Collection<Object> dataAssembleTypes = beansWithAnnotation.values();
        LOGGER.debug("Found following " + DataRepositoryComponent.class.getSimpleName() + ": {}",
                     dataAssembleTypes.stream()
                                      .map(it -> it.getClass().getSimpleName())
                                      .collect(Collectors.joining(", ")));
        return dataAssembleTypes.stream()
                                .filter(DataRepository.class::isInstance)
                                .map(DataRepository.class::cast);
    }

    @Override
    public boolean isKnown(String valueType) {
        return hasCacheEntry(valueType) || getAllDataAssemblers().map(this::getDataType)
                                                                 .filter(it -> it.equals(valueType))
                                                                 .findFirst()
                                                                 .isPresent();
    }

    private Optional<DataRepository<? extends DatasetEntity,
                                    ? extends DataEntity<?>,
                                    ? extends AbstractValue<?>,
                                    ?>> findDataAssembler(String type) {
        return getAllDataAssemblers().filter(it -> getDataType(it).equals(type)).findFirst();
    }

    @Override
    public Set<String> getKnownTypes() {
        return getAllDataAssemblers().map(this::getDataType)
                                     .collect(Collectors.toSet());
    }

    private String getDataType(DataRepository<? extends DatasetEntity,
                                              ? extends DataEntity<?>,
                                              ? extends AbstractValue<?>,
                                              ?> assembler) {
        return assembler.getClass().getAnnotation(DataRepositoryComponent.class).value();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends DatasetEntity,
            E extends DataEntity<T>,
            V extends AbstractValue< ? >,
            T> DataRepository<S, E, V, T> create(String type, Class<S> entityType) {
        return (DataRepository<S, E, V, T>) addToCache(type, findDataAssembler(type).orElseThrow(throwException(type)));
    }

    private <A extends DataRepository<? extends DatasetEntity,
                                      ? extends DataEntity<?>,
                                      ? extends AbstractValue<?>,
                                      ?>> A addToCache(String valueType, A assembler) {
        cache.put(valueType, assembler);
        return assembler;
    }

    private Supplier< ? extends DataAccessException> throwException(String type) {
        return () -> new DataAccessException("Unknown type: " + type);
    }

    @Override
    public Class< ? extends DatasetEntity> getDatasetEntityType(String valueType) {
        return findDataAssembler(valueType).map(Object::getClass)
                                           .map(it -> it.getAnnotation(DataRepositoryComponent.class))
                                           .map(DataRepositoryComponent::datasetEntityType)
                                           .get();
    }

    @Override
    public boolean hasCacheEntry(String valueType) {
        return cache.containsKey(valueType);
    }

}