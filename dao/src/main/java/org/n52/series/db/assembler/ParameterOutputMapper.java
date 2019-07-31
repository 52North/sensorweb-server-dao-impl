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
package org.n52.series.db.assembler;

import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DefaultDbQueryFactory;

public final class ParameterOutputMapper implements OutputMapper {

    private final DbQuery query;

    public ParameterOutputMapper(final DbQuery query) {
        this.query = query == null ? new DefaultDbQueryFactory().createDefault() : query;
    }

    @Override
    public <E extends DescribableEntity, O extends ParameterOutput> O createCondensed(final E entity, final O output) {
        final IoParameters parameters = query.getParameters();

        final Long id = entity.getId();
        final String label = entity.getLabelFrom(query.getLocale());
        final String domainId = entity.getIdentifier();
        final String hrefBase = query.getHrefBase();

        output.setId(Long.toString(id));
        output.setValue(ParameterOutput.LABEL, label, parameters, output::setLabel);
        output.setValue(ParameterOutput.DOMAIN_ID, domainId, parameters, output::setDomainId);
        if (!parameters.shallBehaveBackwardsCompatible()) {
            output.setValue(ParameterOutput.HREF_BASE, hrefBase, parameters, output::setHrefBase);
        }
        return output;
    }
}
