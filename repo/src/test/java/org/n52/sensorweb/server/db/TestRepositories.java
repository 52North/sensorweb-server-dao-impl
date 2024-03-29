/*
 * Copyright (C) 2015-2023 52°North Spatial Information Research GmbH
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
package org.n52.sensorweb.server.db;

import static org.n52.sensorweb.server.test.DatasetEntityBuilder.newDataset;
import static org.n52.sensorweb.server.test.FeatureBuilder.newFeature;
import static org.n52.sensorweb.server.test.FormatBuilder.newFormat;
import static org.n52.sensorweb.server.test.OfferingBuilder.newOffering;
import static org.n52.sensorweb.server.test.PhenomenonBuilder.newPhenomenon;
import static org.n52.sensorweb.server.test.ProcedureBuilder.newProcedure;

import org.n52.sensorweb.server.db.repositories.core.CategoryRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.sensorweb.server.db.repositories.core.FeatureRepository;
import org.n52.sensorweb.server.db.repositories.core.FormatRepository;
import org.n52.sensorweb.server.db.repositories.core.OfferingRepository;
import org.n52.sensorweb.server.db.repositories.core.PhenomenonRepository;
import org.n52.sensorweb.server.db.repositories.core.ProcedureRepository;
import org.n52.sensorweb.server.test.CategoryBuilder;
import org.n52.sensorweb.server.test.FeatureBuilder;
import org.n52.sensorweb.server.test.ProcedureBuilder;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.FormatEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestRepositories {

    @Autowired
    private PhenomenonRepository phenomenonRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private OfferingRepository offeringRepository;

    @Autowired
    private ProcedureRepository procedureRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    public <T extends DatasetEntity> T persistSimpleDataset(final String phenomenonIdentifier,
                                  final String offeringIdentifier,
                                  final String procedureIdentifier,
                                  final String procedureFormat,
                                  final T emptyDatasetEntity) {
        final T dataset = buildNewDataset(procedureFormat,
                                    procedureIdentifier,
                                    phenomenonIdentifier,
                                    offeringIdentifier,
                                    emptyDatasetEntity);
        return save(dataset);
    }

    public <T extends DatasetEntity> T persistSimpleDataset(final String phenomenonIdentifier,
                                  final String offeringIdentifier,
                                  final String procedureIdentifier,
                                  final String procedureFormat,
                                  final String featureIdentifier,
                                  final String featureFormat,
                                  final T emptyDatasetEntity) {
        final T dataset = buildNewDataset(procedureFormat,
                                    procedureIdentifier,
                                    phenomenonIdentifier,
                                    offeringIdentifier,
                                    emptyDatasetEntity);
        dataset.setFeature(upsertSimpleFeature(featureIdentifier, featureFormat));
        return save(dataset);
    }

    private <T extends DatasetEntity> T buildNewDataset(final String procedureFormat,
                              final String procedureIdentifier,
                              final String phenomenonIdentifier,
                              final String offeringIdentifier,
                              final T emptyDatasetEntity) {
        return newDataset().setOffering(upsertSimpleOffering(offeringIdentifier))
                           .setPhenomemon(upsertSimplePhenomenon(phenomenonIdentifier))
                           .setProcedure(upsertSimpleProcedure(procedureIdentifier, procedureFormat))
                           .setCategory(upsertSimpleCategory(phenomenonIdentifier))
                           .build(emptyDatasetEntity);
    }

    public ProcedureEntity upsertSimpleProcedure(final String procedureIdentifier, final String format) {
        return procedureRepository.findByIdentifier(procedureIdentifier)
                                  .orElseGet(() -> persistSimpleProcedure(procedureIdentifier, format));
    }

    public ProcedureEntity persistSimpleProcedure(final String procedureIdentifier, final String format) {
        final FormatEntity formatEntity = upsertFormat(format);
        final ProcedureBuilder builder = newProcedure(procedureIdentifier);
        final ProcedureEntity entity = builder.setFormat(formatEntity)
                                        .build();
        return save(entity);
    }

    public PhenomenonEntity upsertSimplePhenomenon(final String phenomenonIdentifier) {
        return phenomenonRepository.findByIdentifier(phenomenonIdentifier)
                                   .orElseGet(() -> persistSimplePhenomenon(phenomenonIdentifier));
    }

    public PhenomenonEntity persistSimplePhenomenon(final String phenomenonIdentifier) {
        return save(newPhenomenon(phenomenonIdentifier).build());
    }

    public CategoryEntity upsertSimpleCategory(final String categoryIdentifier) {
        return categoryRepository.findByIdentifier(categoryIdentifier)
                                   .orElseGet(() -> persistSimpleCategory(categoryIdentifier));
    }

    public CategoryEntity persistSimpleCategory(final String categoryIdentifier) {
        return save(CategoryBuilder.newCategory(categoryIdentifier).build());
    }

    public AbstractFeatureEntity upsertSimpleFeature(final String featureIdentifier, final String format) {
        return featureRepository.findByIdentifier(featureIdentifier)
                                .orElseGet(() -> persistSimpleFeature(featureIdentifier, format));
    }

    public FeatureEntity persistSimpleFeature(final String featureIdentifier, final String format) {
        final FormatEntity formatEntity = upsertFormat(format);
        final FeatureBuilder builder = newFeature(featureIdentifier);
        final FeatureEntity entity = builder.setFormat(formatEntity)
                                      .build();
        return save(entity);
    }

    public FormatEntity upsertFormat(final String format) {
        return formatRepository.existsByFormat(format)
            ? formatRepository.findByFormat(format).get()
            : save(newFormat(format).build());
    }

    public OfferingEntity upsertSimpleOffering(final String offeringIdentifier) {
        return offeringRepository.findByIdentifier(offeringIdentifier)
                                 .orElseGet(() -> persistSimpleOffering(offeringIdentifier));
    }

    public OfferingEntity persistSimpleOffering(final String offeringIdentifier) {
        return save(newOffering(offeringIdentifier).build());
    }

    public PhenomenonEntity save(final PhenomenonEntity entity) {
        return phenomenonRepository.save(entity);
    }

    public FeatureEntity save(final FeatureEntity entity) {
        return featureRepository.save(entity);
    }

    public OfferingEntity save(final OfferingEntity entity) {
        return offeringRepository.save(entity);
    }

    public ProcedureEntity save(final ProcedureEntity entity) {
        return procedureRepository.saveAndFlush(entity);
    }

    public CategoryEntity save(final CategoryEntity entity) {
        return categoryRepository.save(entity);
    }

    public FormatEntity save(final FormatEntity entity) {
        return formatRepository.save(entity);
    }

    public <T extends DatasetEntity> T save(final T entity) {
        return datasetRepository.save(entity);
    }

}
