
package org.n52.series.springdata;


import org.n52.series.db.beans.DatasetEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface DatasetRepository<T extends DatasetEntity> extends ParameterDataRepository<T> {

}
