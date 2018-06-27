
package org.n52.series.test;

import org.n52.series.db.beans.CategoryEntity;

public class CategoryBuilder extends DescribableEntityBuilder<CategoryEntity> {

    private CategoryBuilder(String identifier) {
        super(identifier);
    }

    public static CategoryBuilder newCategory(String identifier) {
        return new CategoryBuilder(identifier);
    }

    @Override
    public CategoryEntity build() {
        CategoryEntity entity = prepare(new CategoryEntity());
        return entity;
    }

}
