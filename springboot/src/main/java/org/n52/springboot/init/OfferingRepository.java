package org.n52.springboot.init;

import org.n52.series.db.beans.OfferingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferingRepository extends JpaRepository<OfferingEntity, Long> {

}
