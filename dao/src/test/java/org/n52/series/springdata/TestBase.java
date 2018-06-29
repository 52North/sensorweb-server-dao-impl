package org.n52.series.springdata;

import org.junit.jupiter.api.BeforeEach;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.NotInitializedDatasetEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.beans.TextDatasetEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DefaultDbQueryFactory;
import org.n52.series.springdata.query.DatasetQuerySpecifications;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class TestBase {

    @Autowired
    protected TestRepositories<DatasetEntity> testRepositories;

    protected DatasetQuerySpecifications defaultFilterSpec;

    protected DbQuery defaultQuery;

    @BeforeEach
    public void setUp() {
        this.defaultQuery = new DefaultDbQueryFactory().createDefault();
        this.defaultFilterSpec = DatasetQuerySpecifications.of(defaultQuery);
    }

    protected DatasetEntity uninitializedDataset(final String phenomenonIdentifier,
                                               final String offeringIdentifier,
                                               final String procedureIdentifier,
                                               final String procedureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     procedureIdentifier,
                                                     procedureFormat,
                                                     new NotInitializedDatasetEntity());
    }

    protected DatasetEntity uninitializedDataset(final String phenomenonIdentifier,
                                               final String offeringIdentifier,
                                               final String procedureIdentifier,
                                               final String procedureFormat,
                                               final String featureIdentifier,
                                               final String featureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     procedureIdentifier,
                                                     procedureFormat,
                                                     featureIdentifier,
                                                     featureFormat,
                                                     new NotInitializedDatasetEntity());
    }

    protected DatasetEntity quantityDataset(final String phenomenonIdentifier,
                                          final String offeringIdentifier,
                                          final String procedureIdentifier,
                                          final String procedureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     procedureIdentifier,
                                                     procedureFormat,
                                                     new QuantityDatasetEntity());
    }

    protected DatasetEntity quantityDataset(final String phenomenonIdentifier,
                                          final String offeringIdentifier,
                                          final String procedureIdentifier,
                                          final String procedureFormat,
                                          final String featureIdentifier,
                                          final String featureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     procedureIdentifier,
                                                     procedureFormat,
                                                     featureIdentifier,
                                                     featureFormat,
                                                     new QuantityDatasetEntity());
    }

    protected DatasetEntity textDataset(final String phenomenonIdentifier,
                                      final String offeringIdentifier,
                                      final String procedureIdentifier,
                                      final String procedureFormat,
                                      final String featureIdentifier,
                                      final String featureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     procedureIdentifier,
                                                     procedureFormat,
                                                     featureIdentifier,
                                                     featureFormat,
                                                     new TextDatasetEntity());
    }

}
