package org.n52.series.db.dao;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.n52.series.db.beans.AbstractQuantityDataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QuantityDataEntity;

public class QuantityDataDao extends DataDao<AbstractQuantityDataEntity> {

    public QuantityDataDao(Session session) {
        super(session);
    }

    @Override
    public Criteria getDefaultCriteria(String alias, DbQuery query) {
        return getDefaultCriteria(alias, query, getEntity());
    }

    @Override
    public Criteria getDefaultCriteria(DbQuery query) {
        return addRestrictions(session.createCriteria(getEntity()), query);
    }

    protected Criteria getDefaultCriteria(DbQuery query, DatasetEntity series) {
        return addRestrictions(session.createCriteria(getEntity()), query);
    }

    @Override
    public boolean hasInstance(Long id, DbQuery query) {
        return session.get(getEntity(), id) != null;
    }

    private Class<QuantityDataEntity> getEntity() {
        return QuantityDataEntity.class;
    }
}
