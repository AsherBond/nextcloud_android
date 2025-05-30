/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.nextcloud.client.account.User;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.nextcloud.client.preferences.SubFolderRule;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.model.ServerFileInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import javax.annotation.Nullable;

import androidx.annotation.NonNull;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;

/**
 * Database provider for handling the persistence aspects of {@link SyncedFolder}s.
 */
public class SyncedFolderProvider extends Observable {
    static private final String TAG = SyncedFolderProvider.class.getSimpleName();

    private final ContentResolver mContentResolver;
    private final AppPreferences preferences;
    private final Clock clock;

    /**
     * constructor.
     *
     * @param contentResolver the ContentResolver to work with.
     */
    public SyncedFolderProvider(ContentResolver contentResolver, AppPreferences preferences, Clock clock) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        mContentResolver = contentResolver;
        this.preferences = preferences;
        this.clock = clock;
    }

    /**
     * Stores a synced folder object in database.
     *
     * @param syncedFolder synced folder to store
     * @return synced folder id, -1 if the insert process fails.
     */
    public long storeSyncedFolder(SyncedFolder syncedFolder) {
        Log_OC.v(TAG, "Inserting " + syncedFolder.getLocalPath() + " with enabled=" + syncedFolder.isEnabled());

        ContentValues cv = createContentValuesFromSyncedFolder(syncedFolder);

        Uri result = mContentResolver.insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS, cv);

        if (result != null) {
            return Long.parseLong(result.getPathSegments().get(1));
        } else {
            Log_OC.e(TAG, "Failed to insert item " + syncedFolder.getLocalPath() + " into folder sync db.");
            return -1;
        }
    }

    public static boolean isAutoUploadFolder(SyncedFolderProvider syncedFolderProvider, ServerFileInterface file, User user) {
        return syncedFolderProvider != null && syncedFolderProvider.findByRemotePathAndAccount(file.getRemotePath(), user);
    }

    public int countEnabledSyncedFolders() {
        int count = 0;
        Cursor cursor = mContentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                null,
                ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED + " = ?",
                new String[]{"1"},
                null
        );

        if (cursor != null) {
             count = cursor.getCount();
             cursor.close();
        }

        return count;
    }

    /**
     * get all synced folder entries.
     *
     * @return all synced folder entries, empty if none have been found
     */
    public List<SyncedFolder> getSyncedFolders() {
        Cursor cursor = mContentResolver.query(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
            null,
            null,
            null,
            null
                                              );

        if (cursor != null) {
            List<SyncedFolder> list = new ArrayList<>(cursor.getCount());
            if (cursor.moveToFirst()) {
                do {
                    SyncedFolder syncedFolder = createSyncedFolderFromCursor(cursor);
                    if (syncedFolder == null) {
                        Log_OC.e(TAG, "SyncedFolder could not be created from cursor");
                    } else {
                        list.add(cursor.getPosition(), syncedFolder);
                    }
                } while (cursor.moveToNext());

            }
            cursor.close();
            return list;
        } else {
            Log_OC.e(TAG, "DB error creating read all cursor for synced folders.");
        }

        return new ArrayList<>(0);
    }

    /**
     * Update upload status of file uniquely referenced by id.
     *
     * @param id      synced folder id.
     * @param enabled new status.
     * @return the number of rows updated.
     */
    public int updateSyncedFolderEnabled(long id, Boolean enabled) {
        Log_OC.v(TAG, "Storing synced folder id" + id + " with enabled=" + enabled);

        int result = 0;
        Cursor cursor = mContentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                null,
                ProviderMeta.ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(id)},
                null
        );

        if (cursor != null && cursor.getCount() == 1) {
            while (cursor.moveToNext()) {
                // read sync folder object and update
                SyncedFolder syncedFolder = createSyncedFolderFromCursor(cursor);

                syncedFolder.setEnabled(enabled, clock.getCurrentTime());

                // update sync folder object in db
                result = updateSyncFolder(syncedFolder);

            }
        } else {
            if (cursor == null) {
                Log_OC.e(TAG, "Sync folder db cursor for ID=" + id + " in NULL.");
            } else {
                Log_OC.e(TAG, cursor.getCount() + " items for id=" + id + " available in sync folder database. " +
                        "Expected 1. Failed to update sync folder db.");
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;
    }

    public SyncedFolder findByLocalPathAndAccount(String localPath, User user) {
        SyncedFolder result = null;
        Cursor cursor = mContentResolver.query(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
            null,
            ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH + " LIKE ? AND " +
                ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT + " =? ",
            new String[]{localPath + "%", user.getAccountName()},
            null
        );

        if (cursor != null && cursor.getCount() == 1) {
            result = createSyncedFolderFromCursor(cursor);
        } else {
            if (cursor == null) {
                Log_OC.e(TAG, "Sync folder db cursor for local path=" + localPath + " in NULL.");
            } else {
                Log_OC.e(TAG, cursor.getCount() + " items for local path=" + localPath
                        + " available in sync folder db. Expected 1. Failed to update sync folder db.");
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;

    }

    @Nullable
    public SyncedFolder getSyncedFolderByID(Long syncedFolderID) {
        SyncedFolder result = null;
        Cursor cursor = mContentResolver.query(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
            null,
            ProviderMeta.ProviderTableMeta._ID + " =? ",
            new String[]{syncedFolderID.toString()},
            null
                                              );

        if (cursor != null && cursor.getCount() == 1 && cursor.moveToFirst()) {
            result = createSyncedFolderFromCursor(cursor);
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;

    }

    /**
     *  Delete all synced folders for an account
     *
     *  @param user whose synced folders should be deleted
     */
    public int deleteSyncFoldersForAccount(User user) {
        return mContentResolver.delete(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT + " = ?",
                new String[]{String.valueOf(user.getAccountName())}
        );
    }

    /**
     * Delete a synced folder from the db
     *
     * @param id for the synced folder.
     */
    private void deleteSyncFolderWithId(long id) {
        mContentResolver.delete(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
            ProviderMeta.ProviderTableMeta._ID + " = ?",
            new String[]{String.valueOf(id)}
                               );
    }


    /**
     * Try to figure out if a path exists for synced folder, and if not, go one folder back
     * Otherwise, delete the entry
     *
     * @param context the context.
     */
    public void updateAutoUploadPaths(Context context) {
        List<SyncedFolder> syncedFolders = getSyncedFolders();
        for (SyncedFolder syncedFolder : syncedFolders) {
            if (!new File(syncedFolder.getLocalPath()).exists()) {
                String localPath = syncedFolder.getLocalPath();
                if (localPath.endsWith(PATH_SEPARATOR)) {
                    localPath = localPath.substring(0, localPath.lastIndexOf('/'));
                }
                localPath = localPath.substring(0, localPath.lastIndexOf('/'));
                if (new File(localPath).exists()) {
                    syncedFolder.setLocalPath(localPath);
                    updateSyncFolder(syncedFolder);
                } else {
                    deleteSyncFolderWithId(syncedFolder.getId());
                }
            }
        }

        if (context != null) {
            AppPreferences preferences = AppPreferencesImpl.fromContext(context);
            preferences.setAutoUploadPathsUpdateEnabled(true);
        }
    }

    /**
     * delete any records of synchronized folders that are not within the given list of ids.
     *
     * @param ids          the list of ids to be excluded from deletion.
     * @return number of deleted records.
     */
    public int deleteSyncedFoldersNotInList(List<Long> ids) {
        int result = mContentResolver.delete(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                ProviderMeta.ProviderTableMeta._ID + " NOT IN (?)",
                new String[]{String.valueOf(ids)}
        );

        if(result > 0) {
            preferences.setLegacyClean(true);
        }

        return result;
    }

    /**
     * delete record of synchronized folder with the given id.
     */
    public int deleteSyncedFolder(long id) {
        return mContentResolver.delete(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                ProviderMeta.ProviderTableMeta._ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public AppPreferences getPreferences() {
        return preferences;
    }

    /**
     * update given synced folder.
     *
     * @param syncedFolder the synced folder to be updated.
     * @return the number of rows updated.
     */
    public int updateSyncFolder(SyncedFolder syncedFolder) {
        Log_OC.v(TAG, "Updating " + syncedFolder.getLocalPath() + " with enabled=" + syncedFolder.isEnabled());

        ContentValues cv = createContentValuesFromSyncedFolder(syncedFolder);

        return mContentResolver.update(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                cv,
                ProviderMeta.ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(syncedFolder.getId())}
        );
    }

    /**
     * maps a cursor into a SyncedFolder object.
     *
     * @param cursor the db cursor
     * @return the mapped SyncedFolder, null if cursor is null
     */
    private SyncedFolder createSyncedFolderFromCursor(Cursor cursor) {
        SyncedFolder syncedFolder = null;
        if (cursor != null) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(ProviderMeta.ProviderTableMeta._ID));
            String localPath = cursor.getString(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH));
            String remotePath = cursor.getString(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH));
            boolean wifiOnly = cursor.getInt(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_WIFI_ONLY)) == 1;
            boolean chargingOnly = cursor.getInt(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_CHARGING_ONLY)) == 1;
            boolean existing = cursor.getInt(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_EXISTING)) == 1;
            boolean subfolderByDate = cursor.getInt(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_BY_DATE)) == 1;
            String accountName = cursor.getString(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT));
            int uploadAction = cursor.getInt(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_UPLOAD_ACTION));
            int nameCollisionPolicy = cursor.getInt(cursor.getColumnIndexOrThrow(
                ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY));
            boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED)) == 1;
            long enabledTimestampMs = cursor.getLong(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS));
            MediaFolderType type = MediaFolderType.getById(cursor.getInt(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_TYPE)));
            boolean hidden = cursor.getInt(cursor.getColumnIndexOrThrow(
                ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_HIDDEN)) == 1;
            SubFolderRule subFolderRule = SubFolderRule.values()[cursor.getInt(
                    cursor.getColumnIndexOrThrow(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_RULE))];
            boolean excludeHidden = cursor.getInt(cursor.getColumnIndexOrThrow(
                ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_EXCLUDE_HIDDEN)) == 1;
            long lastScanTimestampMs = cursor.getLong(cursor.getColumnIndexOrThrow(
                ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LAST_SCAN_TIMESTAMP_MS));


            syncedFolder = new SyncedFolder(id,
                                            localPath,
                                            remotePath,
                                            wifiOnly,
                                            chargingOnly,
                                            existing,
                                            subfolderByDate,
                                            accountName,
                                            uploadAction,
                                            nameCollisionPolicy,
                                            enabled,
                                            enabledTimestampMs,
                                            type,
                                            hidden,
                                            subFolderRule,
                                            excludeHidden,
                                            lastScanTimestampMs);
        }
        return syncedFolder;
    }

    /**
     * create ContentValues object based on given SyncedFolder.
     *
     * @param syncedFolder the synced folder
     * @return the corresponding ContentValues object
     */
    @NonNull
    private ContentValues createContentValuesFromSyncedFolder(SyncedFolder syncedFolder) {
        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH, syncedFolder.getLocalPath());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH, syncedFolder.getRemotePath());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_WIFI_ONLY, syncedFolder.isWifiOnly());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_CHARGING_ONLY, syncedFolder.isChargingOnly());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_EXISTING, syncedFolder.isExisting());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED, syncedFolder.isEnabled());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS, syncedFolder.getEnabledTimestampMs());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_BY_DATE, syncedFolder.isSubfolderByDate());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT, syncedFolder.getAccount());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_UPLOAD_ACTION, syncedFolder.getUploadAction());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY,
               syncedFolder.getNameCollisionPolicyInt());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_TYPE, syncedFolder.getType().id);
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_HIDDEN, syncedFolder.isHidden());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_RULE, syncedFolder.getSubfolderRule().ordinal());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_EXCLUDE_HIDDEN, syncedFolder.isExcludeHidden());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LAST_SCAN_TIMESTAMP_MS, syncedFolder.getLastScanTimestampMs());
        return cv;
    }

    /**
     * method to check if sync folder for the remote path exist in table or not
     *
     * @param remotePath to be check
     * @param user       for which we are looking
     * @return <code>true</code> if exist, <code>false</code> otherwise
     */
    public boolean findByRemotePathAndAccount(String remotePath, User user) {
        boolean result = false;

        //if path ends with / then remove the last / to work the query right way
        //because the sub folders of synced folders will not have the slash at the end
        if (remotePath.endsWith("/")) {
            remotePath = remotePath.substring(0, remotePath.length() - 1);
        }

        Cursor cursor = mContentResolver.query(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
            null,
            ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH + " LIKE ? AND " +
                ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT + " =? ",
            new String[]{"%" + remotePath + "%", user.getAccountName()},
            null);

        if (cursor != null && cursor.getCount() >= 1) {
            result = true;
        } else {
            if (cursor == null) {
                Log_OC.e(TAG, "Sync folder db cursor for remote path = " + remotePath + " in NULL.");
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;

    }
}
