package org.n52.series.springdata;


import org.n52.series.db.beans.FeatureEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface FeatureRepository extends ParameterDataRepository<FeatureEntity> {

}
