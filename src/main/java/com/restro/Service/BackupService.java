package com.restro.Service;

import java.io.IOException;

/**
 * Exports/imports the restaurant's *configuration* - profile, branding
 * images, categories, food items + photos, tables, taxes and discounts -
 * as a single portable JSON file, so an owner can move to a new server or
 * recover after wiping a table by mistake.
 *
 * Deliberately does NOT include order/sales history or the owner login: a
 * restore already deletes and rebuilds every table/category/food item with
 * fresh database IDs, so old order rows (which point at those IDs) would
 * either break or silently point at the wrong items. For full data
 * protection including order history, use your hosting provider's regular
 * database backups instead - this is a "get my restaurant running again
 * quickly" tool, not a substitute for real backups.
 */
public interface BackupService {

    byte[] exportBackup();

    void restoreBackup(byte[] backupJson) throws IOException;
}
