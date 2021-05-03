/*
 * Copyright (C) 2015-2021 52°North Initiative for Geospatial Open Source
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
package org.n52.series.db.da.mapper;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.locationtech.jts.geom.Geometry;
import org.n52.io.request.IoParameters;
import org.n52.io.response.CategoryOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.TimeOutput;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.sampling.MeasuringProgramOutput;
import org.n52.io.response.sampling.ProducerOutput;
import org.n52.io.response.sampling.SamplingOutput;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.sampling.MeasuringProgramEntity;
import org.n52.series.db.dao.DbQuery;

public class MeasuringProgramMapper extends AbstractOuputMapper<MeasuringProgramOutput, MeasuringProgramEntity> {

    public MeasuringProgramMapper(MapperFactory mapperFactory, IoParameters params) {
        super(mapperFactory, params);
    }

    @Override
    public MeasuringProgramOutput createCondensed(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return createCondensed(new MeasuringProgramOutput(), measuringProgram, query);
    }

    @Override
    public MeasuringProgramOutput createExpanded(MeasuringProgramEntity measuringProgram, DbQuery query,
            Session session) {
        IoParameters parameters = query.getParameters();
        MeasuringProgramOutput result = createCondensed(measuringProgram, query);
        if (parameters.isSelected(MeasuringProgramOutput.DATASETS)) {
            result.setValue(MeasuringProgramOutput.DATASETS, getDatasets(measuringProgram, query, session), parameters,
                    result::setDatasets);
        }
        if (parameters.isSelected(MeasuringProgramOutput.SAMPLINGS)) {
            result.setValue(MeasuringProgramOutput.SAMPLINGS, getSamplings(measuringProgram, query), parameters,
                    result::setSamplings);
        }
        if (parameters.isSelected(MeasuringProgramOutput.FEATURES)) {
            result.setValue(MeasuringProgramOutput.FEATURES, getFeatures(measuringProgram, query), parameters,
                    result::setFeatures);
        }
        if (parameters.isSelected(MeasuringProgramOutput.PHENOMENA)) {
            result.setValue(MeasuringProgramOutput.PHENOMENA, getPhenomena(measuringProgram, query), parameters,
                    result::setPhenomena);
        }
        if (parameters.isSelected(MeasuringProgramOutput.CATEGORIES)) {
            result.setValue(MeasuringProgramOutput.CATEGORIES, getCategories(measuringProgram, query), parameters,
                    result::setCategories);
        }
        return result;
    }

    @Override
    public void addAll(MeasuringProgramOutput result, MeasuringProgramEntity measuringProgram, DbQuery query,
            IoParameters parameters) {
        super.addAll(result, measuringProgram, query, parameters);
        addOrderId(result, measuringProgram, query, parameters);
        addTimeStart(result, measuringProgram, query, parameters);
        addTimeEnd(result, measuringProgram, query, parameters);
        addProducer(result, measuringProgram, query, parameters);
        addObservedArea(result, measuringProgram, query, parameters);
    }

    @Override
    public void addSelected(MeasuringProgramOutput result, MeasuringProgramEntity entity, DbQuery query,
            IoParameters parameters) {
        super.addSelected(result, entity, query, parameters);
        for (String selected : parameters.getSelectOriginal()) {
            switch (selected) {
                case MeasuringProgramOutput.ORDER_ID:
                    addOrderId(result, entity, query, parameters);
                    break;
                case MeasuringProgramOutput.MEASURING_PROGRAM_TIME_START:
                    addTimeStart(result, entity, query, parameters);
                    break;
                case MeasuringProgramOutput.MEASURING_PROGRAM_TIME_END:
                    addTimeEnd(result, entity, query, parameters);
                    break;
                case MeasuringProgramOutput.PRODUCER:
                    addProducer(result, entity, query, parameters);
                    break;
                case MeasuringProgramOutput.OBSERVED_AREA:
                    addObservedArea(result, entity, query, parameters);
                    break;
                default:
                    break;
            }
        }
    }

    private void addOrderId(MeasuringProgramOutput result, MeasuringProgramEntity measuringProgram, DbQuery query,
            IoParameters parameters) {
        result.setValue(MeasuringProgramOutput.ORDER_ID, measuringProgram.getIdentifier(), parameters,
                result::setOrderId);
    }

    private void addTimeStart(MeasuringProgramOutput result, MeasuringProgramEntity measuringProgram, DbQuery query,
            IoParameters parameters) {
        result.setValue(MeasuringProgramOutput.MEASURING_PROGRAM_TIME_START,
                createTimeOutput(measuringProgram.getMeasuringTimeStart(), parameters), parameters,
                result::setMeasuringProgramTimeStart);
    }

    private void addTimeEnd(MeasuringProgramOutput result, MeasuringProgramEntity measuringProgram, DbQuery query,
            IoParameters parameters) {
        result.setValue(MeasuringProgramOutput.MEASURING_PROGRAM_TIME_END,
                getMeasuringtimeEnd(measuringProgram, parameters), parameters, result::setMeasuringProgramTimeEnd);
    }

    private void addProducer(MeasuringProgramOutput result, MeasuringProgramEntity measuringProgram, DbQuery query,
            IoParameters parameters) {
        result.setValue(MeasuringProgramOutput.PRODUCER,
                getCondensedProducer(measuringProgram.getProducer(), parameters), parameters, result::setProducer);
    }

    private void addObservedArea(MeasuringProgramOutput result, MeasuringProgramEntity measuringProgram, DbQuery query,
            IoParameters parameters) {
        result.setValue(MeasuringProgramOutput.OBSERVED_AREA, getObservedArea(measuringProgram, query), parameters,
                result::setObservedArea);
    }

    private TimeOutput getMeasuringtimeEnd(MeasuringProgramEntity measuringProgram, IoParameters parameters) {
        if (measuringProgram.isSetMeasuringTimeEnd()) {
            return createTimeOutput(measuringProgram.getMeasuringTimeStart(), parameters);
        }
        return null;
    }

    private ProducerOutput getCondensedProducer(String producer, IoParameters parameters) {
        if (producer != null) {
            ProducerOutput result = new ProducerOutput();
            result.setValue(ProducerOutput.LABEL, producer, parameters, result::setLabel);
            return result;
        }
        return null;
    }

    private Geometry getObservedArea(MeasuringProgramEntity measuringProgram, DbQuery query) {
        Geometry observedArea = null;
        if (measuringProgram.hasDatasets()) {
            for (DatasetEntity dataset : measuringProgram.getDatasets()) {
                if (dataset.isSetFeature() && dataset.getFeature().isSetGeometry()) {
                    Geometry featureGeometry = getGeometry(dataset.getFeature().getGeometryEntity(), query);
                    if (observedArea == null) {
                        observedArea = featureGeometry;
                    } else {
                        observedArea.getEnvelopeInternal().expandToInclude(featureGeometry.getEnvelopeInternal());
                    }
                }
            }
        }
        return observedArea;
    }

    private List<DatasetOutput<?>> getDatasets(MeasuringProgramEntity measuringProgram, DbQuery query,
            Session session) {
        return measuringProgram.getDatasets() != null
                ? measuringProgram.getDatasets().stream()
                        .map(d -> createCondensedMinimal(new DatasetOutput<>(), d, query)).collect(Collectors.toList())
                : new LinkedList<>();
    }

    private List<SamplingOutput> getSamplings(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getSamplings() != null
                ? measuringProgram.getSamplings().stream()
                        .map(s -> createCondensedMinimal(new SamplingOutput(), s, query)).collect(Collectors.toList())
                : new LinkedList<>();
    }

    private List<FeatureOutput> getFeatures(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getDatasets() != null ? measuringProgram.getDatasets().stream()
                .map(d -> getCondensedFeature(d.getFeature(), query)).collect(Collectors.toList())
                : new LinkedList<>();
    }

    private List<PhenomenonOutput> getPhenomena(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getDatasets() != null
                ? measuringProgram.getDatasets().stream().map(d -> getCondensedPhenomenon(d.getPhenomenon(), query))
                        .collect(Collectors.toList())
                : new LinkedList<>();
    }

    private List<CategoryOutput> getCategories(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getDatasets() != null
                ? measuringProgram.getDatasets().stream().map(d -> getCondensedCategory(d.getCategory(), query))
                        .collect(Collectors.toList())
                : new LinkedList<>();
    }
}
