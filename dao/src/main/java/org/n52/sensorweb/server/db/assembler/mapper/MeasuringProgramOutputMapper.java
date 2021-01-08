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
        super(query, outputMapperFactory);
    }

    @Override
    public MeasuringProgramOutput createCondensed(MeasuringProgramEntity entity, MeasuringProgramOutput output) {
        return condensed(entity, super.createCondensed(entity, output));
    }

    private MeasuringProgramOutput condensed(MeasuringProgramEntity entity, MeasuringProgramOutput output) {
        MeasuringProgramOutput result = super.createCondensed(entity, output);
        IoParameters parameters = query.getParameters();
        result.setValue(MeasuringProgramOutput.ORDER_ID, entity.getIdentifier(), parameters, result::setOrderId);
        result.setValue(MeasuringProgramOutput.MEASURING_PROGRAM_TIME_START,
                createTimeOutput(entity.getMeasuringTimeStart(), parameters), parameters,
                result::setMeasuringProgramTimeStart);
        result.setValue(MeasuringProgramOutput.MEASURING_PROGRAM_TIME_END, getMeasuringtimeEnd(entity, parameters),
                parameters, result::setMeasuringProgramTimeEnd);
        result.setValue(MeasuringProgramOutput.PRODUCER, getCondensedProducer(entity.getProducer(), parameters),
                parameters, result::setProducer);
        result.setValue(MeasuringProgramOutput.OBSERVED_AREA, getObservedArea(entity, query), parameters,
                result::setObservedArea);
        return result;
    }

    @Override
    public MeasuringProgramOutput addExpandedValues(MeasuringProgramEntity entity, MeasuringProgramOutput output) {
        IoParameters parameters = query.getParameters();
        output.setValue(MeasuringProgramOutput.DATASETS, getDatasets(entity, query), parameters, output::setDatasets);
        output.setValue(MeasuringProgramOutput.SAMPLINGS, getSamplings(entity, query), parameters,
                output::setSamplings);
        output.setValue(MeasuringProgramOutput.FEATURES, getFeatures(entity, query), parameters, output::setFeatures);
        output.setValue(MeasuringProgramOutput.PHENOMENA, getPhenomena(entity, query), parameters,
                output::setPhenomena);
        output.setValue(MeasuringProgramOutput.CATEGORIES, getCategories(entity, query), parameters,
                output::setCategories);
        return output;
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

    private List<DatasetOutput<?>> getDatasets(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getDatasets() != null ? measuringProgram.getDatasets().stream()
                .map(d -> getDatasetOutput(d, query)).collect(Collectors.toList()) : new LinkedList<>();
    }

    private List<SamplingOutput> getSamplings(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getSamplings() != null ? measuringProgram.getSamplings().stream()
                .map(s -> getSamplingOutput(s, query)).collect(Collectors.toList()) : new LinkedList<>();
    }

    private List<FeatureOutput> getFeatures(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getDatasets() != null ? measuringProgram.getDatasets().stream()
                .map(d -> getFeatureOutput(d.getFeature(), query)).collect(Collectors.toList()) : new LinkedList<>();
    }

    private List<PhenomenonOutput> getPhenomena(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getDatasets() != null
                ? measuringProgram.getDatasets().stream().map(d -> getPhenomenonOutput(d.getPhenomenon(), query))
                        .collect(Collectors.toList())
                : new LinkedList<>();
    }

    private List<CategoryOutput> getCategories(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getDatasets() != null ? measuringProgram.getDatasets().stream()
                .map(d -> getCategoryOutput(d.getCategory(), query)).collect(Collectors.toList()) : new LinkedList<>();
    }

    @Override
    public MeasuringProgramOutput getParameterOuput() {
        return new MeasuringProgramOutput();
    }
}
