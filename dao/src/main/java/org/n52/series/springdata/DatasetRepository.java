
package org.n52.series.springdata;


import org.n52.series.db.beans.DatasetEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface DatasetRepository<T extends DatasetEntity> extends ParameterDataRepository<T> {

    @Modifying(clearAutomatically = true)
    @Query("Update DatasetEntity d set d.valueType = :valueType where d.id = :id")
    void qualifyDataset(@Param("valueType") String valueType, @Param("id") Long id);

}
