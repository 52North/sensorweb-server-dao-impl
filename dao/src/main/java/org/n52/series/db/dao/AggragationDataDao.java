package org.n52.series.db.dao;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.AggregationQuantityDataEntity;
import org.n52.series.db.beans.AggregationTypeEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.AbstractQuantityDataEntity;
import org.n52.series.db.da.AveragingTimeUtil;

public class AggragationDataDao extends DataDao<AbstractQuantityDataEntity> {

    public AggragationDataDao(Session session) {
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

    protected Criteria getAllInstancesForCriteria(DatasetEntity series, DbQuery query) throws DataAccessException {
        Criteria criteria = super.getAllInstancesForCriteria(series, query);
        criteria.createCriteria(AggregationQuantityDataEntity.PROPERTY_AGGREGATION, "at")
                .add(Restrictions.eq(AggregationTypeEntity.PROPERTY_DOMAIN_ID, AveragingTimeUtil.getAggregationType(query)));
        return criteria;
    }

    @Override
    public boolean hasInstance(Long id, DbQuery query) {
        return session.get(getEntity(), id) != null;
    }

    private Class<AggregationQuantityDataEntity> getEntity() {
        return AggregationQuantityDataEntity.class;
    }
}
