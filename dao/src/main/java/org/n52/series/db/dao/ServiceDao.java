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
package org.n52.series.db.dao;

import org.hibernate.Session;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.i18n.I18nServiceEntity;

public class ServiceDao extends ParameterDao<ServiceEntity, I18nServiceEntity> {

    private static final String SERIES_PROPERTY = "service";

    public ServiceDao(Session session) {
        super(session);
    }

    @Override
    protected Class<ServiceEntity> getEntityClass() {
        return ServiceEntity.class;
    }

    @Override
    protected String getDatasetProperty() {
        return SERIES_PROPERTY;
    }

    @Override
    protected Class<I18nServiceEntity> getI18NEntityClass() {
        return I18nServiceEntity.class;
    }

}
