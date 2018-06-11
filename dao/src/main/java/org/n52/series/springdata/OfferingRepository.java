package org.n52.series.springdata;


import org.n52.series.db.beans.OfferingEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface OfferingRepository extends ParameterDataRepository<OfferingEntity> {

}
