package com.restro.Impl;

import com.restro.Repo.AssistanceRequestRepository;
import com.restro.Repo.OrderRepository;
import com.restro.Repo.QrCodeRepository;
import com.restro.Repo.RestaurantTableRepository;
import com.restro.Service.FileStorageService;
import com.restro.Service.TableService;
import com.restro.entity.QrCode;
import com.restro.entity.Restaurant;
import com.restro.entity.RestaurantTable;
import com.restro.util.QrCodeGenerator;
import com.restro.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class TableServiceImpl implements TableService {

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Autowired
    private QrCodeRepository qrCodeRepository;

    @Autowired
    private AssistanceRequestRepository assistanceRequestRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private QrCodeGenerator qrCodeGenerator;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantTable> allTables(Integer restaurantId) {
        return tableRepository.findByRestaurant_RestaurantIdOrderByTableNoAsc(restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RestaurantTable> validate(Integer tableId, String token) {
        return tableRepository.findByTableIdAndQrToken(tableId, token)
                .filter(RestaurantTable::isActive);
    }

    @Override
    @Transactional
    public RestaurantTable createTable(Restaurant restaurant, String tableNo, int capacity) {
        RestaurantTable table = RestaurantTable.builder()
                .restaurant(restaurant)
                .tableNo(tableNo)
                .capacity(capacity)
                .qrToken(TokenUtil.generateHexToken(16))
                .active(true)
                .build();
        return tableRepository.save(table);
    }

    /**
     * Deletes a table and its QR-code history (cascaded at the entity level) and any open
     * assistance requests. If the table has real order history, Orders.table_id is a hard,
     * non-null foreign key by design (an order must always show which table it came from) - so
     * we deliberately refuse the delete with a clear message rather than let a raw SQL constraint
     * error bubble up, and point the owner at deactivating the table instead.
     */
    @Override
    @Transactional
    public void deleteTable(Integer tableId) {
        if (orderRepository.existsByTable_TableId(tableId)) {
            throw new IllegalStateException(
                    "This table has order history and can't be deleted. Use the Active/Inactive toggle instead to retire it.");
        }
        assistanceRequestRepository.deleteByTable_TableId(tableId);
        try {
            tableRepository.deleteById(tableId);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "This table can't be deleted because it's still referenced elsewhere. Use the Active/Inactive toggle instead.", e);
        }
    }

    @Override
    @Transactional
    public void toggleActive(Integer tableId) {
        RestaurantTable table = tableRepository.findById(tableId).orElseThrow();
        table.setActive(!table.isActive());
        tableRepository.save(table);
    }

    @Override
    public String targetUrl(RestaurantTable table) {
        return baseUrl + "/t/" + table.getTableId() + "/" + table.getQrToken();
    }

    @Override
    @Transactional
    public QrCode generateQrCode(RestaurantTable table) throws IOException {
        String url = targetUrl(table);
        byte[] png = qrCodeGenerator.generatePng(url, 400);
        String path = fileStorageService.storeBytes(png, "image/png", "qr",
                "table-" + table.getTableId() + "-" + System.currentTimeMillis() + ".png");
        QrCode qrCode = QrCode.builder()
                .table(table)
                .imagePath(path)
                .targetUrl(url)
                .build();
        return qrCodeRepository.save(qrCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QrCode> latestQrCode(Integer tableId) {
        return qrCodeRepository.findByTable_TableIdOrderByGeneratedAtDesc(tableId).stream().findFirst();
    }
}
