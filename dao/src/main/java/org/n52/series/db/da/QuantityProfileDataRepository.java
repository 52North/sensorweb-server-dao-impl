
package org.n52.series.db.da;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.profile.ProfileData;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.ProfileDatasetEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.dao.DbQuery;

public class QuantityProfileDataRepository
        extends AbstractDataRepository<ProfileData, ProfileDatasetEntity, ProfileDataEntity, ProfileValue> {

    private final QuantityDataRepository quantityRepository;

    public QuantityProfileDataRepository() {
        this.quantityRepository = new QuantityDataRepository();
    }
    
    @Override
    public ProfileValue getFirstValue(ProfileDatasetEntity dataset, Session session, DbQuery query) {
        query.setComplexParent(true);
        ProfileDataEntity parentEntity = super.getFirstValueEntity(dataset, query, session);
        return assembleChildrenFrom(parentEntity, dataset, query);
    }

    @Override
    public ProfileValue getLastValue(ProfileDatasetEntity dataset, Session session, DbQuery query) {
        query.setComplexParent(true);
        ProfileDataEntity parentEntity = super.getLastValueEntity(dataset, query, session);
        return assembleChildrenFrom(parentEntity, dataset, query);
    }

    @Override
    public Class<ProfileDatasetEntity> getDatasetEntityType() {
        return ProfileDatasetEntity.class;
    }
    
    private ProfileValue assembleChildrenFrom(ProfileDataEntity parentValue, ProfileDatasetEntity dataset, DbQuery query) {

        Date timeend = parentValue.getTimeend();
        Date timestart = parentValue.getTimestart();
        long end = timeend.getTime();
        long start = timestart.getTime();
        IoParameters parameters = query.getParameters();
        ProfileValue profile = parameters.isShowTimeIntervals()
                ? new ProfileValue(start, end, null)
                : new ProfileValue(end, null);
        
        List<QuantityProfileDataItem> dataItems = new ArrayList<>();
        for (DataEntity< ? > dataEntity : parentValue.getValue()) {
            QuantityDataEntity quantityEntity = (QuantityDataEntity) dataEntity;
            QuantityValue valueItem = quantityRepository.createValue(quantityEntity.getValue(), quantityEntity, query);
            addParameters(quantityEntity, valueItem, query);
            for (Map<String, Object> parameterObject : valueItem.getParameters()) {
                String verticalName = dataset.getVerticalParameterName();
                if (isVertical(parameterObject, verticalName)) {
                    // TODO vertical unit is missing for now
                    QuantityProfileDataItem dataItem = new QuantityProfileDataItem();
                    dataItem.setValue(quantityEntity.getValue());
                    // set vertical's value
                    dataItem.setVertical((double) parameterObject.get("value"));
                    String verticalUnit = (String) parameterObject.get("unit");
                    if (profile.getVerticalUnit() == null) {
                        profile.setVerticalUnit(verticalUnit);
                    }
                    if (profile.getVerticalUnit() == null
                            || !profile.getVerticalUnit().equals(verticalUnit)) {
                        dataItem.setVerticalUnit(verticalUnit);
                    }
                    dataItems.add(dataItem);
                }
            }
        }
        
        profile.setValue(dataItems);
        return profile;
    }
    
    private boolean isVertical(Map<String, Object> parameterObject, String verticalName) {
        return parameterObject.containsKey("name") 
                && ((String)parameterObject.get("name")).equalsIgnoreCase(verticalName);
    }

    public static class QuantityProfileDataItem {
        private String verticalUnit;
        private Double vertical;
        private Double value;
        public String getVerticalUnit() {
            return verticalUnit;
        }
        public void setVerticalUnit(String verticalUnit) {
            this.verticalUnit = verticalUnit;
        }
        public Double getVertical() {
            return vertical;
        }
        public void setVertical(Double vertical) {
            this.vertical = vertical;
        }
        public Double getValue() {
            return value;
        }
        public void setValue(Double value) {
            this.value = value;
        }
        
    }

    @Override
    protected ProfileValue createSeriesValueFor(ProfileDataEntity valueEntity,
                                                ProfileDatasetEntity datasetEntity,
                                                DbQuery query) {
        return null;
    }

    @Override
    protected ProfileData assembleData(ProfileDatasetEntity datasetEntity, DbQuery query, Session session)
            throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected ProfileData assembleDataWithReferenceValues(ProfileDatasetEntity datasetEntity,
                                                          DbQuery dbQuery,
                                                          Session session)
            throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }
}
