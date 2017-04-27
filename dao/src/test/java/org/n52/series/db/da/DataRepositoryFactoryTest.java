/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
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

package org.n52.series.db.da;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.n52.io.DatasetFactoryException;
import org.n52.io.response.dataset.count.CountDatasetOutput;
import org.n52.io.response.dataset.quantity.QuantityDatasetOutput;
import org.n52.io.response.dataset.text.TextDatasetOutput;

public class DataRepositoryFactoryTest {

    private IDataRepositoryFactory factory;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws URISyntaxException {
        File config = getConfigFile("dataset-factory.properties");
        factory = new DefaultDataRepositoryFactory(config);
    }

    @Test
    public void when_createdWithNoConfig_useDefaultConfig() throws DatasetFactoryException {
        IDataRepositoryFactory m = new DefaultDataRepositoryFactory();
        assertFalse(m.isKnown(TextDatasetOutput.VALUE_TYPE));
        assertFalse(m.isKnown(CountDatasetOutput.VALUE_TYPE));
        assertTrue(m.create(QuantityDatasetOutput.VALUE_TYPE)
                    .getClass() == QuantityDataRepository.class);
    }

    @Test
    public void when_mapToText_then_returnTextDataRepository() throws DatasetFactoryException {
        assertTrue(factory.create(TextDatasetOutput.VALUE_TYPE)
                          .getClass() == TextDataRepository.class);
    }

    @Test
    public void when_mapToText_then_returnCountDataRepository() throws DatasetFactoryException {
        assertTrue(factory.create(CountDatasetOutput.VALUE_TYPE)
                          .getClass() == CountDataRepository.class);
    }

    @Test
    public void when_mapToText_then_returnQuantityDataRepository() throws DatasetFactoryException {
        assertTrue(factory.create(QuantityDatasetOutput.VALUE_TYPE)
                          .getClass() == QuantityDataRepository.class);
    }

    @Test
    public void when_instanceCreated_then_nextTimeFromCache() throws DatasetFactoryException {
        DataRepository instance = factory.create(QuantityDatasetOutput.VALUE_TYPE);
        Assert.assertTrue(factory.hasCacheEntry(QuantityDatasetOutput.VALUE_TYPE));
        Assert.assertTrue(instance == factory.create(QuantityDatasetOutput.VALUE_TYPE));
    }

    private File getConfigFile(String name) throws URISyntaxException {
        Path root = Paths.get(getClass().getResource("/files")
                                        .toURI());
        return root.resolve(name)
                   .toFile();
    }

}
