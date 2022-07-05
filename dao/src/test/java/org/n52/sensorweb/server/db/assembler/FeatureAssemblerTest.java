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
package org.n52.sensorweb.server.db.assembler;

import static org.n52.sensorweb.server.test.FeatureBuilder.newFeature;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.sensorweb.server.db.assembler.core.FeatureAssembler;
import org.n52.sensorweb.server.db.repositories.core.FeatureRepository;
import org.n52.sensorweb.server.db.repositories.core.UnitRepository;
import org.n52.sensorweb.server.test.FeatureBuilder;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.Describable;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.FormatEntity;
import org.n52.series.db.beans.UnitEntity;
import org.n52.series.db.beans.i18n.I18nEntity;
import org.n52.series.db.beans.i18n.I18nFeatureEntity;
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
@TestMethodOrder(OrderAnnotation.class)
public class FeatureAssemblerTest extends AbstractAssemblerTest {

    @Autowired
    private FeatureRepository repository;

    @Autowired
    private FeatureAssembler assembler;

    @Test
    @Order(1)
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

        AbstractFeatureEntity<?> inserted = assembler.getOrInsertInstance(entity);
        Assertions.assertNotNull(inserted);
        Assertions.assertTrue(inserted.hasParameters());
        Assertions.assertEquals(1, inserted.getParameters().size());
        ParameterEntity<?> param = inserted.getParameters().iterator().next();
        Assertions.assertNotNull(param);
        Assertions.assertTrue(param instanceof FeatureQuantityParameterEntity);
        Assertions.assertNotNull(((FeatureQuantityParameterEntity) param).getUnit());

        repository.delete(inserted);
        repository.flush();
        Optional<AbstractFeatureEntity> deleted = repository.findById(inserted.getId());
        Assertions.assertNotNull(deleted);
        Assertions.assertFalse(deleted.isPresent());
    }

    @Test
    @Order(2)
    public void insert_feature_with_translation() {
        final FormatEntity formatEntity = new FormatEntity();
        formatEntity.setFormat(OGCConstants.UNKNOWN);
        final FeatureBuilder builder = newFeature("feature_translation");
        final FeatureEntity entity = builder.setFormat(formatEntity).build();

        I18nFeatureEntity i18n = new I18nFeatureEntity();
        i18n.setLocale("en");
        i18n.setEntity(entity);
        i18n.setName("test");
        Set<I18nEntity<? extends Describable>> i18ns = new LinkedHashSet<>();
        i18ns.add(i18n);
        entity.setTranslations(i18ns);

        AbstractFeatureEntity<?> inserted = assembler.getOrInsertInstance(entity);
        Assertions.assertNotNull(inserted);
        Assertions.assertTrue(inserted.hasTranslations());
        Assertions.assertEquals(1, inserted.getTranslations().size());
        I18nEntity<? extends Describable> param = inserted.getTranslations().iterator().next();
        Assertions.assertNotNull(param);
        Assertions.assertTrue(param instanceof I18nFeatureEntity);

        repository.delete(inserted);
        repository.flush();
        Optional<AbstractFeatureEntity> deleted = repository.findById(inserted.getId());
        Assertions.assertNotNull(deleted);
        Assertions.assertFalse(deleted.isPresent());
    }

    @Test
    @Order(3)
    public void insert_feature_with_complex_parameter() {
        final FormatEntity formatEntity = new FormatEntity();
        formatEntity.setFormat(OGCConstants.UNKNOWN);
        final FeatureBuilder builder = newFeature("feature_param_2");
        final FeatureEntity entity = builder.setFormat(formatEntity).build();
        entity.addParameter(getComplex(entity));
        AbstractFeatureEntity<?> inserted = assembler.getOrInsertInstance(entity);
        Assertions.assertNotNull(inserted);
        Assertions.assertNotNull(inserted);
        Assertions.assertTrue(inserted.hasParameters());

        Assertions.assertEquals(1, inserted.getParameters().size());
        ParameterEntity<?> param = inserted.getParameters().iterator().next();
        Assertions.assertNotNull(param);
        Assertions.assertTrue(param instanceof FeatureComplexParameterEntity);
        FeatureComplexParameterEntity complex = (FeatureComplexParameterEntity) param;
        Assertions.assertNotNull(complex.getValue());
        Assertions.assertEquals(2, complex.getValue().size());

        repository.delete(inserted);
        repository.flush();
        Optional<AbstractFeatureEntity> deleted = repository.findById(inserted.getId());
        Assertions.assertNotNull(deleted);
        Assertions.assertFalse(deleted.isPresent());
    }

    @Test
    @Order(4)
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
        AbstractFeatureEntity<?> inserted = assembler.getOrInsertInstance(entity);
        Assertions.assertNotNull(inserted);
        Assertions.assertTrue(inserted.hasParameters());

        Assertions.assertEquals(2, inserted.getParameters().size());
        Iterator<ParameterEntity<?>> iterator = inserted.getParameters().iterator();
        ParameterEntity<?> param = iterator.next();
        Assertions.assertNotNull(param);
        Assertions.assertTrue(param instanceof FeatureComplexParameterEntity);
        FeatureComplexParameterEntity complex = (FeatureComplexParameterEntity) param;
        Assertions.assertNotNull(complex.getValue());
        Assertions.assertEquals(2, complex.getValue().size());

        param = iterator.next();
        Assertions.assertTrue(param instanceof FeatureQuantityParameterEntity);

        repository.delete(inserted);
        repository.flush();
        Optional<AbstractFeatureEntity> deleted = repository.findById(inserted.getId());
        Assertions.assertNotNull(deleted);
        Assertions.assertFalse(deleted.isPresent());
    }
    
    @Test
    @Order(5)
    public void update_feature_with_simple_parameter() {
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

        AbstractFeatureEntity<?> inserted = assembler.getOrInsertInstance(entity);
        Assertions.assertNotNull(inserted);
        Assertions.assertTrue(inserted.hasParameters());
        Assertions.assertEquals(1, inserted.getParameters().size());
        ParameterEntity<?> param = inserted.getParameters().iterator().next();
        Assertions.assertNotNull(param);
        Assertions.assertTrue(param instanceof FeatureQuantityParameterEntity);
        Assertions.assertNotNull(((FeatureQuantityParameterEntity) param).getUnit());
        
        Optional<AbstractFeatureEntity> optional = repository.findById(inserted.getId());
        Assertions.assertNotNull(optional);
        Assertions.assertTrue(optional.isPresent());
        AbstractFeatureEntity foi = optional.get();
        ((FeatureQuantityParameterEntity) foi.getParameters().iterator().next()).setValue(new BigDecimal("1.10"));
        FeatureQuantityParameterEntity parameter2 = new FeatureQuantityParameterEntity();
        parameter2.setName("param_name_2");
        parameter2.setValue(new BigDecimal("2.00"));
        parameter2.setFeature(entity);
        parameter2.setUnit(unit);
        entity.addParameter(parameter2);
        AbstractFeatureEntity updateInstance = assembler.updateInstance(entity);
        Assertions.assertNotNull(updateInstance);
        Assertions.assertTrue(updateInstance.hasParameters());
        Assertions.assertEquals(2, updateInstance.getParameters().size());
        for (ParameterEntity<?> p : updateInstance.getParameters()) {
            Assertions.assertNotNull(p);
            Assertions.assertTrue(p instanceof FeatureQuantityParameterEntity);
            FeatureQuantityParameterEntity fqpe = (FeatureQuantityParameterEntity) p;
            if (fqpe.getName().equals("param_name")) {
                Assertions.assertEquals(new BigDecimal("1.10"), fqpe.getValue());
            } else if (fqpe.getName().equals("param_name_2")) {
                Assertions.assertEquals(new BigDecimal("2.00"), fqpe.getValue());
            }
        }

        repository.delete(updateInstance);
        repository.flush();
        Optional<AbstractFeatureEntity> deleted = repository.findById(inserted.getId());
        Assertions.assertNotNull(deleted);
        Assertions.assertFalse(deleted.isPresent());
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
        child1.setParent(parameter);
        UnitEntity unit = new UnitEntity();
        unit.setIdentifier("m");
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
