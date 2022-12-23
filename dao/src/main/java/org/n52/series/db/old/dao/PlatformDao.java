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
package org.n52.series.db.old.dao;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.n52.io.response.PlatformOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.beans.i18n.I18nPlatformEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PlatformDao extends ParameterDao<PlatformEntity, I18nPlatformEntity> {

    public PlatformDao(Session session) {
        super(session);
    }

    @Override
    protected String getDatasetProperty() {
        return DatasetEntity.PROPERTY_PLATFORM;
    }

    @Override
    protected Class<PlatformEntity> getEntityClass() {
        return PlatformEntity.class;
    }

    @Override
    protected Class<I18nPlatformEntity> getI18NEntityClass() {
        return I18nPlatformEntity.class;
    }

    @Override
    protected Criteria addFetchModes(Criteria criteria, DbQuery query, boolean instance) {
        super.addFetchModes(criteria, query, instance);
        if ((query.isExpanded() || instance) && query.getParameters().isSelected(PlatformOutput.DATASETS)) {
            criteria.setFetchMode(PlatformEntity.PROPERTY_DATASETS, FetchMode.JOIN);
            criteria.setFetchMode(getFetchPath(PlatformEntity.PROPERTY_DATASETS, DatasetEntity.PROPERTY_PROCEDURE),
                    FetchMode.JOIN);
            criteria.setFetchMode(getFetchPath(PlatformEntity.PROPERTY_DATASETS, DatasetEntity.PROPERTY_PHENOMENON),
                    FetchMode.JOIN);
            criteria.setFetchMode(getFetchPath(PlatformEntity.PROPERTY_DATASETS, DatasetEntity.PROPERTY_OFFERING),
                    FetchMode.JOIN);
            criteria.setFetchMode(getFetchPath(PlatformEntity.PROPERTY_DATASETS, DatasetEntity.PROPERTY_FEATURE),
                    FetchMode.JOIN);
            criteria.setFetchMode(getFetchPath(PlatformEntity.PROPERTY_DATASETS, DatasetEntity.PROPERTY_UNIT),
                    FetchMode.JOIN);
            if (!query.isDefaultLocal()) {
                criteria.setFetchMode(getFetchPath(PlatformEntity.PROPERTY_DATASETS,
                        DatasetEntity.PROPERTY_PROCEDURE, TRANSLATIONS_ALIAS), FetchMode.JOIN);
                criteria.setFetchMode(getFetchPath(PlatformEntity.PROPERTY_DATASETS,
                        DatasetEntity.PROPERTY_PHENOMENON, TRANSLATIONS_ALIAS), FetchMode.JOIN);
                criteria.setFetchMode(getFetchPath(PlatformEntity.PROPERTY_DATASETS,
                        DatasetEntity.PROPERTY_OFFERING, TRANSLATIONS_ALIAS), FetchMode.JOIN);
                criteria.setFetchMode(getFetchPath(PlatformEntity.PROPERTY_DATASETS,
                        DatasetEntity.PROPERTY_FEATURE, TRANSLATIONS_ALIAS), FetchMode.JOIN);
                criteria.setFetchMode(getFetchPath(PlatformEntity.PROPERTY_DATASETS,
                        DatasetEntity.PROPERTY_CATEGORY, TRANSLATIONS_ALIAS), FetchMode.JOIN);
                criteria.setFetchMode(getFetchPath(PlatformEntity.PROPERTY_DATASETS,
                        DatasetEntity.PROPERTY_UNIT, TRANSLATIONS_ALIAS), FetchMode.JOIN);

            }
        }
        return criteria;
    }
}
