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

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.dao.DbQuery;
import org.slf4j.Logger;

public interface OutputMapper<T extends ParameterOutput, S extends DescribableEntity> {

    T createCondensed(S entity, DbQuery query);

    default T createCondensed(T result, S entity, DbQuery query) {
        return condensed(result, entity, query);
    }

    default T condensed(T result, DescribableEntity entity, DbQuery query) {
        try {
            IoParameters parameters = query.getParameters();
            String id = Long.toString(entity.getId());
            result.setId(id);
            if (parameters.isSelected(ParameterOutput.LABEL)) {
                result.setValue(ParameterOutput.LABEL, entity.getLabelFrom(query.getLocaleForLabel()), parameters,
                        result::setLabel);
            }
            if (parameters.isSelected(ParameterOutput.DOMAIN_ID)) {
                result.setValue(ParameterOutput.DOMAIN_ID, entity.getIdentifier(), parameters, result::setDomainId);
            }
            if (parameters.isSelected(ParameterOutput.HREF)) {
                result.setValue(ParameterOutput.HREF, query.getHrefBase(), parameters,
                        result::setHref);
            }
            return result;
        } catch (Exception e) {
            log(entity, e);
        }
        return null;
    }

    T createExpanded(S entity, DbQuery query, Session session);

    Logger getLogger();

    default void log(DescribableEntity entity, Exception e) {
        getLogger().error("Error while processing {} with id {}! Exception: {}", entity.getClass().getSimpleName(),
                entity.getId(), e);
    }

}
