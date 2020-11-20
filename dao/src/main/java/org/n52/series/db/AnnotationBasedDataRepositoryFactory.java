/*
 * Copyright (C) 2015-2020 52°North Initiative for Geospatial Open Source
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.n52.io.response.dataset.AbstractValue;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.dataset.ObservationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class AnnotationBasedDataRepositoryFactory implements DataRepositoryTypeFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationBasedDataRepositoryFactory.class);

    private final Map<String, ? super ValueAssembler<?, ?, ?>> cache;

    private final ApplicationContext appContext;

    @Autowired
    public AnnotationBasedDataRepositoryFactory(ApplicationContext appContext) {
        this.cache = new HashMap<>();
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
    public boolean isKnown(String observationType, String valueType) {
        return hasCacheEntry(observationType, valueType) || getAllDataAssemblers().map(this::getDataType)
                .filter(it -> it.equals(getType(observationType, valueType))).findFirst().isPresent();
    }

    private String getType(String observationType, String valueType) {
        return (observationType != null && !observationType.isEmpty()
                && !observationType.equalsIgnoreCase(ObservationType.simple.name()))
                        ? valueType + "-" + observationType
                        : valueType;
    }

    private Optional<ValueAssembler<? extends DataEntity<?>, ? extends AbstractValue<?>, ?>> findDataAssembler(
            String observationType, String valueType) {
        return getAllDataAssemblers().filter(it -> getDataType(it).equals(getType(observationType, valueType)))
                .findFirst();
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
    public <E extends DataEntity<T>, V extends AbstractValue<?>, T> ValueAssembler<E, V, T> create(
            String observationType, String valueType, Class<?> entityType) {
        Optional<ValueAssembler<? extends DataEntity<?>, ? extends AbstractValue<?>, ?>> assembler =
                findDataAssembler(observationType, valueType);
        return assembler.isPresent()
                ? addToCache(observationType, valueType,
                        (ValueAssembler<DataEntity<T>, AbstractValue<?>, T>) assembler.get())
                : null;
    }

    @SuppressWarnings("unchecked")
    private <E extends DataEntity<T>, V extends AbstractValue<?>, T> ValueAssembler<E, V, T> addToCache(
            String observationType, String valueType, ValueAssembler<DataEntity<T>, AbstractValue<?>, T> assembler) {
        cache.put(getType(observationType, valueType), assembler);
        return (ValueAssembler<E, V, T>) assembler;
    }

    @Override
    public Class<? extends DatasetEntity> getDatasetEntityType(String observationType, String valueType) {
        return findDataAssembler(observationType, valueType).map(Object::getClass)
                .map(it -> it.getAnnotation(ValueAssemblerComponent.class))
                .map(ValueAssemblerComponent::datasetEntityType).get();
    }

    @Override
    public boolean hasCacheEntry(String observationType, String valueType) {
        return cache.containsKey(getType(observationType, valueType));
    }

    //
    // private static final Logger LOGGER =
    // LoggerFactory.getLogger(AnnotationBasedDataRepositoryFactory.class);
    //
    // private final ApplicationContext applicationContext;
    //
    // private final Map<String, ? super ValueAssembler< ?, ?, ?, ? >> cache;
    //
    // public AnnotationBasedDataRepositoryFactory(ApplicationContext
    // applicationContext) {
    // this.applicationContext = applicationContext;
    // this.cache = new HashMap<>();
    // }
    //
    // private Stream<ValueAssembler<? extends DatasetEntity, ? extends
    // DataEntity<?>, ? extends AbstractValue<?>, ?>> getAllDataAssemblers() {
    // Map<String, Object> beansWithAnnotation =
    // applicationContext.getBeansWithAnnotation(ValueAssemblerComponent.class);
    // Collection<Object> dataAssembleTypes = beansWithAnnotation.values();
    // LOGGER.debug("Found following " +
    // ValueAssemblerComponent.class.getSimpleName() + ": {}",
    // dataAssembleTypes.stream()
    // .map(it -> it.getClass().getSimpleName())
    // .collect(Collectors.joining(", ")));
    // return dataAssembleTypes.stream()
    // .filter(ValueAssembler.class::isInstance)
    // .map(ValueAssembler.class::cast);
    // }
    //
    // @Override
    // public boolean isKnown(String valueType) {
    // return hasCacheEntry(valueType) ||
    // getAllDataAssemblers().map(this::getDataType)
    // .filter(it -> it.equals(valueType))
    // .findFirst()
    // .isPresent();
    // }
    //
    // @Override
    // public boolean isKnown(String observationType, String valueType) {
    // return hasCacheEntry(observationType, valueType) ||
    // getAllDataAssemblers().map(this::getDataType)
    // .filter(it -> it.equals(getType(observationType,
    // valueType))).findFirst().isPresent();
    // }
    //
    // @Override
    // public Class<? extends DatasetEntity> getDatasetEntityType(String
    // observationType, String valueType) {
    // // TODO Auto-generated method stub
    // return null;
    // }
    //
    // private Optional<ValueAssembler<? extends DatasetEntity, ? extends
    // DataEntity<?>, ? extends AbstractValue<?>, ?>> findDataAssembler(String
    // type) {
    // return getAllDataAssemblers().filter(it ->
    // getDataType(it).equals(type)).findFirst();
    // }
    //
    // @Override
    // public Set<String> getKnownTypes() {
    // return getAllDataAssemblers().map(this::getDataType)
    // .collect(Collectors.toSet());
    // }
    //
    // private String getDataType(ValueAssembler<? extends DatasetEntity, ?
    // extends DataEntity<?>, ? extends AbstractValue<?>, ?> it) {
    // return
    // it.getClass().getAnnotation(ValueAssemblerComponent.class).value();
    // }
    //
    //
    // @Override
    // @SuppressWarnings("unchecked")
    // public <S extends DatasetEntity, E extends DataEntity<T>, V extends
    // AbstractValue< ? >, T> ValueAssembler<S, E, V, T> create(String
    // valueTypem, Class<?> type) throws DatasetFactoryException {
    // return (ValueAssembler<S, E, V, T>) addToCache(type,
    // findDataAssembler(type).orElseThrow(throwAccessException(type)));
    // }
    //
    // @Override
    // public ValueAssembler<?, ?, ?, ?> create(String valueType, Class<?>
    // entityType) throws DatasetFactoryException {
    // // TODO Auto-generated method stub
    // return null;
    // }
    //
    // private <A extends ValueAssembler<? extends DatasetEntity, ? extends
    // DataEntity<?>, ? extends AbstractValue<?>, ?>> A addToCache(String
    // valueType, A assembler) {
    // cache.put(valueType, assembler);
    // return assembler;
    // }
    //
    // @Override
    // public Class< ? extends DatasetEntity> getDatasetEntityType(String
    // valueType) {
    // return findDataAssembler(valueType).map(Object::getClass)
    // .map(it -> it.getAnnotation(ValueAssemblerComponent.class))
    // .map(ValueAssemblerComponent::datasetEntityType)
    // .get();
    // }
    //
    // @Override
    // public boolean hasCacheEntry(String valueType) {
    // return cache.containsKey(valueType);
    // }

}
