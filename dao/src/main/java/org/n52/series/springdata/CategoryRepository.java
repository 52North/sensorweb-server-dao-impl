package org.n52.series.springdata;


import org.n52.series.db.beans.CategoryEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface CategoryRepository extends ParameterDataRepository<CategoryEntity> {

}
