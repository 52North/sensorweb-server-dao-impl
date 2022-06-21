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
package org.n52.sensorweb.server.db.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.n52.sensorweb.server.test.TestUtils.fromWkt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.io.ParseException;
import org.n52.io.request.Parameters;
import org.n52.sensorweb.server.db.TestBase;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.repositories.core.FeatureRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class FeatureQuerySpecificationsTest extends TestBase {

    @Autowired
    private FeatureRepository featureRepository;

    @Test
    @DisplayName("Filter datasets via bbox")
    public void given_datasetsWithGeometries_when_queryingWithMixedParameterList_then_returnedDatasetsAreFilteredProperly()
            throws ParseException {
        final DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1");
        final FeatureEntity f1 = testRepositories.persistSimpleFeature("f1", "format1");
        f1.setGeometry(fromWkt("SRID=4326;POINT (7.3 52.8)"));
        d1.setFeature(f1);

        Assertions.assertAll("No match when laying outside bbox", () -> {
            final DbQuery query = defaultQuery.replaceWith(Parameters.BBOX, "5, 50, 6, 51");
            final FeatureQuerySpecifications filterSpec = FeatureQuerySpecifications.of(query, entityManager);
            assertThat(featureRepository.findAll(filterSpec.matchesSpatially())).isEmpty();
        });

        final DatasetEntity d2 = quantityDataset("ph1", "of1", "pr1", "format1");
        final FeatureEntity f2 = testRepositories.persistSimpleFeature("f2", "format1");
        f2.setGeometry(fromWkt("SRID=4326;POINT (7.2 55)"));
        d2.setFeature(f2);

        Assertions.assertAll("Match when laying inside bbox", () -> {
            final DbQuery query = defaultQuery.replaceWith(Parameters.BBOX, "7, 52, 7.5, 53");
            final FeatureQuerySpecifications filterSpec = FeatureQuerySpecifications.of(query, entityManager);
            assertThat(featureRepository.findAll(filterSpec.matchesSpatially())).hasSize(1);
        });
    }

}
