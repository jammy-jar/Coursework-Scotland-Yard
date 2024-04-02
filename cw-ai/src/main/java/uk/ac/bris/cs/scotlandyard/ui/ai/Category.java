package uk.ac.bris.cs.scotlandyard.ui.ai;

public class Category {
    int categoryNum;

    public Category(int categoryNum) {
        if (categoryNum > 5 || categoryNum < 1) throw new IllegalArgumentException("There are 5 (positive) categories, category " + categoryNum + " is not valid.");
        this.categoryNum = categoryNum;
    }

    public double getWeight() {
        return ScotLandYardAi.MIN_DIST_CAT_WEIGHTS.get(categoryNum - 1);
    }
}
