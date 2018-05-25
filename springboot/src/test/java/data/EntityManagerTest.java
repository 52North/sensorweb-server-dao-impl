
package data;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.springboot.init.Application;
import org.n52.springboot.init.DatabaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@DataJpaTest
@RunWith(SpringRunner.class)
public class EntityManagerTest {

    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    public void foo() {
        OfferingEntity offeringEntity = new OfferingEntity();
        OfferingEntity savedEntity = entityManager.persist(offeringEntity);
        assertThat(savedEntity.getId(), IsNull.notNullValue());
    }
    
    @SpringBootConfiguration
    @Import(value = {Application.class, DatabaseConfig.class})
    static class Config {
    }
}
