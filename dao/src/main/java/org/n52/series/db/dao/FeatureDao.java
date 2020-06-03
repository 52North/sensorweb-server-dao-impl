/*
 * Copyright (C) 2015-2020 52°North Initiative for Geospatial Open Source
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
package org.n52.series.db.dao;

import java.util.Collection;
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
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.i18n.I18nFeatureEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class FeatureDao extends ParameterDao<FeatureEntity, I18nFeatureEntity> {

    public FeatureDao(Session session) {
        super(session);
    }

    @Override
    protected String getDatasetProperty() {
        return DatasetEntity.PROPERTY_FEATURE;
    }

    @Override
    protected Class<FeatureEntity> getEntityClass() {
        return FeatureEntity.class;
    }

    @Override
    protected Class<I18nFeatureEntity> getI18NEntityClass() {
        return I18nFeatureEntity.class;
    }

    @Override
    public Collection<FeatureEntity> get(DbQuery query) {
        return getCriteria(query).list();
    }

    @Override
    public List<FeatureEntity> getAllInstances(DbQuery query) throws DataAccessException {
        Set<String> features = query.getParameters().getFeatures();
        if (query.isExpanded() && query.getLevel() != null) {
            if (hasFilterParameter(query)) {
                List<FeatureEntity> children = null;
                if (features != null && !features.isEmpty()) {
                    IoParameters params = query.getParameters().replaceWith(Parameters.FEATURES,
                            toStringList(getChildrenFeatureIds(query, features)));
                    params.removeAllOf(Parameters.MATCH_DOMAIN_IDS);
                    children = super.getAllInstances(new DbQuery(params));
                } else {
                    children = super.getAllInstances(query);
                }
                return createReverse(children, features);
            } else {
                Criteria c = getCriteria(query);
                if (features == null || features.isEmpty()) {
                    c.add(Restrictions.isEmpty(FeatureEntity.PROPERTY_PARENTS));
                }
                return c.list();
            }
        }
        if (features != null && !features.isEmpty()) {
            IoParameters params = query.getParameters().replaceWith(Parameters.FEATURES,
                    toStringList(getChildrenFeatureIds(query, features)));
            params.removeAllOf(Parameters.MATCH_DOMAIN_IDS);
            return super.getAllInstances(new DbQuery(params));
        }
        return super.getAllInstances(query);
    }

    private Criteria getCriteria(DbQuery query) throws DataAccessException {
        Criteria c = getDefaultCriteria();
        IoParameters parameters = query.getParameters();
        if (parameters.getFeatures() != null && !parameters.getFeatures().isEmpty()) {
            c.add(query.getParameters().isMatchDomainIds() ? createDomainIdFilter(parameters.getFeatures())
                    : createIdFilter(parameters.getFeatures()));
        }
        query.addSpatialFilter(c);
        return c;
    }

    private Set<Long> getChildrenFeatureIds(DbQuery query, Set<String> features) {
        Criteria c = getDefaultCriteria();
        c.add(query.getParameters().isMatchDomainIds() ? createDomainIdFilter(features)
                : createIdFilter(features));
        c.createCriteria(FeatureEntity.PROPERTY_CHILDREN, "c");
        c.setProjection(Projections.property("c." + FeatureEntity.PROPERTY_ID));
        return queryRecursiv(new LinkedHashSet<>(c.list()));
    }

    private Set<Long> queryRecursiv(Set<Long> feats) {
        Set<Long> features = new LinkedHashSet<>(feats);
        Criteria c = getDefaultCriteria();
        c.add(createLongIdFilter(feats));
        final String alias = "c";
        c.createCriteria(FeatureEntity.PROPERTY_CHILDREN, alias);
        c.setProjection(Projections.property(alias + "." + FeatureEntity.PROPERTY_ID));
        List result = c.list();
        if (result != null && !result.isEmpty()) {
            result.removeAll(features);
            features.addAll(queryRecursiv(new LinkedHashSet<>(result)));
        }
        return features;
    }

    private Criteria getDefaultCriteria() {
        return session.createCriteria(getEntityClass(), getDefaultAlias())
                .setResultTransformer(RootEntityResultTransformer.INSTANCE);
    }

    private Criterion createDomainIdFilter(Collection<String> filterValues) {
        return filterValues.stream().map(filter -> Restrictions.ilike(FeatureEntity.PROPERTY_DOMAIN_ID, filter))
                .collect(Restrictions::disjunction, Disjunction::add, (a, b) -> b.conditions().forEach(a::add));
    }

    private Criterion createIdFilter(Collection<String> filterValues) {
        return createLongIdFilter(QueryUtils.parseToIds(filterValues));
    }

    private Criterion createLongIdFilter(Collection<Long> filterValues) {
        return Restrictions.in(FeatureEntity.PROPERTY_ID, filterValues);
    }

    private List<String> toStringList(Set<Long> set) {
        return set.stream().map(s -> s.toString()).collect(Collectors.toList());
    }

    private boolean hasFilterParameter(DbQuery query) {
        IoParameters parameters = query.getParameters();
        return parameters.containsParameter(Parameters.DATASETS) || parameters.containsParameter(Parameters.CATEGORIES)
                || parameters.containsParameter(Parameters.OFFERINGS)
                || parameters.containsParameter(Parameters.PHENOMENA)
                || parameters.containsParameter(Parameters.PLATFORMS)
                || parameters.containsParameter(Parameters.PROCEDURES);
    }

    private List<FeatureEntity> createReverse(Collection<FeatureEntity> children, Set<String> filterFeatures) {
        Map<Long, FeatureEntity> roots = new LinkedHashMap<>();
        processReverse(null, children, roots, new LinkedHashMap<>(), filterFeatures);
        return new LinkedList<>(roots.values());
    }

    private void processReverse(Long childId, Collection<FeatureEntity> features,
            Map<Long, FeatureEntity> roots, Map<Long, FeatureEntity> leafs, Set<String> filterFeatures) {
        for (FeatureEntity feature : features) {
            if (childId != null) {
                if (!leafs.containsKey(feature.getId()) && feature.hasChildren()) {
                    feature.getChildren().clear();
                }
                feature.addChild(leafs.get(childId)) ;
            }
            if (feature.hasParents() && notQueried(feature, filterFeatures)) {
                leafs.put(feature.getId(), feature);
                processReverse(feature.getId(), feature.getParents(), roots, leafs, filterFeatures);
            } else {
                roots.put(feature.getId(), feature);
            }
        }
    }

    private boolean notQueried(FeatureEntity feature, Collection<String> filterFeatures) {
        return filterFeatures == null || filterFeatures.isEmpty()
                || !(filterFeatures.contains(feature.getId().toString())
                || filterFeatures.contains(feature.getIdentifier()));
    }

}
