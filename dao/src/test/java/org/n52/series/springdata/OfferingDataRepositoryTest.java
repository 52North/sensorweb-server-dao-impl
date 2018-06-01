
package org.n52.series.springdata;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.series.db.beans.OfferingEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class OfferingDataRepositoryTest {

    @Autowired
    private OfferingDataRepository repository;

    @Test
    public void given_emptyDatabase_when_findAllQuery_then_emptyCollection() {
        List<OfferingEntity> allEntities = repository.findAll();
        Assertions.assertIterableEquals(allEntities, Collections.emptyList());
    }
    
    @Test
    public void given_emptyDatabase_when_saveOfferingWithRequiredMembersSet_then_entityGetsSavedProperly() {
        OfferingEntity entity = new OfferingEntity();
        entity.setIdentifier("foo");
        
        OfferingEntity savedEntity = repository.save(entity);
        assertAll("saving entity",
                  () -> assertNotNull(savedEntity, "saving entity returned null"),
                  () -> assertNotNull(savedEntity.getId(), "not id generated"),
                  () -> assertEquals("foo", savedEntity.getIdentifier()));
    }
    
    @Test
    public void given_persistentOffering_when_existsByIdentifier_then_entityGetsFound() {
        OfferingEntity entity = new OfferingEntity();
        entity.setIdentifier("foo");

        repository.save(entity);
        assertAll("exists by identifier",
                  () -> assertTrue(repository.existsByIdentifier("foo"), "entity is not found by identifier"));
    }

    @SpringBootConfiguration
    @EnableJpaRepositories(basePackageClasses = OfferingDataRepository.class)
    static class Config {
        @Bean
        public EntityManagerFactory entityManagerFactory(DataSource datasource, JpaProperties properties)
                throws IOException {
            LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
            emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            emf.setPersistenceXmlLocation("classpath*:META-INF/persistence.xml");
            emf.setJpaPropertyMap(properties.getProperties());
            emf.setDataSource(datasource);

            emf.afterPropertiesSet();
            return emf.getNativeEntityManagerFactory();
        }
    }
}
