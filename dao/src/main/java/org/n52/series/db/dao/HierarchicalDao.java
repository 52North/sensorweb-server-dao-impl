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
package org.n52.series.db.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.RootEntityResultTransformer;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataModelUtil;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.HierarchicalEntity;
import org.n52.series.db.beans.IdEntity;
import org.n52.series.db.beans.i18n.I18nEntity;

public abstract class HierarchicalDao<T extends HierarchicalEntity<T>, I extends I18nEntity<T>>
        extends ParameterDao<T, I> {

    public HierarchicalDao(Session session) {
        super(session);
    }

    @Override
    public List<T> getAllInstances(DbQuery q) throws DataAccessException {
        DbQuery query = checkLevelParameterForHierarchyQuery(q);
        Set<String> entities = getParameter(query);
        if (query.isExpanded() && query.getLevel() != null) {
            if (hasFilterParameter(query)) {
                List<T> children = null;
                if (entities != null && !entities.isEmpty()) {
                    children =
                            super.getAllInstances(updateQuery(query, toStringList(getChildrenIds(query, entities))));
                } else {
                    children = super.getAllInstances(query);
                }
                return createReverse(children, entities);
            } else {
                Criteria c = getCriteria(query);
                if (entities == null || entities.isEmpty()) {
                    c.add(Restrictions.isEmpty(HierarchicalEntity.PROPERTY_PARENTS));
                }
                return c.list();
            }
        }
        if (entities != null && !entities.isEmpty()) {
            List<String> ids = toStringList(getChildrenIds(query, entities));
            if (ids != null && !ids.isEmpty()) {
                if (query.isMatchDomainIds()) {
                    ids.addAll(getAllInstances(query).stream().map(f -> f.getId().toString())
                            .collect(Collectors.toSet()));
                } else {
                    ids.addAll(entities);
                }
                return super.getAllInstances(updateQuery(query, ids));
            }
        }
        return super.getAllInstances(query);
    }

    protected Criteria getCriteria(DbQuery query) throws DataAccessException {
        Criteria c = getDefaultCriteria();
        Set<String> parameters = getParameter(query);
        if (parameters != null && !parameters.isEmpty()) {
            c.add(query.getParameters().isMatchDomainIds() ? createDomainIdFilter(parameters)
                    : createIdFilter(parameters));
        }
        query.addSpatialFilter(c);
        return c;
    }

    protected abstract IoParameters replaceParameter(DbQuery query, Collection<String> entites);

    protected abstract Set<String> getParameter(DbQuery query);

    public abstract Set<Long> getChildrenIds(DbQuery query);

    protected Set<Long> getChildrenIds(DbQuery query, Set<String> entities) {
        return getChildrenIds(query, entities, Integer.MAX_VALUE);
    }

    protected Set<Long> getChildrenIds(DbQuery query, Set<String> entities, int level) {
        Criteria c = getDefaultCriteria();
        c.add(query.getParameters().isMatchDomainIds() ? createDomainIdFilter(entities) : createIdFilter(entities));
        if (checkChildrenProperty()) {
            c.createCriteria(HierarchicalEntity.PROPERTY_CHILDREN, "c");
            c.setProjection(Projections.property("c." + IdEntity.PROPERTY_ID));
            return queryRecursiv(new LinkedHashSet<>(c.list()), level - 1);
        }
        return Collections.emptySet();
    }

    private boolean checkChildrenProperty() {
        return DataModelUtil.isPropertyNameSupported(HierarchicalEntity.PROPERTY_CHILDREN, getEntityClass(), session);
    }

    protected DbQuery updateQuery(DbQuery query, Collection<String> entities) {
        IoParameters params =  replaceParameter(query, entities).removeAllOf(Parameters.MATCH_DOMAIN_IDS);
        return new DbQuery(params);
    }

    protected Set<Long> queryRecursiv(Set<Long> entities, int level) {
        Set<Long> features = new LinkedHashSet<>(entities);
        if (level > 0) {
            Criteria c = getDefaultCriteria();
            c.add(createLongIdFilter(entities));
            final String alias = "c";
            c.createCriteria(HierarchicalEntity.PROPERTY_CHILDREN, alias);
            c.setProjection(Projections.property(alias + "." + IdEntity.PROPERTY_ID));
            List result = c.list();
            if (result != null && !result.isEmpty()) {
                result.removeAll(entities);
                features.addAll(queryRecursiv(new LinkedHashSet<>(result), level - 1));
            }
        }
        return features;
    }

    protected Criteria getDefaultCriteria() {
        return session.createCriteria(getEntityClass(), getDefaultAlias())
                .setResultTransformer(RootEntityResultTransformer.INSTANCE);
    }

    protected Criterion createDomainIdFilter(Collection<String> filterValues) {
        return filterValues.stream().map(filter -> Restrictions.ilike(DescribableEntity.PROPERTY_DOMAIN_ID, filter))
                .collect(Restrictions::disjunction, Disjunction::add, (a, b) -> b.conditions().forEach(a::add));
    }

    protected Criterion createIdFilter(Collection<String> filterValues) {
        return createLongIdFilter(QueryUtils.parseToIds(filterValues));
    }

    protected Criterion createLongIdFilter(Collection<Long> filterValues) {
        return Restrictions.in(IdEntity.PROPERTY_ID, filterValues);
    }

    protected boolean hasFilterParameter(DbQuery query) {
        IoParameters parameters = query.getParameters();
        return parameters.containsParameter(Parameters.DATASETS) || parameters.containsParameter(Parameters.CATEGORIES)
                || parameters.containsParameter(Parameters.OFFERINGS)
                || parameters.containsParameter(Parameters.PHENOMENA)
                || parameters.containsParameter(Parameters.PLATFORMS)
                || parameters.containsParameter(Parameters.PROCEDURES);
    }

    protected List<T> createReverse(Collection<T> children, Set<String> filtered) {
        Map<Long, T> roots = new LinkedHashMap<>();
        processReverse(null, children, roots, new LinkedHashMap<>(), filtered);
        return new LinkedList<>(roots.values());
    }

    protected void processReverse(Long childId, Collection<T> entities, Map<Long, T> roots, Map<Long, T> leafs,
            Set<String> filtered) {
        for (T entity : entities) {
            if (childId != null) {
                if (!leafs.containsKey(entity.getId()) && entity.hasChildren()) {
                    entity.getChildren().clear();
                }
                entity.addChild(leafs.get(childId));
            }
            if (entity.hasParents() && notQueried(entity, filtered)) {
                leafs.put(entity.getId(), entity);
                processReverse(entity.getId(), entity.getParents(), roots, leafs, filtered);
            } else {
                roots.put(entity.getId(), entity);
            }
        }
    }

    private boolean notQueried(T entity, Collection<String> filtered) {
        return filtered == null || filtered.isEmpty()
                || !(filtered.contains(entity.getId().toString()) || filtered.contains(entity.getIdentifier()));
    }
}
