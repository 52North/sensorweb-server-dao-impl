package org.n52.series.db.dao;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.I18nEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ParameterDao<T extends DescribableEntity, I extends I18nEntity> extends AbstractDao<T> implements SearchableDao<T> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterDao.class);

    public ParameterDao(Session session) {
        super(session);
    }

    protected abstract Class<I> getI18NEntityClass();

    @Override
    @SuppressWarnings("unchecked")
    public List<T> find(DbQuery query) {
        LOGGER.debug("find instance: {}", query);
        Criteria criteria = getDefaultCriteria(query);
        criteria = i18n(getI18NEntityClass(), criteria, query);
        criteria.add(Restrictions.ilike(DescribableEntity.PROPERTY_NAME, "%" + query.getSearchTerm() + "%"));
        return query.addFilters(criteria, getDatasetProperty())
                    .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getAllInstances(DbQuery query) throws DataAccessException {
        LOGGER.debug("get all instances: {}", query);
        Criteria criteria = getDefaultCriteria(query);
        criteria = i18n(getI18NEntityClass(), criteria, query);
        return query.addFilters(criteria, getDatasetProperty())
                    .list();
    }
}
