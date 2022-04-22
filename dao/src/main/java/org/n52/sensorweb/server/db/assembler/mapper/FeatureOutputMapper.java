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
package org.n52.sensorweb.server.db.assembler.mapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.n52.io.response.FeatureOutput;
import org.n52.io.response.HierarchicalParameterOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.parameter.BooleanParameterEntity;
import org.n52.series.db.beans.parameter.CategoryParameterEntity;
import org.n52.series.db.beans.parameter.ComplexParameterEntity;
import org.n52.series.db.beans.parameter.CountParameterEntity;
import org.n52.series.db.beans.parameter.JsonParameterEntity;
import org.n52.series.db.beans.parameter.ParameterEntity;
import org.n52.series.db.beans.parameter.QuantityParameterEntity;
import org.n52.series.db.beans.parameter.TextParameterEntity;
import org.n52.series.db.beans.parameter.XmlParameterEntity;
import org.n52.series.db.beans.parameter.feature.FeatureComplexParameterEntity;
import org.n52.series.db.beans.parameter.feature.FeatureParameterEntity;

public class FeatureOutputMapper extends ParameterOutputSearchResultMapper<AbstractFeatureEntity, FeatureOutput> {

    private ParameterCreator creator = new ParameterCreator();

    public FeatureOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory) {
        this(query, outputMapperFactory, false);
    }

    public FeatureOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory, boolean subMapper) {
        super(query, outputMapperFactory, subMapper);
    }

    @Override
    public FeatureOutput createCondensed(AbstractFeatureEntity entity, FeatureOutput output) {
        FeatureOutput result = super.createCondensed(entity, output);
        if (getDbQuery().getParameters().isSelected(FeatureOutput.GEOMETRY)) {
            result.setValue(FeatureOutput.GEOMETRY, createGeometry(entity), getDbQuery().getParameters(),
                    result::setGeometry);
        }
        return result;
    }

    @Override
    public FeatureOutput addExpandedValues(AbstractFeatureEntity entity, FeatureOutput output) {
        return addExpandedValues(entity, output, false, false, getDbQuery().getLevel());
    }

    protected FeatureOutput addExpandedValues(AbstractFeatureEntity entity, FeatureOutput output, boolean isParent,
            boolean isChild, Integer level) {
        if (!isParent && !isChild && getDbQuery().getParameters().isSelected(FeatureOutput.PROPERTIES)) {
            Map<String, DatasetParameters> timeseriesList = createTimeseriesList(entity.getDatasets());
            output.setValue(FeatureOutput.PROPERTIES, timeseriesList, getDbQuery().getParameters(),
                    output::setDatasets);
            if (entity.hasParameters()) {
                output.setValue(FeatureOutput.PARAMETERS, createParameters(entity), getDbQuery().getParameters(),
                        output::setParameters);
            }
        }
        if (entity instanceof FeatureEntity) {
            if (!isParent && !isChild && entity.hasParents()
                    && getDbQuery().getParameters().isSelected(HierarchicalParameterOutput.PARENTS)) {
                List<FeatureOutput> parents = getMemberList(entity.getParents(), level, true, false);
                output.setValue(HierarchicalParameterOutput.PARENTS, parents, getDbQuery().getParameters(),
                        output::setParents);
            }
            if (level != null && level > 0) {
                if ((!isParent && !isChild || !isParent && isChild) && entity.hasChildren()
                        && getDbQuery().getParameters().isSelected(HierarchicalParameterOutput.CHILDREN)) {
                    List<FeatureOutput> children = getMemberList(entity.getChildren(), level - 1, false, true);
                    output.setValue(HierarchicalParameterOutput.CHILDREN, children, getDbQuery().getParameters(),
                            output::setChildren);
                }
            }
        }
        return output;
    }

    protected List<FeatureOutput> getMemberList(Set<AbstractFeatureEntity> entities, Integer level,
            boolean isNotParent, boolean isNotChild) {
        List<FeatureOutput> list = new LinkedList<>();
        for (AbstractFeatureEntity e : entities) {
            list.add(createExpanded(e, getParameterOuput(), isNotParent, isNotChild, level));
        }
        return list;
    }

    private FeatureOutput createExpanded(AbstractFeatureEntity entity, FeatureOutput output, boolean isParent,
            boolean isChild, Integer level) {
        createCondensed(entity, output);
        super.addExpandedValues(entity, output);
        addExpandedValues(entity, output, isParent, isChild, level);
        return output;
    }

    private Map<String, DatasetParameters> createTimeseriesList(Collection<DatasetEntity> series) {
        Map<String, DatasetParameters> timeseriesOutputs = new HashMap<>();
        DatasetParameterChecker checker = new DatasetParameterChecker(getDbQuery());
        for (DatasetEntity dataset : series) {
            if (checker.check(dataset)) {
                String timeseriesId = Long.toString(dataset.getId());
                timeseriesOutputs.put(timeseriesId, createTimeseriesOutput(dataset, getDbQuery()));
            }
        }
        return timeseriesOutputs;
    }

    private Map<String, Object> createParameters(AbstractFeatureEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (ParameterEntity<?> parameter : entity.getParameters()) {
            map.putAll(creator.visit(parameter));
        }
        return map;
    }

    @Override
    public FeatureOutput getParameterOuput() {
        return new FeatureOutput();
    }

    public static class ParameterCreator {

        private static final String UOM = "uom";
        private static final String DESCRIPTION = "description";
        private static final String DOMAIN = "domain";
        private static final String LAST_UPDATE = "lastUpdate";

        public Map<String, Object> visit(ParameterEntity<?> parameter) {
            if (parameter instanceof QuantityParameterEntity) {
                return visit((QuantityParameterEntity) parameter, setCommonValues(parameter));
            } else if (parameter instanceof CountParameterEntity) {
                return visit((CountParameterEntity) parameter, setCommonValues(parameter));
            } else if (parameter instanceof BooleanParameterEntity) {
                return visit((BooleanParameterEntity) parameter, setCommonValues(parameter));
            } else if (parameter instanceof CategoryParameterEntity) {
                return visit((CategoryParameterEntity) parameter, setCommonValues(parameter));
            } else if (parameter instanceof TextParameterEntity) {
                return visit((TextParameterEntity) parameter, setCommonValues(parameter));
            } else if (parameter instanceof XmlParameterEntity) {
                return visit((XmlParameterEntity) parameter, setCommonValues(parameter));
            } else if (parameter instanceof JsonParameterEntity) {
                return visit((JsonParameterEntity) parameter, setCommonValues(parameter));
            } else if (parameter instanceof ComplexParameterEntity) {
                return visit((ComplexParameterEntity) parameter, setCommonValues(parameter));
            }
            return null;
        }

        public Map<String, Object> visit(QuantityParameterEntity parameter, Map<String, Object> map) {
            if (parameter.isSetUnit()) {
                map.put(UOM, parameter.getUnit().getSymbol());
            }
            if (parameter.isSetValue()) {
                map.put(parameter.getName(), parameter.getValue().doubleValue());
            }
            return map;
        }

        public Map<String, Object> visit(BooleanParameterEntity parameter, Map<String, Object> map) {
            if (parameter.isSetValue()) {
                map.put(parameter.getName(), parameter.getValue());
            }
            return map;
        }

        public Map<String, Object> visit(CategoryParameterEntity parameter, Map<String, Object> map) {
            if (parameter.isSetUnit()) {
                map.put(UOM, parameter.getUnit().getSymbol());
            }
            if (parameter.isSetValue()) {
                map.put(parameter.getName(), parameter.getValue());
            }
            return map;
        }

        public Map<String, Object> visit(ComplexParameterEntity parameter, Map<String, Object> map) {
            Map<String, Object> subMap = new LinkedHashMap<>();
            for (FeatureParameterEntity<?> param : ((FeatureComplexParameterEntity) parameter).getValue()) {
                subMap.putAll(visit(param));
            }
            map.put(parameter.getName(), subMap);
            return map;
        }

        public Map<String, Object> visit(CountParameterEntity parameter, Map<String, Object> map) {
            if (parameter.isSetValue()) {
                map.put(parameter.getName(), parameter.getValue());
            }
            return map;
        }

        public Map<String, Object> visit(TextParameterEntity parameter, Map<String, Object> map) {
            if (parameter.isSetValue()) {
                map.put(parameter.getName(), parameter.getValue());
            }
            return map;
        }

        public Map<String, Object> visit(XmlParameterEntity parameter, Map<String, Object> map) {
            if (parameter.isSetValue()) {
                map.put(parameter.getName(), parameter.getValue());
            }
            return map;
        }

        public Map<String, Object> visit(JsonParameterEntity parameter, Map<String, Object> map) {
            if (parameter.isSetValue()) {
                map.put(parameter.getName(), parameter.getValue());
            }
            return map;
        }

        private Map<String, Object> setCommonValues(ParameterEntity<?> p) {
            Map<String, Object> map = new LinkedHashMap<>();
            if (p.isSetDescription()) {
                map.put(DESCRIPTION, p.getDescription());
            }
            if (p.isSetDomain()) {
                map.put(DOMAIN, p.getDescription());
            }
            if (p.isSetLastUpdate()) {
                map.put(LAST_UPDATE, p.getDescription());
            }
            return map;
        }

    }

}
