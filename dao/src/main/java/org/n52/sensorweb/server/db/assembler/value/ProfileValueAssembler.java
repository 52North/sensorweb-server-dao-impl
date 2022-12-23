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
package org.n52.sensorweb.server.db.assembler.value;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.profile.ProfileDataItem;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DataQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.DataRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.StreamUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public abstract class ProfileValueAssembler<V, T>
        extends AbstractValueAssembler<ProfileDataEntity, ProfileValue<V>, Set<DataEntity<?>>> {

    private final DataRepository<ProfileDataEntity> profileDataRepository;

    @SuppressFBWarnings({ "EI_EXPOSE_REP2" })
    public ProfileValueAssembler(DataRepository<ProfileDataEntity> profileDataRepository,
            DatasetRepository datasetRepository) {
        super(profileDataRepository, datasetRepository);
        this.profileDataRepository = profileDataRepository;
    }

    @Override
    protected Data<ProfileValue<V>> assembleDataValues(DatasetEntity dataset, DbQuery query) {
        query.setComplexParent(true);
        return super.assembleDataValues(dataset, query);
    }

    @Override
    public abstract ProfileValue<V> assembleDataValue(ProfileDataEntity observation, DatasetEntity dataset,
            DbQuery query);

    protected <E extends DataEntity<V>> ProfileDataItem<V> assembleDataItem(E dataEntity, ProfileValue<T> profile,
            ProfileDataEntity observation, DbQuery query) {
        ProfileDataItem<V> dataItem = createProfileDataItem();
        dataItem.setValue(dataEntity.getValue());

        // IoParameters parameters = query.getParameters();
        BigDecimal verticalTo = format(dataEntity.getVerticalTo(), dataEntity.getDataset());
        BigDecimal verticalFrom = format(dataEntity.getVerticalFrom(), dataEntity.getDataset());

        dataItem.setVertical(verticalTo);
        if (dataEntity.hasVerticalInterval()) {
            dataItem.setVerticalFrom(verticalFrom);
        }
        return dataItem;
    }

    protected <E extends DataEntity<T>> ProfileDataItem<T> assembleDataItem(E dataEntity, ProfileValue<T> profile,
            Set<Map<String, Object>> parameters, DatasetEntity dataset, DbQuery query) {
        ProfileDataItem<T> dataItem = new ProfileDataItem<>();
        dataItem.setValue(dataEntity.getValue());
        return dataItem;
    }

    @Override
    protected Stream<ProfileDataEntity> findAll(DatasetEntity dataset, DbQuery query) {
        DataQuerySpecifications dataFilterSpec = DataQuerySpecifications.<ProfileDataEntity>of(query);
        Specification<ProfileDataEntity> predicate = dataFilterSpec.matchFiltersParentsIsNull();
        Iterable<ProfileDataEntity> entities = profileDataRepository.findAll(predicate);
        return StreamUtils.createStreamFromIterator(entities.iterator());
    }

    public ProfileDataItem<V> createProfileDataItem() {
        return new ProfileDataItem<>();
    }
}
