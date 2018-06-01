package org.n52.series.springdata;

import javax.transaction.Transactional;

import org.n52.series.db.beans.OfferingEntity;

@Transactional
public interface OfferingDataRepository extends ParameterDataRepository<OfferingEntity> {

}
