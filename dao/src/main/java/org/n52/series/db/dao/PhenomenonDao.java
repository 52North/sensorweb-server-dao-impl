/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.n52.series.db.DataModelUtil;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ObservationConstellationEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.i18n.I18nPhenomenonEntity;
import org.n52.shetland.ogc.om.AbstractPhenomenon;
import org.n52.shetland.ogc.om.OmCompositePhenomenon;
import org.n52.shetland.ogc.om.OmObservableProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PhenomenonDao extends ParameterDao<PhenomenonEntity, I18nPhenomenonEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenomenonDao.class);

    public PhenomenonDao(Session session) {
        super(session);
    }

    @Override
    protected String getDatasetProperty() {
        return QueryUtils.createAssociation(DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION,
                                            ObservationConstellationEntity.OBSERVABLE_PROPERTY);
    }

    @Override
    protected DetachedCriteria projectOnDatasetParameterId(DetachedCriteria subquery) {
        return subquery.createCriteria(DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION)
                       .createCriteria(ObservationConstellationEntity.OBSERVABLE_PROPERTY)
                       .setProjection(Projections.property(DescribableEntity.PROPERTY_ID));
    }

    @Override
    protected Class<PhenomenonEntity> getEntityClass() {
        return PhenomenonEntity.class;
    }

    @Override
    protected Class<I18nPhenomenonEntity> getI18NEntityClass() {
        return I18nPhenomenonEntity.class;
    }

    public Map<String, PhenomenonEntity> getOrInsertAsMap(Collection<? extends AbstractPhenomenon> observableProperties,
            boolean hiddenChild) {
        Map<String, PhenomenonEntity> existing = getExisting(observableProperties);
        insertNonExisting(observableProperties, hiddenChild, existing);
        insertHierachy(observableProperties, existing);
        return existing;
    }

    protected Map<String, PhenomenonEntity> getExistingObservableProperties(
            List<? extends AbstractPhenomenon> observableProperty) {
        List<String> identifiers = getIdentifiers(observableProperty);
        return getAsMap(identifiers);
    }

    protected List<String> getIdentifiers(Collection<? extends AbstractPhenomenon> observableProperty) {
        List<String> identifiers = new ArrayList<>(observableProperty.size());
        for (AbstractPhenomenon sosObservableProperty : observableProperty) {
            identifiers.add(sosObservableProperty.getIdentifier());
            if (sosObservableProperty instanceof OmCompositePhenomenon) {
                OmCompositePhenomenon parent = (OmCompositePhenomenon) sosObservableProperty;
                for (OmObservableProperty child : parent.getPhenomenonComponents()) {
                    identifiers.add(child.getIdentifier());
                }
            }
        }
        return identifiers;
    }

    protected Map<String, PhenomenonEntity> getAsMap(Collection<String> identifiers) {
        List<PhenomenonEntity> obsProps = get(identifiers);
        Map<String, PhenomenonEntity> existing = new HashMap<>(identifiers.size());
        for (PhenomenonEntity obsProp : obsProps) {
            existing.put(obsProp.getIdentifier(), obsProp);
        }
        return existing;
    }

    protected Map<String, PhenomenonEntity> getExisting(
            Collection<? extends AbstractPhenomenon> observableProperty) {
        List<String> identifiers = getIdentifiers(observableProperty);
        return getAsMap(identifiers);
    }

    protected void insertNonExisting(Collection<? extends AbstractPhenomenon> observableProperties, boolean hiddenChild,
            Map<String, PhenomenonEntity> existing)
            throws HibernateException {
        for (AbstractPhenomenon sosObsProp : observableProperties) {
            insertNonExisting(sosObsProp, hiddenChild, existing);
        }
    }

    protected void insertNonExisting(AbstractPhenomenon sosObsProp, boolean hiddenChild,
            Map<String, PhenomenonEntity> existing)
            throws HibernateException {
        if (!existing.containsKey(sosObsProp.getIdentifier())) {
            PhenomenonEntity obsProp = new PhenomenonEntity();
            // FIXME: how to do this???
//            addIdentifierNameDescription(sosObsProp, obsProp);
            obsProp.setHiddenChild(hiddenChild);
            session.save(obsProp);
            session.flush();
            session.refresh(obsProp);
            existing.put(obsProp.getIdentifier(), obsProp);
        }
        if (sosObsProp instanceof OmCompositePhenomenon) {
            insertNonExisting(((OmCompositePhenomenon) sosObsProp).getPhenomenonComponents(), true, existing);
        }
    }

    protected void insertHierachy(Collection<? extends AbstractPhenomenon> observableProperty,
            Map<String, PhenomenonEntity> existing) {
        for (AbstractPhenomenon sosObsProp : observableProperty) {
            if (sosObsProp instanceof OmCompositePhenomenon) {
                insertHierachy((OmCompositePhenomenon) sosObsProp, existing);
            }
        }
    }

    protected void insertHierachy(OmCompositePhenomenon parent, Map<String, PhenomenonEntity> existing)
            throws HibernateException {
        PhenomenonEntity parentObsProp = get(parent.getIdentifier(), existing);
        for (OmObservableProperty child : parent) {
            PhenomenonEntity childObsProp = get(child.getIdentifier(), existing);
            childObsProp.addParent(parentObsProp);
            session.update(childObsProp);
        }
        // do not save the parent, as it would result in a duplicate key
        // error...
        session.flush();
        session.refresh(parentObsProp);
    }

    private PhenomenonEntity get(String identifier, Map<String, PhenomenonEntity> observableProperties) {
        // TODO check if this is still required
        if (identifier == null) {
            return null;
        }
        PhenomenonEntity observableProperty = observableProperties.get(identifier);
        if (observableProperty != null) {
            return observableProperty;
        }
        observableProperty = get(identifier);
        observableProperties.put(identifier, observableProperty);
        return observableProperty;
    }

    /**
     * Get observable property by identifier
     *
     * @param identifier
     *            The observable property's identifier
     * @return Observable property object
     */
    public PhenomenonEntity get(String identifier) {
        Criteria criteria = session.createCriteria(PhenomenonEntity.class)
                .add(Restrictions.eq(PhenomenonEntity.PROPERTY_DOMAIN_ID, identifier));
        LOGGER.debug("QUERY getObservablePropertyForIdentifier(identifier): {}",
                DataModelUtil.getSqlString(criteria));
        return (PhenomenonEntity) criteria.uniqueResult();
    }

    /**
     * Get observable property objects for observable property identifiers
     *
     * @param identifiers
     *            Observable property identifiers
     * @return Observable property objects
     */
    @SuppressWarnings("unchecked")
    public List<PhenomenonEntity> get(Collection<String> identifiers) {
        Criteria criteria =
                session.createCriteria(PhenomenonEntity.class).add(
                        Restrictions.in(PhenomenonEntity.PROPERTY_DOMAIN_ID, identifiers));
        LOGGER.debug("QUERY getObservableProperties(identifiers): {}", DataModelUtil.getSqlString(criteria));
        return criteria.list();
    }

}
