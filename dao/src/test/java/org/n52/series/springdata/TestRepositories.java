
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
        dataset.setFeature(upsertSimpleFeature(featureIdentifier, featureFormat));
        return save(dataset);
    }

    private T buildNewDataset(String procedureFormat,
                              String procedureIdentifier,
                              String phenomenonIdentifier,
                              String offeringIdentifier,
                              T emptyDatasetEntity) {
        return newDataset().setOffering(upsertSimpleOffering(offeringIdentifier))
                           .setPhenomemon(upsertSimplePhenomenon(phenomenonIdentifier))
                           .setProcedure(upsertSimpleProcedure(procedureIdentifier, procedureFormat))
                           .build(emptyDatasetEntity);
    }

    public ProcedureEntity upsertSimpleProcedure(String procedureIdentifier, String format) {
        return procedureRepository.findByIdentifier(procedureIdentifier)
                                  .orElseGet(() -> persistSimpleProcedure(procedureIdentifier, format));
    }

    public ProcedureEntity persistSimpleProcedure(String procedureIdentifier, String format) {
        FormatEntity formatEntity = upsertFormat(format);
        ProcedureBuilder builder = newProcedure(procedureIdentifier);
        ProcedureEntity entity = builder.setFormat(formatEntity)
                                        .build();
        return save(entity);
    }

    public PhenomenonEntity upsertSimplePhenomenon(String phenomenonIdentifier) {
        return phenomenonRepository.findByIdentifier(phenomenonIdentifier)
                                   .orElseGet(() -> persistSimplePhenomenon(phenomenonIdentifier));
    }

    public PhenomenonEntity persistSimplePhenomenon(String phenomenonIdentifier) {
        return save(newPhenomenon(phenomenonIdentifier).build());
    }

    public FeatureEntity upsertSimpleFeature(String featureIdentifier, String format) {
        return featureRepository.findByIdentifier(featureIdentifier)
                                .orElseGet(() -> persistSimpleFeature(featureIdentifier, format));
    }

    public FeatureEntity persistSimpleFeature(String featureIdentifier, String format) {
        FormatEntity formatEntity = upsertFormat(format);
        FeatureBuilder builder = newFeature(featureIdentifier);
        FeatureEntity entity = builder.setFormat(formatEntity)
                                      .build();
        return save(entity);
    }

    private FormatEntity upsertFormat(String format) {
        return formatRepository.existsByFormat(format)
            ? formatRepository.findByFormat(format)
            : save(newFormat(format).build());
    }

    public OfferingEntity upsertSimpleOffering(String offeringIdentifier) {
        return offeringRepository.findByIdentifier(offeringIdentifier)
                                 .orElseGet(() -> persistSimpleOffering(offeringIdentifier));
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
