
package org.n52.series.springdata;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class OfferingRepositoryTest {

    @Autowired
    private OfferingRepository repository;

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
    @EnableJpaRepositories(basePackageClasses = OfferingRepository.class)
    static class Config extends TestRepositoryConfig<DatasetEntity> {
        public Config() {
            super("/mapping/core/persistence.xml");
        }

        @Override
        public TestRepositories<DatasetEntity> testRepositories() {
            return new TestRepositories<>();
        }
    }
}
