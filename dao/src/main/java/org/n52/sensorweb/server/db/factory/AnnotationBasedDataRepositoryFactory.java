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
package org.n52.sensorweb.server.db.factory;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.n52.io.response.dataset.AbstractValue;
import org.n52.sensorweb.server.db.ValueAssembler;
import org.n52.sensorweb.server.db.ValueAssemblerComponent;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.dataset.DatasetType;
import org.n52.series.db.beans.dataset.ObservationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class AnnotationBasedDataRepositoryFactory implements DataRepositoryTypeFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationBasedDataRepositoryFactory.class);

    private final Map<String, ? super ValueAssembler<?, ?, ?>> cache;
    private final Set<String> valueTypes;

    private final ApplicationContext appContext;

    @Inject
    public AnnotationBasedDataRepositoryFactory(ApplicationContext appContext) {
        this.cache = new HashMap<>();
        this.valueTypes = new LinkedHashSet<>();
        this.appContext = appContext;
    }

    private Stream<ValueAssembler<? extends DataEntity<?>, ? extends AbstractValue<?>, ?>> getAllDataAssemblers() {
        Map<String, Object> beansWithAnnotation = appContext.getBeansWithAnnotation(ValueAssemblerComponent.class);
        Collection<Object> dataAssembleTypes = beansWithAnnotation.values();
        LOGGER.trace("Found following " + ValueAssemblerComponent.class.getSimpleName() + ": {}",
                dataAssembleTypes.stream().map(it -> it.getClass().getSimpleName()).collect(Collectors.joining(", ")));
        return dataAssembleTypes.stream().filter(ValueAssembler.class::isInstance).map(ValueAssembler.class::cast);
    }

    @Override
    public boolean isKnown(String datasetType, String observationType, String valueType) {
        return hasCacheEntry(datasetType, observationType, valueType) || getAllDataAssemblers().map(this::getDataType)
                .filter(it -> it.equals(getType(datasetType, observationType, valueType))).findFirst().isPresent();
    }

    private String getType(String datasetType, String observationType, String valueType) {
        return (datasetType != null && !datasetType.isEmpty()
                && datasetType.equalsIgnoreCase(DatasetType.trajectory.name()))
                        ? valueType + "-" + datasetType
                        : (observationType != null && !observationType.isEmpty()
                                && !observationType.equalsIgnoreCase(ObservationType.simple.name()))
                                        ? valueType + "-" + observationType
                                        : valueType;
    }

    private Optional<ValueAssembler<? extends DataEntity<?>, ? extends AbstractValue<?>, ?>> findDataAssembler(
            String datasetType, String observationType, String valueType) {
        return getAllDataAssemblers()
                .filter(it -> getDataType(it).equals(getType(datasetType, observationType, valueType))).findFirst();
    }

    @Override
    public Set<String> getKnownTypes() {
        return getAllDataAssemblers().map(this::getDataType).collect(Collectors.toSet());
    }

    private String getDataType(ValueAssembler<? extends DataEntity<?>, ? extends AbstractValue<?>, ?> assembler) {
        return assembler.getClass().getAnnotation(ValueAssemblerComponent.class).value();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends DataEntity<T>, V extends AbstractValue<?>, T> ValueAssembler<E, V, T> create(String datasetType,
            String observationType, String valueType, Class<?> entityType) {
        Optional<ValueAssembler<? extends DataEntity<?>, ? extends AbstractValue<?>, ?>> assembler =
                findDataAssembler(datasetType, observationType, valueType);
        return assembler.isPresent()
                ? addToCache(datasetType, observationType, valueType,
                        (ValueAssembler<DataEntity<T>, AbstractValue<?>, T>) assembler.get())
                : null;
    }

    @SuppressWarnings("unchecked")
    private <E extends DataEntity<T>, V extends AbstractValue<?>, T> ValueAssembler<E, V, T> addToCache(
            String datasetType, String observationType, String valueType,
            ValueAssembler<DataEntity<T>, AbstractValue<?>, T> assembler) {
        cache.put(getType(datasetType, observationType, valueType), assembler);
        return (ValueAssembler<E, V, T>) assembler;
    }

    @Override
    public Class<? extends DatasetEntity> getDatasetEntityType(String datasetType, String observationType,
            String valueType) {
        return findDataAssembler(datasetType, observationType, valueType).map(Object::getClass)
                .map(it -> it.getAnnotation(ValueAssemblerComponent.class))
                .map(ValueAssemblerComponent::datasetEntityType).get();
    }

    @Override
    public boolean hasCacheEntry(String datasetType, String observationType, String valueType) {
        return cache.containsKey(getType(datasetType, observationType, valueType));
    }

    @Override
    public Set<String> getValueTypes() {
        if (valueTypes.isEmpty()) {
            getAllDataAssemblers().map(this::getDataType).forEach(type -> {
                if (type.contains("-")) {
                    valueTypes.add(type.split("-")[0]);
                } else {
                    valueTypes.add(type);
                }
            });
        }
        return valueTypes;
    }

}
