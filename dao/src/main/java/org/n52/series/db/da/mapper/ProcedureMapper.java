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
import org.n52.io.response.HierarchicalParameterOutput;
import org.n52.io.response.ProcedureOutput;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.dao.DbQuery;

public class ProcedureMapper extends AbstractOuputMapper<ProcedureOutput, ProcedureEntity> {

    public ProcedureMapper(MapperFactory mapperFactory, IoParameters params) {
        super(mapperFactory, params, false);
    }

    public ProcedureMapper(MapperFactory mapperFactory, IoParameters params, boolean subMapper) {
        super(mapperFactory, params, subMapper);
    }

    @Override
    public ProcedureOutput createCondensed(ProcedureEntity entity, DbQuery query) {
        return createCondensed(new ProcedureOutput(), entity, query);
    }

    @Override
    public ProcedureOutput createExpanded(ProcedureEntity entity, DbQuery query, Session session) {
        try {
            ProcedureOutput result = createCondensed(entity, query);
            addService(result, entity, query);
            if (entity.hasParents() && query.getParameters().isSelected(ProcedureOutput.PARENTS)) {
                result.setValue(HierarchicalParameterOutput.PARENTS,
                        createCondensed(entity.getParents(), query, session), query.getParameters(),
                        result::setParents);
            }
            if (entity.hasChildren() && query.getParameters().isSelected(ProcedureOutput.CHILDREN)) {
                result.setValue(HierarchicalParameterOutput.CHILDREN,
                        createCondensed(entity.getChildren(), query, session), query.getParameters(),
                        result::setChildren);
            }
            return result;
        } catch (Exception e) {
            log(entity, e);
        }
        return null;
    }

}
