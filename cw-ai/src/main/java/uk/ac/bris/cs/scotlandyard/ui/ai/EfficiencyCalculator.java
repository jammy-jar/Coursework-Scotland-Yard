package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.List;

public class EfficiencyCalculator {

    public static long initPossibleMrXLocationsTime = 0;
    public static long initLocationCategoryMapTime = 0;
    public static long selectAssumedMrXLocationTime = 0;
    public static long calcMCDHeuristicTime = 0;
    public static long calcMTDORCALHeuristicTime = 0;

    public static List<Integer> getRatio() {
        long sum = initPossibleMrXLocationsTime + initLocationCategoryMapTime + selectAssumedMrXLocationTime + calcMCDHeuristicTime + calcMTDORCALHeuristicTime;
        double scalar = 100.0 / sum;
        return List.of((int) (initPossibleMrXLocationsTime * scalar), (int) (initLocationCategoryMapTime * scalar), (int) (selectAssumedMrXLocationTime * scalar), (int) (calcMCDHeuristicTime * scalar), (int) (calcMTDORCALHeuristicTime * scalar));
    }


}
