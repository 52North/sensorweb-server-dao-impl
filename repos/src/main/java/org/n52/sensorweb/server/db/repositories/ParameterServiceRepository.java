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
package org.n52.sensorweb.server.db.repositories;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ParameterServiceRepository<T extends DescribableEntity>
        extends ParameterDataRepository<T>, NameRepository<T> {

    default Optional<T> findByIdentifierAndService(T entity) {
        return findOne(createExample(entity, createMatcher()));
    }

    @Override
    default T getInstance(T entity) {
        return findByIdentifierAndService(createIdentifierServiceExample(entity)).orElse(null);
    }

    default Example<T> createExample(T entity, ExampleMatcher matcher) {
        return Example.<T> of(entity, matcher);
    }

    default ExampleMatcher createMatcher() {
        return ExampleMatcher.matching().withIgnorePaths(getMatcherIgnorePaths().toArray(new String[0]))
                .withMatcher(DescribableEntity.PROPERTY_IDENTIFIER, GenericPropertyMatchers.exact())
                .withMatcher(DescribableEntity.PROPERTY_SERVICE + "." + DescribableEntity.PROPERTY_IDENTIFIER,
                        GenericPropertyMatchers.exact());
    }

    default Set<String> getMatcherIgnorePaths() {
        Set<String> paths = new HashSet<>();
        paths.add(DescribableEntity.PROPERTY_ID);
        paths.add(DescribableEntity.STA_IDENTIFIER);
        return paths;
    }

    default <T extends DescribableEntity> T createIdentifierServiceExample(T entity) {
        try {
            DescribableEntity example = entity.getClass().newInstance();
            example.setIdentifier(entity.getIdentifier());
            example.setService(entity.getService());
            return (T) example;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
            return entity;
        }
    }

    List<T> findByService(ServiceEntity service);

    default List<T> findByService(T entity, ServiceEntity service) {
        entity.setService(service);
        return findAll(createExample(entity, createMatcher()));
    }

    default void deleteByService(ServiceEntity service) {
        deleteAll(findByService(service));
    }

}
