
package org.n52.series.springdata;

import static org.n52.series.test.DatasetEntityBuilder.newDataset;
import static org.n52.series.test.FeatureBuilder.newFeature;
import static org.n52.series.test.FormatBuilder.newFormat;
import static org.n52.series.test.OfferingBuilder.newOffering;
import static org.n52.series.test.PhenomenonBuilder.newPhenomenon;
import static org.n52.series.test.ProcedureBuilder.newProcedure;

import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.FormatEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.test.FeatureBuilder;
import org.n52.series.test.ProcedureBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestRepositories<T extends DatasetEntity> {

    @Autowired
    private PhenomenonRepository phenomenonRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private OfferingRepository offeringRepository;

    @Autowired
    private ProcedureRepository procedureRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private DatasetRepository<T> datasetRepository;

    public T persistSimpleDataset(String phenomenonIdentifier,
                                  String offeringIdentifier,
                                  String procedureIdentifier,
                                  String procedureFormat,
                                  T emptyDatasetEntity) {
        T dataset = buildNewDataset(procedureFormat,
                                    procedureIdentifier,
                                    phenomenonIdentifier,
                                    offeringIdentifier,
                                    emptyDatasetEntity);
        return save(dataset);
    }

    public T persistSimpleDataset(String phenomenonIdentifier,
                                  String offeringIdentifier,
                                  String procedureIdentifier,
                                  String procedureFormat,
                                  String featureIdentifier,
                                  String featureFormat,
                                  T emptyDatasetEntity) {
        T dataset = buildNewDataset(procedureFormat,
                                    procedureIdentifier,
                                    phenomenonIdentifier,
                                    offeringIdentifier,
                                    emptyDatasetEntity);
        dataset.setFeature(persistSimpleFeature(featureIdentifier, featureFormat));
        return save(dataset);
    }

    private T buildNewDataset(String procedureFormat,
                              String procedureIdentifier,
                              String phenomenonIdentifier,
                              String offeringIdentifier,
                              T emptyDatasetEntity) {
        return newDataset().setOffering(persistSimpleOffering(offeringIdentifier))
                           .setPhenomemon(persistSimplePhenomenon(phenomenonIdentifier))
                           .setProcedure(persistSimpleProcedure(procedureIdentifier, procedureFormat))
                           .build(emptyDatasetEntity);
    }

    public ProcedureEntity persistSimpleProcedure(String procedureIdentifier, String format) {
        FormatEntity formatEntity = save(newFormat(format).build());
        ProcedureBuilder builder = newProcedure(procedureIdentifier);
        ProcedureEntity entity = builder.setFormat(formatEntity)
                                        .build();
        return save(entity);
    }

    public PhenomenonEntity persistSimplePhenomenon(String phenomenonIdentifier) {
        return save(newPhenomenon(phenomenonIdentifier).build());
    }

    public FeatureEntity persistSimpleFeature(String featureIdentifier, String format) {
        FormatEntity formatEntity = save(newFormat(format).build());
        FeatureBuilder builder = newFeature(featureIdentifier);
        FeatureEntity entity = builder.setFormat(formatEntity)
                                      .build();
        return save(entity);
    }

    public OfferingEntity persistSimpleOffering(String offeringIdentifier) {
        return save(newOffering(offeringIdentifier).build());
    }

    public PhenomenonEntity save(PhenomenonEntity entity) {
        return phenomenonRepository.save(entity);
    }

    public FeatureEntity save(FeatureEntity entity) {
        return featureRepository.save(entity);
    }

    public OfferingEntity save(OfferingEntity entity) {
        return offeringRepository.save(entity);
    }

    public ProcedureEntity save(ProcedureEntity entity) {
        return procedureRepository.save(entity);
    }

    public FormatEntity save(FormatEntity entity) {
        return formatRepository.save(entity);
    }

    public T save(T entity) {
        return datasetRepository.save(entity);
    }

}
