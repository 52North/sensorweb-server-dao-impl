package org.n52.series.springdata;


import org.n52.series.db.beans.FormatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface FormatRepository extends JpaRepository<FormatEntity, Long> {

}
