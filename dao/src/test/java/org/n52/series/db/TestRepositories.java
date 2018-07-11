
package org.n52.series.db;

import static org.n52.series.test.DatasetEntityBuilder.newDataset;
import static org.n52.series.test.FeatureBuilder.newFeature;
import static org.n52.series.test.FormatBuilder.newFormat;
import static org.n52.series.test.OfferingBuilder.newOffering;
import static org.n52.series.test.PhenomenonBuilder.newPhenomenon;
import static org.n52.series.test.ProcedureBuilder.newProcedure;

import org.n52.series.db.CategoryRepository;
import org.n52.series.db.DatasetRepository;
import org.n52.series.db.FeatureRepository;
import org.n52.series.db.FormatRepository;
import org.n52.series.db.OfferingRepository;
import org.n52.series.db.PhenomenonRepository;
import org.n52.series.db.ProcedureRepository;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.FormatEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.test.CategoryBuilder;
import org.n52.series.test.FeatureBuilder;
import org.n52.series.test.ProcedureBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestRepositories {

    @Autowired
    private PhenomenonRepository phenomenonRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private OfferingRepository offeringRepository;

    @Autowired
    private ProcedureRepository procedureRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private DatasetRepository<? super DatasetEntity> datasetRepository;

    public <T extends DatasetEntity> T persistSimpleDataset(final String phenomenonIdentifier,
                                  final String offeringIdentifier,
                                  final String procedureIdentifier,
                                  final String procedureFormat,
                                  final T emptyDatasetEntity) {
        final T dataset = buildNewDataset(procedureFormat,
                                    procedureIdentifier,
                                    phenomenonIdentifier,
                                    offeringIdentifier,
                                    emptyDatasetEntity);
        return save(dataset);
    }

    public <T extends DatasetEntity> T persistSimpleDataset(final String phenomenonIdentifier,
                                  final String offeringIdentifier,
                                  final String procedureIdentifier,
                                  final String procedureFormat,
                                  final String featureIdentifier,
                                  final String featureFormat,
                                  final T emptyDatasetEntity) {
        final T dataset = buildNewDataset(procedureFormat,
                                    procedureIdentifier,
                                    phenomenonIdentifier,
                                    offeringIdentifier,
                                    emptyDatasetEntity);
        dataset.setFeature(upsertSimpleFeature(featureIdentifier, featureFormat));
        return save(dataset);
    }

    private <T extends DatasetEntity> T buildNewDataset(final String procedureFormat,
                              final String procedureIdentifier,
                              final String phenomenonIdentifier,
                              final String offeringIdentifier,
                              final T emptyDatasetEntity) {
        return newDataset().setOffering(upsertSimpleOffering(offeringIdentifier))
                           .setPhenomemon(upsertSimplePhenomenon(phenomenonIdentifier))
                           .setProcedure(upsertSimpleProcedure(procedureIdentifier, procedureFormat))
                           .setCategory(upsertSimpleCategory(phenomenonIdentifier))
                           .build(emptyDatasetEntity);
    }

    public ProcedureEntity upsertSimpleProcedure(final String procedureIdentifier, final String format) {
        return procedureRepository.findByIdentifier(procedureIdentifier)
                                  .orElseGet(() -> persistSimpleProcedure(procedureIdentifier, format));
    }

    public ProcedureEntity persistSimpleProcedure(final String procedureIdentifier, final String format) {
        final FormatEntity formatEntity = upsertFormat(format);
        final ProcedureBuilder builder = newProcedure(procedureIdentifier);
        final ProcedureEntity entity = builder.setFormat(formatEntity)
                                        .build();
        return save(entity);
    }

    public PhenomenonEntity upsertSimplePhenomenon(final String phenomenonIdentifier) {
        return phenomenonRepository.findByIdentifier(phenomenonIdentifier)
                                   .orElseGet(() -> persistSimplePhenomenon(phenomenonIdentifier));
    }

    public PhenomenonEntity persistSimplePhenomenon(final String phenomenonIdentifier) {
        return save(newPhenomenon(phenomenonIdentifier).build());
    }

    public CategoryEntity upsertSimpleCategory(final String categoryIdentifier) {
        return categoryRepository.findByIdentifier(categoryIdentifier)
                                   .orElseGet(() -> persistSimpleCategory(categoryIdentifier));
    }

    public CategoryEntity persistSimpleCategory(final String categoryIdentifier) {
        return save(CategoryBuilder.newCategory(categoryIdentifier).build());
    }

    public FeatureEntity upsertSimpleFeature(final String featureIdentifier, final String format) {
        return featureRepository.findByIdentifier(featureIdentifier)
                                .orElseGet(() -> persistSimpleFeature(featureIdentifier, format));
    }

    public FeatureEntity persistSimpleFeature(final String featureIdentifier, final String format) {
        final FormatEntity formatEntity = upsertFormat(format);
        final FeatureBuilder builder = newFeature(featureIdentifier);
        final FeatureEntity entity = builder.setFormat(formatEntity)
                                      .build();
        return save(entity);
    }

    private FormatEntity upsertFormat(final String format) {
        return formatRepository.existsByFormat(format)
            ? formatRepository.findByFormat(format)
            : save(newFormat(format).build());
    }

    public OfferingEntity upsertSimpleOffering(final String offeringIdentifier) {
        return offeringRepository.findByIdentifier(offeringIdentifier)
                                 .orElseGet(() -> persistSimpleOffering(offeringIdentifier));
    }

    public OfferingEntity persistSimpleOffering(final String offeringIdentifier) {
        return save(newOffering(offeringIdentifier).build());
    }

    public PhenomenonEntity save(final PhenomenonEntity entity) {
        return phenomenonRepository.save(entity);
    }

    public FeatureEntity save(final FeatureEntity entity) {
        return featureRepository.save(entity);
    }

    public OfferingEntity save(final OfferingEntity entity) {
        return offeringRepository.save(entity);
    }

    public ProcedureEntity save(final ProcedureEntity entity) {
        return procedureRepository.save(entity);
    }

    public CategoryEntity save(final CategoryEntity entity) {
        return categoryRepository.save(entity);
    }

    public FormatEntity save(final FormatEntity entity) {
        return formatRepository.save(entity);
    }

    public <T extends DatasetEntity> T save(final T entity) {
        return datasetRepository.save(entity);
    }

}
