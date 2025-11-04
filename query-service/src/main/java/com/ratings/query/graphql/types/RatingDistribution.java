package com.ratings.query.graphql.types;

import java.util.Map;

/**
 * GraphQL type representing the distribution of ratings by star level.
 */
public class RatingDistribution {
    
    private Integer oneStar;
    private Integer twoStar;
    private Integer threeStar;
    private Integer fourStar;
    private Integer fiveStar;
    private Integer total;
    private Integer mostCommonRating;
    private Boolean hasPositiveRatings;
    private Boolean hasNegativeRatings;
    private Double diversityScore;
    
    public RatingDistribution() {
        this.oneStar = 0;
        this.twoStar = 0;
        this.threeStar = 0;
        this.fourStar = 0;
        this.fiveStar = 0;
        this.total = 0;
        this.hasPositiveRatings = false;
        this.hasNegativeRatings = false;
    }
    
    public RatingDistribution(Map<Integer, Integer> distribution) {
        this.oneStar = distribution.getOrDefault(1, 0);
        this.twoStar = distribution.getOrDefault(2, 0);
        this.threeStar = distribution.getOrDefault(3, 0);
        this.fourStar = distribution.getOrDefault(4, 0);
        this.fiveStar = distribution.getOrDefault(5, 0);
        this.total = this.oneStar + this.twoStar + this.threeStar + this.fourStar + this.fiveStar;
    }
    
    public RatingDistribution(Integer oneStar, Integer twoStar, Integer threeStar, Integer fourStar, Integer fiveStar) {
        this.oneStar = oneStar != null ? oneStar : 0;
        this.twoStar = twoStar != null ? twoStar : 0;
        this.threeStar = threeStar != null ? threeStar : 0;
        this.fourStar = fourStar != null ? fourStar : 0;
        this.fiveStar = fiveStar != null ? fiveStar : 0;
        this.total = this.oneStar + this.twoStar + this.threeStar + this.fourStar + this.fiveStar;
    }
    
    public Integer getOneStar() {
        return oneStar;
    }
    
    public void setOneStar(Integer oneStar) {
        this.oneStar = oneStar != null ? oneStar : 0;
        recalculateTotal();
    }
    
    public Integer getTwoStar() {
        return twoStar;
    }
    
    public void setTwoStar(Integer twoStar) {
        this.twoStar = twoStar != null ? twoStar : 0;
        recalculateTotal();
    }
    
    public Integer getThreeStar() {
        return threeStar;
    }
    
    public void setThreeStar(Integer threeStar) {
        this.threeStar = threeStar != null ? threeStar : 0;
        recalculateTotal();
    }
    
    public Integer getFourStar() {
        return fourStar;
    }
    
    public void setFourStar(Integer fourStar) {
        this.fourStar = fourStar != null ? fourStar : 0;
        recalculateTotal();
    }
    
    public Integer getFiveStar() {
        return fiveStar;
    }
    
    public void setFiveStar(Integer fiveStar) {
        this.fiveStar = fiveStar != null ? fiveStar : 0;
        recalculateTotal();
    }
    
    public Integer getTotal() {
        return total;
    }
    
    public Integer getMostCommonRating() {
        return mostCommonRating;
    }
    
    public void setMostCommonRating(Integer mostCommonRating) {
        this.mostCommonRating = mostCommonRating;
    }
    
    public Boolean getHasPositiveRatings() {
        return hasPositiveRatings;
    }
    
    public void setHasPositiveRatings(Boolean hasPositiveRatings) {
        this.hasPositiveRatings = hasPositiveRatings;
    }
    
    public Boolean getHasNegativeRatings() {
        return hasNegativeRatings;
    }
    
    public void setHasNegativeRatings(Boolean hasNegativeRatings) {
        this.hasNegativeRatings = hasNegativeRatings;
    }
    
    public Double getDiversityScore() {
        return diversityScore;
    }
    
    public void setDiversityScore(Double diversityScore) {
        this.diversityScore = diversityScore;
    }
    
    private void recalculateTotal() {
        this.total = this.oneStar + this.twoStar + this.threeStar + this.fourStar + this.fiveStar;
    }
}