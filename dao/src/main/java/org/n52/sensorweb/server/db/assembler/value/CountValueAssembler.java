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
package org.n52.sensorweb.server.db.assembler.value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetMetadata;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.count.CountValue;
import org.n52.sensorweb.server.db.ValueAssemblerComponent;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.repositories.core.DataRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.CountDataEntity;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProcedureEntity;

@ValueAssemblerComponent(value = "count", datasetEntityType = DatasetEntity.class)
public class CountValueAssembler extends AbstractNumericalValueAssembler<CountDataEntity, CountValue, Integer> {

    public CountValueAssembler(DataRepository<CountDataEntity> dataRepository, DatasetRepository datasetRepository) {
        super(dataRepository, datasetRepository);
    }

    @Override
    public CountValue assembleDataValue(CountDataEntity data, DatasetEntity dataset, DbQuery query) {
        final CountValue assembledValue = prepareValue(new CountValue(), data, dataset, query);
        assembledValue.setValue(getDataValue(data, dataset));
        return assembledValue;
    }

    @Override
    public CountValue getFirstValue(DatasetEntity dataset, DbQuery query) {
        if (hasConnector(dataset)) {
            DataEntity<?> data = getConnector(dataset).getFirstObservation(dataset).orElse(null);
            return assembleDataValue((CountDataEntity) data, dataset, query);
        }
        return super.getFirstValue(dataset, query);
    }

    @Override
    public CountValue getLastValue(DatasetEntity dataset, DbQuery query) {
        if (hasConnector(dataset)) {
            DataEntity<?> data = getConnector(dataset).getLastObservation(dataset).orElse(null);
            return assembleDataValue((CountDataEntity) data, dataset, query);
        }
        return super.getLastValue(dataset, query);
    }

    @Override
    protected Data<CountValue> assembleDataValues(DatasetEntity dataset, DbQuery query) {
        if (hasConnector(dataset)) {
            Data<CountValue> result = new Data<>();
            getConnector(dataset).getObservations(dataset, query).stream()
                    .map(entry -> assembleDataValue((CountDataEntity) entry, dataset, query))
                    .forEach(entry -> result.addNewValue(entry));
            return result;
        }
        return super.assembleDataValues(dataset, query);
    }

    private Integer getDataValue(CountDataEntity observation, DatasetEntity dataset) {
        return !isNoDataValue(observation, dataset) && observation.getValue() != null ? observation.getValue() : null;
    }

    @Override
    protected Data<CountValue> assembleExpandedDataValues(DatasetEntity dataset, DbQuery query) {
        Data<CountValue> result = assembleDataValues(dataset, query);
        if (!result.hasMetadata()) {
            result.setMetadata(new DatasetMetadata<>());
        }
        DatasetMetadata<CountValue> metadata = result.getMetadata();

        CountDataEntity previousValue = getClosestValueAfterEnd(dataset, query);
        CountDataEntity nextValue = getClosestValueAfterEnd(dataset, query);
        if (previousValue != null) {
            metadata.setValueBeforeTimespan(assembleDataValue(previousValue, dataset, query));
        }
        if (nextValue != null) {
            metadata.setValueAfterTimespan(assembleDataValue(nextValue, dataset, query));
        }

        List<DatasetEntity> referenceValues = dataset.getReferenceValues();
        if ((referenceValues != null) && !referenceValues.isEmpty()) {
            metadata.setReferenceValues(assembleReferenceSeries(referenceValues, query));
        }
        return result;
    }

    private Map<String, Data<CountValue>> assembleReferenceSeries(List<DatasetEntity> referenceValues, DbQuery query) {
        Map<String, Data<CountValue>> referenceSeries = new HashMap<>();
        for (DatasetEntity referenceSeriesEntity : referenceValues) {
            if (referenceSeriesEntity.isPublished()) {
                Data<CountValue> referenceSeriesData = assembleDataValues(referenceSeriesEntity, query);
                if (haveToExpandReferenceData(referenceSeriesData)) {
                    referenceSeriesData = expandReferenceDataIfNecessary(referenceSeriesEntity, query);
                }
                referenceSeries.put(createReferenceDatasetId(query, referenceSeriesEntity), referenceSeriesData);
            }
        }
        return referenceSeries;
    }

    private boolean haveToExpandReferenceData(Data<CountValue> referenceSeriesData) {
        Set<CountValue> values = referenceSeriesData.getValues();
        return values.size() <= 1;
    }

    private Data<CountValue> expandReferenceDataIfNecessary(DatasetEntity dataset, DbQuery query) {
        Data<CountValue> result = new Data<>();
        List<CountDataEntity> observations = findAll(dataset, query).collect(Collectors.toList());
        if (!hasValidEntriesWithinRequestedTimespan(observations)) {
            CountValue lastValue = getLastValue(dataset, query);
            result.addValues(expandToInterval(lastValue.getValue(), dataset, query));
        }

        if (hasSingleValidReferenceValue(observations)) {
            CountDataEntity entity = observations.get(0);
            result.addValues(expandToInterval(entity.getValue(), dataset, query));
        }
        return result;
    }

    private CountValue[] expandToInterval(Integer value, DatasetEntity series, DbQuery query) {
        CountDataEntity referenceStart = new CountDataEntity();
        CountDataEntity referenceEnd = new CountDataEntity();
        referenceStart.setSamplingTimeEnd(query.getTimespan().getStart().toDate());
        referenceEnd.setSamplingTimeEnd(query.getTimespan().getEnd().toDate());
        referenceStart.setValue(value);
        referenceEnd.setValue(value);
        return new CountValue[] { assembleDataValue(referenceStart, series, query),
                assembleDataValue(referenceEnd, series, query), };
    }

    @Override
    public List<ReferenceValueOutput<CountValue>> getReferenceValues(DatasetEntity datasetEntity, DbQuery query) {
        List<DatasetEntity> referenceValues = datasetEntity.getReferenceValues();
        List<ReferenceValueOutput<CountValue>> outputs = new ArrayList<>();
        for (DatasetEntity referenceSeriesEntity : referenceValues) {
            ReferenceValueOutput<CountValue> refenceValueOutput = new ReferenceValueOutput<>();
            ProcedureEntity procedure = referenceSeriesEntity.getProcedure();
            refenceValueOutput.setLabel(procedure.getNameI18n(query.getLocale()));
            refenceValueOutput.setReferenceValueId(createReferenceDatasetId(query, referenceSeriesEntity));

            CountDataEntity lastValue = (CountDataEntity) referenceSeriesEntity.getLastObservation();
            refenceValueOutput.setLastValue(assembleDataValue(lastValue, referenceSeriesEntity, query));
            outputs.add(refenceValueOutput);
        }
        return outputs;
    }

    private String createReferenceDatasetId(DbQuery query, DatasetEntity referenceSeriesEntity) {
        DatasetOutput<?> dataset = new DatasetOutput<>();
        Long id = referenceSeriesEntity.getId();
        dataset.setId(id.toString());

        String referenceDatasetId = dataset.getId();
        return referenceDatasetId.toString();
    }

}
