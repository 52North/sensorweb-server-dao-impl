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
package org.n52.sensorweb.server.db.assembler.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.n52.io.request.Parameters;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetMetadata;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.janmayen.i18n.LocaleHelper;
import org.n52.sensorweb.server.db.ValueAssemblerComponent;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.repositories.core.DataRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.QuantityDataEntity;

@ValueAssemblerComponent(value = "quantity", datasetEntityType = DatasetEntity.class)
public class QuantityValueAssembler
        extends AbstractNumericalValueAssembler<QuantityDataEntity, QuantityValue, BigDecimal> {

    public QuantityValueAssembler(DataRepository<QuantityDataEntity> dataRepository,
            DatasetRepository datasetRepository) {
        super(dataRepository, datasetRepository);
    }

    @Override
    public QuantityValue assembleDataValue(QuantityDataEntity data, DatasetEntity dataset, DbQuery query) {
        final QuantityValue assembledValue = prepareValue(new QuantityValue(), data, dataset, query);
        assembledValue.setValue(getDataValue(data, dataset));
        return assembledValue;
    }

    @Override
    public QuantityValue getFirstValue(DatasetEntity dataset, DbQuery query) {
        if (hasConnector(dataset)) {
            DataEntity<?> data = getConnector(dataset).getFirstObservation(dataset)
                    .orElse(null);
            return assembleDataValue(unproxy(data), dataset, query);
        } else if (dataset.getFirstQuantityValue() != null) {
            QuantityValue value = new QuantityValue();
            value.setValue(format(dataset.getFirstQuantityValue(), dataset));
            value.setTimestamp(createTimeOutput(dataset.getFirstValueAt(), null, query.getParameters()));
            Locale locale = LocaleHelper.decode(query.getLocale());
            NumberFormat formatter = NumberFormat.getInstance(locale);
            value.setValueFormatter(formatter::format);
            return value;
        }
        return super.getFirstValue(dataset, query);
    }

    @Override
    public QuantityValue getLastValue(DatasetEntity dataset, DbQuery query) {
        if (hasConnector(dataset)) {
            DataEntity<?> data = getConnector(dataset).getLastObservation(dataset)
                    .orElse(null);
            return assembleDataValue(unproxy(data), dataset, query);
        } else if (dataset.getLastQuantityValue() != null) {
            QuantityValue value = new QuantityValue();
            value.setValue(format(dataset.getLastQuantityValue(), dataset));
            value.setTimestamp(createTimeOutput(dataset.getLastValueAt(), null, query.getParameters()));
            Locale locale = LocaleHelper.decode(query.getLocale());
            NumberFormat formatter = NumberFormat.getInstance(locale);
            value.setValueFormatter(formatter::format);
            return value;
        }
        return super.getLastValue(dataset, query);
    }

    @Override
    protected Data<QuantityValue> assembleDataValues(DatasetEntity dataset, DbQuery query) {
        if (hasConnector(dataset)) {
            Data<QuantityValue> result = new Data<>();
            getConnector(dataset).getObservations(dataset, query)
                    .stream()
                    .map(entry -> assembleDataValue(unproxy(entry), dataset, query))
                    .forEach(entry -> result.addNewValue(entry));
            return result;
        }
        return super.assembleDataValues(dataset, query);
    }

    private BigDecimal getDataValue(QuantityDataEntity observation, DatasetEntity dataset) {
        return !isNoDataValue(observation, dataset) ? format(observation, dataset) : null;
    }

    private BigDecimal format(QuantityDataEntity data, DatasetEntity dataset) {
        if (data.getValue() == null) {
            return data.getValue();
        }
        final Integer scale = dataset.getNumberOfDecimals();
        return scale != null && scale.intValue() >= 0 ? data.getValue()
                .setScale(scale, RoundingMode.HALF_UP) : data.getValue();
    }

    @Override
    protected Data<QuantityValue> assembleExpandedDataValues(DatasetEntity dataset, DbQuery query) {
        Data<QuantityValue> result = assembleDataValues(dataset, query);
        if (!result.hasMetadata()) {
            result.setMetadata(new DatasetMetadata<>());
        }
        DatasetMetadata<QuantityValue> metadata = result.getMetadata();

        DataEntity previousValue = getClosestValueBeforeStart(dataset, query);
        DataEntity nextValue = getClosestValueAfterEnd(dataset, query);
        if (previousValue != null) {
            metadata.setValueBeforeTimespan(assembleDataValue(unproxy(previousValue), dataset, query));
        }
        if (nextValue != null) {
            metadata.setValueAfterTimespan(assembleDataValue(unproxy(nextValue), dataset, query));
        }

        List<DatasetEntity> referenceValues = dataset.getReferenceValues();
        if (referenceValues != null && !referenceValues.isEmpty()) {
            metadata.setReferenceValues(assembleReferenceSeries(referenceValues, query));
        }
        return result;
    }

    private Map<String, Data<QuantityValue>> assembleReferenceSeries(List<DatasetEntity> referenceValues,
            DbQuery query) {
        Map<String, Data<QuantityValue>> referenceSeries = new HashMap<>();
        for (DatasetEntity referenceSeriesEntity : referenceValues) {
            if (referenceSeriesEntity.isPublished()) {
                DbQuery refQuery = query.replaceWith(Parameters.DATASETS, Long.toString(referenceSeriesEntity.getId()));
                Data<QuantityValue> referenceSeriesData = assembleDataValues(referenceSeriesEntity, refQuery);
                referenceSeries.put(createReferenceDatasetId(refQuery, referenceSeriesEntity), referenceSeriesData);
            }
        }
        return referenceSeries;
    }

    @Override
    public List<ReferenceValueOutput<QuantityValue>> getReferenceValues(DatasetEntity datasetEntity, DbQuery query) {
        List<DatasetEntity> referenceValues = datasetEntity.getReferenceValues();
        List<ReferenceValueOutput<QuantityValue>> outputs = new ArrayList<>();
        for (DatasetEntity referenceSeriesEntity : referenceValues) {
            ReferenceValueOutput<QuantityValue> refenceValueOutput = new ReferenceValueOutput<>();
            ProcedureEntity procedure = referenceSeriesEntity.getProcedure();
            refenceValueOutput.setLabel(procedure.getNameI18n(query.getLocale()));
            refenceValueOutput.setReferenceValueId(createReferenceDatasetId(query, referenceSeriesEntity));
            QuantityDataEntity lastValue = unproxy(referenceSeriesEntity.getLastObservation());
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
