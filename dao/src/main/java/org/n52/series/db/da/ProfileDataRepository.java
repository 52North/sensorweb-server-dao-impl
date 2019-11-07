/*
 * Copyright (C) 2015-2019 52°North Initiative for Geospatial Open Source
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
package org.n52.series.db.da;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxy;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.profile.ProfileDataItem;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.io.response.dataset.profile.VerticalExtentOutput;
import org.n52.io.response.dataset.profile.VerticalExtentValueOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.VerticalMetadataEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public abstract class ProfileDataRepository<P extends DatasetEntity, V, T>
        extends
        AbstractDataRepository<P, ProfileDataEntity, ProfileValue<V>, Set<DataEntity< ? >>> {

    private static final String PARAMETER_NAME = "name";

    @Override
    protected ProfileDataEntity unproxy(DataEntity<?> dataEntity, Session session) {
        if (dataEntity instanceof HibernateProxy
                && ((HibernateProxy) dataEntity).getHibernateLazyInitializer().getSession() == null) {
            return unproxy(session.load(dataEntity.getClass(), dataEntity.getId()), session);
        }
        return (ProfileDataEntity) Hibernate.unproxy(dataEntity);
    }

    @Override
    protected ProfileValue<V> createEmptyValue() {
        return new ProfileValue<>();
    }

    protected boolean isVertical(Map<String, Object> parameterObject, String verticalName) {
        if (parameterObject.containsKey(PARAMETER_NAME)) {
            String value = (String) parameterObject.get(PARAMETER_NAME);
            return value.equalsIgnoreCase(verticalName);
        }
        return false;
    }

    @Override
    protected Data<ProfileValue<V>> assembleData(Long dataset, DbQuery query, Session session)
            throws DataAccessException {
        query.setComplexParent(true);
        Data<ProfileValue<V>> result = new Data<>();
        DataDao<ProfileDataEntity> dao = createDataDao(session);
        List<ProfileDataEntity> observations = dao.getAllInstancesFor(dataset, query);
        for (ProfileDataEntity observation : observations) {
            if (observation != null) {
                result.addNewValue(assembleDataValue(observation, (P) observation.getDataset(), query));
            }
        }
        return result;
    }

    @Override
    public ProfileValue<V> assembleDataValue(ProfileDataEntity observation, P dataset, DbQuery query) {
        ProfileValue<V> profile = createValue(observation, dataset, query);
        return addMetadatasIfNeeded(observation, profile, dataset, query);
    }

    protected ProfileValue<V> createProfileValue(ProfileDataEntity observation, DbQuery query) {
        ProfileValue<V> profile = prepareValue(observation, query);
        profile.setVerticalExtent(createVerticalExtent(observation));
        return profile;
    }

    private VerticalExtentOutput createVerticalExtent(ProfileDataEntity observation) {
        VerticalExtentOutput verticalExtent = new VerticalExtentOutput();
        if (observation.getDataset().hasVerticalMetadata()) {
            VerticalMetadataEntity verticalMetadata = observation.getDataset().getVerticalMetadata();
            verticalExtent.setUom(
                    verticalMetadata.hasVerticalUnit() ? verticalMetadata.getVerticalUnit().getSymbol() : null);
            if (verticalMetadata.isSetOrientation()) {
                verticalExtent.setOrientation(verticalMetadata.getOrientation());
            }
            if (verticalMetadata.isSetVerticalOriginName()) {
                verticalExtent.setVerticalOrigin(verticalMetadata.getVerticalOriginName());
            }
            verticalExtent.setFrom(new VerticalExtentValueOutput(getVerticalFromName(verticalMetadata),
                    format(observation.getVerticalFrom(), observation.getDataset())));
            verticalExtent.setTo(new VerticalExtentValueOutput(getVerticalToName(verticalMetadata),
                    format(observation.getVerticalTo(), observation.getDataset())));
            for (DataEntity<?> value : observation.getValue()) {
                verticalExtent.setInterval(value.hasVerticalInterval());
                break;
            }
        }
        return verticalExtent;
    }

    private String getVerticalFromName(VerticalMetadataEntity verticalMetadata) {
        return verticalMetadata.isSetVerticalFromName() ? verticalMetadata.getVerticalFromName()
                : getVerticalToName(verticalMetadata);
    }

    private String getVerticalToName(VerticalMetadataEntity verticalMetadata) {
        return verticalMetadata.isSetVerticalToName() ? verticalMetadata.getVerticalToName()
                : getNameFromOrientation(verticalMetadata);
    }

    private String getNameFromOrientation(VerticalMetadataEntity verticalMetadata) {
        return verticalMetadata.getOrientation() != null && verticalMetadata.getOrientation() > 0 ? "height" : "depth";
    }

    protected abstract ProfileValue<V> createValue(ProfileDataEntity observation,
                                                   DatasetEntity dataset,
                                                   DbQuery query);

    protected <E extends DataEntity<V>> ProfileDataItem<V> assembleDataItem(E dataEntity,
                                                                            ProfileValue<T> profile,
                                                                            ProfileDataEntity observation,
                                                                            DbQuery query) {
        ProfileDataItem<V> dataItem = new ProfileDataItem<>();
        dataItem.setValue(dataEntity.getValue());
        dataItem.setDetectionLimit(getDetectionLimit(dataEntity));

        BigDecimal verticalTo = format(dataEntity.getVerticalTo(), dataEntity.getDataset());
        BigDecimal verticalFrom = format(dataEntity.getVerticalFrom(), dataEntity.getDataset());

        dataItem.setVertical(verticalTo);
        if (dataEntity.hasVerticalInterval()) {
            dataItem.setVerticalFrom(verticalFrom);
        }
        return dataItem;
    }

    protected <E extends DataEntity<T>> ProfileDataItem<T> assembleDataItem(E dataEntity,
                                                                            ProfileValue<T> profile,
                                                                            Set<Map<String, Object>> parameters,
                                                                            DatasetEntity dataset,
                                                                            DbQuery query) {
        ProfileDataItem<T> dataItem = new ProfileDataItem<>();
        dataItem.setValue(dataEntity.getValue());
        dataItem.setDetectionLimit(getDetectionLimit(dataEntity));
        return dataItem;
    }
}
