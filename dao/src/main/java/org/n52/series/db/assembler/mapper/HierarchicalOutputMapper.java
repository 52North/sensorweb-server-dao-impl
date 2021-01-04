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
package org.n52.series.db.assembler.mapper;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.n52.io.response.HierarchicalParameterOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.HierarchicalEntity;

public abstract class HierarchicalOutputMapper<E extends HierarchicalEntity<E>,
                                               O extends HierarchicalParameterOutput<O>>
        extends ParameterOutputSearchResultMapper<E, O> {

    public HierarchicalOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory) {
        super(query, outputMapperFactory);
    }

    @Override
    public O addExpandedValues(E entity, O output) {
        return addExpandedValues(entity, output, false, false, query.getLevel());
    }

    protected O addExpandedValues(E entity, O output, boolean isParent, boolean isChild, Integer level) {
        if (!isParent && !isChild && entity.hasParents()) {
            List<O> parents = getMemberList(entity.getParents(), level, true, false);
            output.setValue(HierarchicalParameterOutput.PARENTS, parents, query.getParameters(), output::setParents);
        }
        if (level != null && level > 0) {
            if (((!isParent && !isChild) || (!isParent && isChild)) && entity.hasChildren()) {
                List<O> children = getMemberList(entity.getChildren(), level - 1, false, true);
                output.setValue(HierarchicalParameterOutput.CHILDREN, children, query.getParameters(),
                        output::setChildren);
            }
        }
        return output;
    }

    protected List<O> getMemberList(Set<E> entities, Integer level, boolean isNotParent, boolean isNotChild) {
        List<O> list = new LinkedList<>();
        for (E e : entities) {
            list.add(createExpanded(e, getParameterOuput(), isNotParent, isNotChild, level));
        }
        return list;
    }

    private O createExpanded(E entity, O output, boolean isParent, boolean isChild, Integer level) {
        createCondensed(entity, output);
        super.addExpandedValues(entity, output);
        addExpandedValues(entity, output, isParent, isChild, level);
        return output;
    }

}
