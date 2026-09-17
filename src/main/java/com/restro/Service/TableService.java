package com.restro.Service;

import com.restro.entity.QrCode;
import com.restro.entity.Restaurant;
import com.restro.entity.RestaurantTable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/** Table & QR management - same "?table=<id>&token=<qrToken>" style link the original app used, now as /t/{id}/{token}. */
public interface TableService {

    List<RestaurantTable> allTables(Integer restaurantId);

    Optional<RestaurantTable> validate(Integer tableId, String token);

    RestaurantTable createTable(Restaurant restaurant, String tableNo, int capacity);

    void deleteTable(Integer tableId);

    void toggleActive(Integer tableId);

    String targetUrl(RestaurantTable table);

    /** Regenerates and stores a fresh QR image for a table; keeps history (old rows untouched). */
    QrCode generateQrCode(RestaurantTable table) throws IOException;

    Optional<QrCode> latestQrCode(Integer tableId);
}
