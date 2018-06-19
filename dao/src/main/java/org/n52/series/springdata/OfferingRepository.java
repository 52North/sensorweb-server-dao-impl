package org.n52.series.springdata;


import org.n52.series.db.beans.OfferingEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface OfferingRepository extends ParameterDataRepository<OfferingEntity> {

}
