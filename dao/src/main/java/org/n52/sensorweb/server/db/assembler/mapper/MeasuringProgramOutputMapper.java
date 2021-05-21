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
package org.n52.sensorweb.server.db.assembler.mapper;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
import org.n52.sensorweb.server.db.TimeOutputCreator;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.sampling.MeasuringProgramEntity;

public class MeasuringProgramOutputMapper
        extends ParameterOutputSearchResultMapper<MeasuringProgramEntity, MeasuringProgramOutput>
        implements TimeOutputCreator {

    public MeasuringProgramOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory) {
        this(query, outputMapperFactory, false);
    }

    public MeasuringProgramOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory, boolean subMapper) {
        super(query, outputMapperFactory, subMapper);
    }

    @Override
    public MeasuringProgramOutput addExpandedValues(MeasuringProgramEntity entity, MeasuringProgramOutput output) {
        IoParameters parameters = getDbQuery().getParameters();
        if (parameters.isSelected(MeasuringProgramOutput.DATASETS)) {
            output.setValue(MeasuringProgramOutput.DATASETS, getDatasets(entity), parameters,
                    output::setDatasets);
        }
        if (parameters.isSelected(MeasuringProgramOutput.SAMPLINGS)) {
            output.setValue(MeasuringProgramOutput.SAMPLINGS, getSamplings(entity), parameters,
                    output::setSamplings);
        }
        if (parameters.isSelected(MeasuringProgramOutput.FEATURES)) {
            output.setValue(MeasuringProgramOutput.FEATURES, getFeatures(entity), parameters,
                    output::setFeatures);
        }
        if (parameters.isSelected(MeasuringProgramOutput.PHENOMENA)) {
            output.setValue(MeasuringProgramOutput.PHENOMENA, getPhenomena(entity), parameters,
                    output::setPhenomena);
        }
        if (parameters.isSelected(MeasuringProgramOutput.CATEGORIES)) {
            output.setValue(MeasuringProgramOutput.CATEGORIES, getCategories(entity), parameters,
                    output::setCategories);
        }
        return output;
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
                    Geometry featureGeometry = createGeometry(dataset.getFeature());
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

    private List<DatasetOutput<?>> getDatasets(MeasuringProgramEntity measuringProgram) {
        return measuringProgram.getDatasets() != null ? measuringProgram.getDatasets().stream()
                .map(d -> getDatasetOutput(d, getDbQuery())).collect(Collectors.toList()) : new LinkedList<>();
    }

    private List<SamplingOutput> getSamplings(MeasuringProgramEntity measuringProgram) {
        return measuringProgram.getSamplings() != null ? measuringProgram.getSamplings().stream()
                .map(s -> getSamplingOutput(s, getDbQuery())).collect(Collectors.toList()) : new LinkedList<>();
    }

    private List<FeatureOutput> getFeatures(MeasuringProgramEntity measuringProgram) {
        return measuringProgram.getDatasets() != null ? measuringProgram.getDatasets().stream()
                .map(d -> getFeatureOutput(d.getFeature())).collect(Collectors.toList()) : new LinkedList<>();
    }

    private List<PhenomenonOutput> getPhenomena(MeasuringProgramEntity measuringProgram) {
        return measuringProgram.getDatasets() != null
                ? measuringProgram.getDatasets().stream().map(d -> getPhenomenonOutput(d.getPhenomenon()))
                        .collect(Collectors.toList())
                : new LinkedList<>();
    }

    private List<CategoryOutput> getCategories(MeasuringProgramEntity measuringProgram) {
        return measuringProgram.getDatasets() != null ? measuringProgram.getDatasets().stream()
                .map(d -> getCategoryOutput(d.getCategory())).collect(Collectors.toList()) : new LinkedList<>();
    }

    @Override
    public MeasuringProgramOutput getParameterOuput() {
        return new MeasuringProgramOutput();
    }
}
