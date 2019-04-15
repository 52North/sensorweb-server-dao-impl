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
package org.n52.series.db;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.series.db.OfferingRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class OfferingRepositoryTest {

    @Autowired
    private OfferingRepository repository;

    @Test
    public void given_emptyDatabase_when_findAllQuery_then_emptyCollection() {
        final List<OfferingEntity> allEntities = repository.findAll();
        Assertions.assertIterableEquals(allEntities, Collections.emptyList());
    }

    @Test
    public void given_emptyDatabase_when_saveOfferingWithRequiredMembersSet_then_entityGetsSavedProperly() {
        final OfferingEntity entity = new OfferingEntity();
        entity.setIdentifier("foo");

        final OfferingEntity savedEntity = repository.save(entity);

        assertAll("saving entity",
                  () -> assertNotNull(savedEntity, "saving entity returned null"),
                  () -> assertNotNull(savedEntity.getId(), "not id generated"),
                  () -> assertEquals("foo", savedEntity.getIdentifier()));
    }

    @Test
    public void given_persistentOffering_when_existsByIdentifier_then_entityGetsFound() {
        final OfferingEntity entity = new OfferingEntity();
        entity.setIdentifier("foo");

        repository.save(entity);
        assertAll("exists by identifier",
                  () -> assertTrue(repository.existsByIdentifier("foo"), "entity is not found by identifier"));
    }

    @SpringBootConfiguration
    @EnableJpaRepositories(basePackageClasses = OfferingRepository.class)
    static class Config extends TestRepositoryConfig<DatasetEntity> {
        public Config() {
            super("/mapping/core/persistence.xml");
        }

        @Override
        public TestRepositories testRepositories() {
            return new TestRepositories();
        }
    }
}
