
package org.n52.series.db;

import org.junit.jupiter.api.BeforeEach;
import org.n52.series.db.beans.NotInitializedDatasetEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.beans.QuantityProfileDatasetEntity;
import org.n52.series.db.beans.TextDatasetEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DefaultDbQueryFactory;
import org.n52.series.db.query.DatasetQuerySpecifications;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class TestBase {

    @Autowired
    protected TestRepositories testRepositories;

    protected DatasetQuerySpecifications defaultFilterSpec;

    protected DbQuery defaultQuery;

    @BeforeEach
    public void setUp() {
        this.defaultQuery = new DefaultDbQueryFactory().createDefault();
        this.defaultFilterSpec = DatasetQuerySpecifications.of(defaultQuery);
    }

    protected NotInitializedDatasetEntity uninitializedDataset(final String phenomenonIdentifier,
                                                        final String offeringIdentifier,
                                                        final String procedureIdentifier,
                                                        final String procedureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     procedureIdentifier,
                                                     procedureFormat,
                                                     new NotInitializedDatasetEntity());
    }

    protected NotInitializedDatasetEntity uninitializedDataset(final String phenomenonIdentifier,
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

    protected QuantityDatasetEntity quantityDataset(final String phenomenonIdentifier,
                                                    final String offeringIdentifier,
                                                    final String procedureIdentifier,
                                                    final String procedureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     procedureIdentifier,
                                                     procedureFormat,
                                                     new QuantityDatasetEntity());
    }

    protected QuantityDatasetEntity quantityDataset(final String phenomenonIdentifier,
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

    protected QuantityProfileDatasetEntity quantityProfileDataset(final String phenomenonIdentifier,
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
                                                     new QuantityProfileDatasetEntity());
    }

    protected TextDatasetEntity textDataset(final String phenomenonIdentifier,
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
