package com.example.bezma.util;

import java.math.BigDecimal;

/**
 * Utility class cho các phép tính Geofencing (Haversine Distance).
 * Dùng để kiểm tra khoảng cách giữa vị trí điểm danh và vị trí văn phòng.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_000; // Bán kính Trái Đất (mét)

    private GeoUtils() {
        // Utility class - không cho phép tạo instance
    }

    /**
     * Tính khoảng cách giữa 2 tọa độ GPS bằng công thức Haversine.
     *
     * @param lat1 Vĩ độ điểm 1 (User)
     * @param lon1 Kinh độ điểm 1 (User)
     * @param lat2 Vĩ độ điểm 2 (Văn phòng)
     * @param lon2 Kinh độ điểm 2 (Văn phòng)
     * @return Khoảng cách tính bằng mét (double)
     */
    public static double calculateDistance(BigDecimal lat1, BigDecimal lon1,
                                           BigDecimal lat2, BigDecimal lon2) {
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Kiểm tra xem vị trí có nằm trong bán kính cho phép không.
     *
     * @param userLat       Vĩ độ của User
     * @param userLon       Kinh độ của User
     * @param officeLat     Vĩ độ văn phòng
     * @param officeLon     Kinh độ văn phòng
     * @param allowedRadius Bán kính cho phép (mét)
     * @return true nếu nằm trong bán kính, false nếu ngoài vùng
     */
    public static boolean isWithinRadius(BigDecimal userLat, BigDecimal userLon,
                                         BigDecimal officeLat, BigDecimal officeLon,
                                         double allowedRadius) {
        double distance = calculateDistance(userLat, userLon, officeLat, officeLon);
        return distance <= allowedRadius;
    }
}
