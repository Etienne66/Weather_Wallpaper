package com.luckycatlabs.sunrisesunset.dto;

import java.math.BigDecimal;

public class Location {
    private BigDecimal latitude;
    private BigDecimal longitude;

    public Location(String latitude2, String longitude2) {
        this.latitude = new BigDecimal(latitude2);
        this.longitude = new BigDecimal(longitude2);
    }

    public Location(double latitude2, double longitude2) {
        this.latitude = new BigDecimal(latitude2);
        this.longitude = new BigDecimal(longitude2);
    }

    public BigDecimal getLatitude() {
        return this.latitude;
    }

    public BigDecimal getLongitude() {
        return this.longitude;
    }
}
