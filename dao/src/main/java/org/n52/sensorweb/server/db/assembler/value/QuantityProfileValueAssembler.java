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
package org.n52.sensorweb.server.db.assembler.value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.profile.ProfileDataItem;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.sensorweb.server.db.ValueAssemblerComponent;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.repositories.core.DataRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.QuantityDataEntity;

@ValueAssemblerComponent(value = "quantity-profile", datasetEntityType = DatasetEntity.class)
public class QuantityProfileValueAssembler extends ProfileValueAssembler<BigDecimal, BigDecimal> {

    public QuantityProfileValueAssembler(DataRepository<ProfileDataEntity> profileDataRepository,
            DatasetRepository datasetRepository) {
        super(profileDataRepository, datasetRepository);
    }

    @Override
    public ProfileValue<BigDecimal> assembleDataValue(ProfileDataEntity observation, DatasetEntity dataset,
            DbQuery query) {
        ProfileValue<BigDecimal> profile = prepareValue(new ProfileValue<>(), observation, dataset, query);
        return assembleDataValue(observation, dataset, query, profile);
    }

    private ProfileValue<BigDecimal> assembleDataValue(ProfileDataEntity observation, DatasetEntity dataset,
            DbQuery query, ProfileValue<BigDecimal> profile) {
        profile.setValue(getDataValue(observation, dataset, profile, query));
        return profile;
    }

    @Override
    public ProfileValue<BigDecimal> getFirstValue(DatasetEntity dataset, DbQuery query) {
        if (hasConnector(dataset)) {
            DataEntity<?> data = getConnector(dataset).getFirstObservation(dataset).orElse(null);
            return assembleDataValue((ProfileDataEntity) data, dataset, query);
        }
        return super.getFirstValue(dataset, query);
    }

    @Override
    public ProfileValue<BigDecimal> getLastValue(DatasetEntity dataset, DbQuery query) {
        if (hasConnector(dataset)) {
            DataEntity<?> data = getConnector(dataset).getLastObservation(dataset).orElse(null);
            return assembleDataValue((ProfileDataEntity) data, dataset, query);
        }
        return super.getLastValue(dataset, query);
    }

    @Override
    protected Data<ProfileValue<BigDecimal>> assembleDataValues(DatasetEntity dataset, DbQuery query) {
        if (hasConnector(dataset)) {
            Data<ProfileValue<BigDecimal>> result = new Data<>();
            getConnector(dataset).getObservations(dataset, query).stream()
                    .map(entry -> assembleDataValue((ProfileDataEntity) entry, dataset, query))
                    .forEach(entry -> result.addNewValue(entry));
            return result;
        }
        return super.assembleDataValues(dataset, query);
    }

    private List<ProfileDataItem<BigDecimal>> getDataValue(ProfileDataEntity observation, DatasetEntity dataset,
            ProfileValue<BigDecimal> profile, DbQuery query) {
        List<ProfileDataItem<BigDecimal>> dataItems = new ArrayList<>();
        for (DataEntity<?> dataEntity : observation.getValue()) {
            QuantityDataEntity quantityEntity = (QuantityDataEntity) dataEntity;
            QuantityValue valueItem = prepareValue(new QuantityValue(), dataEntity, dataset, query);

            // XXX still needed?
            addParameters(quantityEntity, valueItem, query);

            if (observation.hasVerticalFrom() || observation.hasVerticalTo()) {
                dataItems.add(assembleDataItem(quantityEntity, profile, observation, query));
            } else {
                dataItems.add(assembleDataItem(quantityEntity, profile, valueItem.getParameters(), dataset, query));
            }
        }
        return dataItems;
    }

}
