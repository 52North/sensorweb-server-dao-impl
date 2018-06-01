package org.n52.series.springdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ParameterDataRepository<T> extends JpaRepository<T, Long> {

    boolean existsByIdentifier(String identifier);
    
}
