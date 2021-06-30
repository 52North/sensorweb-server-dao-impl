/*
 * Copyright (C) 2015-2021 52°North Spatial Information Research GmbH
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
package org.n52.sensorweb.server.db.assembler;

import static org.n52.sensorweb.server.test.FeatureBuilder.newFeature;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.io.response.FeatureOutput;
import org.n52.sensorweb.server.db.assembler.core.FeatureAssembler;
import org.n52.sensorweb.server.db.repositories.core.FeatureRepository;
import org.n52.sensorweb.server.test.FeatureBuilder;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.FormatEntity;
import org.n52.series.db.beans.UnitEntity;
import org.n52.series.db.beans.parameter.ParameterEntity;
import org.n52.series.db.beans.parameter.feature.FeatureComplexParameterEntity;
import org.n52.series.db.beans.parameter.feature.FeatureCountParameterEntity;
import org.n52.series.db.beans.parameter.feature.FeatureParameterEntity;
import org.n52.series.db.beans.parameter.feature.FeatureQuantityParameterEntity;
import org.n52.shetland.ogc.OGCConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class FeatureAssemblerTest extends AbstractAssemblerTest {

    @Autowired
    private FeatureRepository repository;

    @Autowired
    private FeatureAssembler assembler;

    @Test
    public void insert_feature_with_simple_parameter() {
        final FormatEntity formatEntity = new FormatEntity();
        formatEntity.setFormat(OGCConstants.UNKNOWN);
        final FeatureBuilder builder = newFeature("feature_param");
        final FeatureEntity entity = builder.setFormat(formatEntity).build();
        FeatureQuantityParameterEntity parameter = new FeatureQuantityParameterEntity();
        parameter.setName("param_name");
        parameter.setValue(new BigDecimal("1.0"));
        parameter.setFeature(entity);
        UnitEntity unit = new UnitEntity();
        unit.setIdentifier("m");
        parameter.setUnit(unit);
        entity.addParameter(parameter);

        AbstractFeatureEntity inserted = assembler.getOrInsertInstance(entity);
        Assertions.assertNotNull(inserted);

        AbstractFeatureEntity feature = repository.getById(inserted.getId());
        Assertions.assertNotNull(feature);
        Assertions.assertTrue(feature.hasParameters());

        Assertions.assertEquals(1, feature.getParameters().size());
        ParameterEntity<?> param = feature.getParameters().iterator().next();
        Assertions.assertNotNull(param);
        Assertions.assertTrue(param instanceof FeatureQuantityParameterEntity);
        Assertions.assertNotNull(((FeatureQuantityParameterEntity) param).getUnit());

        repository.delete(feature);
    }

    @Test
    public void insert_feature_with_complex_parameter() {
        final FormatEntity formatEntity = new FormatEntity();
        formatEntity.setFormat(OGCConstants.UNKNOWN);
        final FeatureBuilder builder = newFeature("feature_param_2");
        final FeatureEntity entity = builder.setFormat(formatEntity).build();
        entity.addParameter(getComplex(entity));
        AbstractFeatureEntity inserted = assembler.getOrInsertInstance(entity);
        Assertions.assertNotNull(inserted);

        AbstractFeatureEntity feature = repository.getById(inserted.getId());
        Assertions.assertNotNull(feature);
        Assertions.assertTrue(feature.hasParameters());

        Assertions.assertEquals(1, feature.getParameters().size());
        ParameterEntity<?> param = feature.getParameters().iterator().next();
        Assertions.assertNotNull(param);
        Assertions.assertTrue(param instanceof FeatureComplexParameterEntity);
        FeatureComplexParameterEntity complex = (FeatureComplexParameterEntity) param;
        Assertions.assertNotNull(complex.getValue());
        Assertions.assertEquals(2, complex.getValue().size());

        repository.delete(feature);
    }

    @Test
    public void insert_feature_with_simple_and_complex_parameter() {
        final FormatEntity formatEntity = new FormatEntity();
        formatEntity.setFormat(OGCConstants.UNKNOWN);
        final FeatureBuilder builder = newFeature("feature_param_3");
        final FeatureEntity entity = builder.setFormat(formatEntity).build();
        entity.addParameter(getComplex(entity));

        FeatureQuantityParameterEntity parameter = new FeatureQuantityParameterEntity();
        parameter.setName("param_quantity");
        parameter.setValue(new BigDecimal("2.0"));
        parameter.setFeature(entity);
        entity.addParameter(parameter);
        AbstractFeatureEntity inserted = assembler.getOrInsertInstance(entity);
        Assertions.assertNotNull(inserted);

        AbstractFeatureEntity feature = repository.getById(inserted.getId());
        Assertions.assertNotNull(feature);
        Assertions.assertTrue(feature.hasParameters());

        Assertions.assertEquals(2, feature.getParameters().size());
        Iterator<ParameterEntity<?>> iterator = feature.getParameters().iterator();
        ParameterEntity<?> param = iterator.next();
        Assertions.assertNotNull(param);
        Assertions.assertTrue(param instanceof FeatureComplexParameterEntity);
        FeatureComplexParameterEntity complex = (FeatureComplexParameterEntity) param;
        Assertions.assertNotNull(complex.getValue());
        Assertions.assertEquals(2, complex.getValue().size());

        param = iterator.next();
        Assertions.assertTrue(param instanceof FeatureQuantityParameterEntity);

        repository.delete(feature);

    }

    private FeatureComplexParameterEntity getComplex(AbstractFeatureEntity feature) {
        FeatureComplexParameterEntity parameter = new FeatureComplexParameterEntity();
        parameter.setName("param_complex");
        parameter.setFeature(feature);
        addComplex(parameter);
        return parameter;
    }

    private FeatureComplexParameterEntity addComplex(FeatureComplexParameterEntity parameter) {
        Set<FeatureParameterEntity<?>> children = new LinkedHashSet<>();
        FeatureQuantityParameterEntity child1 = new FeatureQuantityParameterEntity();
        child1.setName("param_complex_quantity");
        child1.setValue(new BigDecimal("1.0"));
        child1.setFeature(parameter.getFeature());
        UnitEntity unit = new UnitEntity();
        unit.setIdentifier("m");
        child1.setUnit(unit);
        children.add(child1);

        FeatureCountParameterEntity child2 = new FeatureCountParameterEntity();
        child2.setName("param_complex_count");
        child2.setValue(2);
        child2.setFeature(parameter.getFeature());
        children.add(child2);
        parameter.setValue(children);
        return parameter;
    }


}
