/*
 *   Nextcloud Android client application
 *
 *   @author Tobias Kaminsky
 *   Copyright (C) 2016 Tobias Kaminsky
 *   Copyright (C) 2016 Nextcloud
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.datamodel;

import com.nextcloud.client.preferences.SubFolderRule;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.File;
import java.io.Serializable;

/**
 * Synced folder entity containing all information per synced folder.
 */
public class SyncedFolder implements Serializable, Cloneable {
    public static final long UNPERSISTED_ID = Long.MIN_VALUE;
    public static final long EMPTY_ENABLED_TIMESTAMP_MS = -1;
    public static final long NOT_SCANNED_YET = -1;
    private static final long serialVersionUID = -793476118299906429L;



    private long id;
    private String localPath;
    private String remotePath;
    private boolean wifiOnly;
    private boolean chargingOnly;
    private boolean existing;
    private boolean subfolderByDate;
    private String account;
    private int uploadAction;
    private int nameCollisionPolicy;
    private boolean enabled;
    private long enabledTimestampMs;
    private MediaFolderType type;
    private boolean hidden;
    private SubFolderRule subfolderRule;
    private boolean excludeHidden;
    private long lastScanTimestampMs;

    /**
     * constructor for new, to be persisted entity.
     *
     * @param localPath           local path
     * @param remotePath          remote path
     * @param wifiOnly            upload on wifi only flag
     * @param chargingOnly        upload on charging only
     * @param existing            upload existing files
     * @param subfolderByDate     create sub-folders by date (month)
     * @param account             the account owning the synced folder
     * @param uploadAction        the action to be done after the upload
     * @param nameCollisionPolicy the behaviour to follow if detecting a collision
     * @param enabled             flag if synced folder config is active
     * @param timestampMs         the current timestamp in milliseconds
     * @param type                the type of the folder
     * @param hidden              hide item flag
     * @param subFolderRule   whether to filter subFolder by year/month/day
     * @param excludeHidden   exclude hidden file or folder, for {@link MediaFolderType#CUSTOM} only
     */
    public SyncedFolder(String localPath,
                        String remotePath,
                        boolean wifiOnly,
                        boolean chargingOnly,
                        boolean existing,
                        boolean subfolderByDate,
                        String account,
                        int uploadAction,
                        int nameCollisionPolicy,
                        boolean enabled,
                        long timestampMs,
                        MediaFolderType type,
                        boolean hidden,
                        SubFolderRule subFolderRule,
                        boolean excludeHidden,
                        long lastScanTimestampMs) {
        this(UNPERSISTED_ID,
             localPath,
             remotePath,
             wifiOnly,
             chargingOnly,
             existing,
             subfolderByDate,
             account,
             uploadAction,
             nameCollisionPolicy,
             enabled,
             timestampMs,
             type,
             hidden,
             subFolderRule,
             excludeHidden,
             lastScanTimestampMs);
    }

    /**
     * constructor for wrapping existing folders.
     *
     * @param id id
     */
    protected SyncedFolder(long id,
                           String localPath,
                           String remotePath,
                           boolean wifiOnly,
                           boolean chargingOnly,
                           boolean existing,
                           boolean subfolderByDate,
                           String account,
                           int uploadAction,
                           int nameCollisionPolicy,
                           boolean enabled,
                           long timestampMs,
                           MediaFolderType type,
                           boolean hidden,
                           SubFolderRule subFolderRule,
                           boolean excludeHidden,
                           long lastScanTimestampMs) {
        this.id = id;
        this.localPath = localPath;
        this.remotePath = remotePath;
        this.wifiOnly = wifiOnly;
        this.chargingOnly = chargingOnly;
        this.existing = existing;
        this.subfolderByDate = subfolderByDate;
        this.account = account;
        this.uploadAction = uploadAction;
        this.nameCollisionPolicy = nameCollisionPolicy;
        this.setEnabled(enabled, timestampMs);
        this.type = type;
        this.hidden = hidden;
        this.subfolderRule = subFolderRule;
        this.excludeHidden = excludeHidden;
        this.lastScanTimestampMs = lastScanTimestampMs;
    }

    /**
     * @param timestampMs the current timestamp in milliseconds
     */
    public void setEnabled(boolean enabled, long timestampMs) {
        this.enabled = enabled;
        this.enabledTimestampMs = enabled ? timestampMs : EMPTY_ENABLED_TIMESTAMP_MS;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public long getId() {
        return this.id;
    }

    public String getLocalPath() {
        return this.localPath;
    }

    public String getRemotePath() {
        return this.remotePath;
    }

    public boolean isWifiOnly() {
        return this.wifiOnly;
    }

    public boolean isChargingOnly() {
        return this.chargingOnly;
    }

    public boolean isExisting() {
        return this.existing;
    }

    public boolean isSubfolderByDate() {
        return this.subfolderByDate;
    }

    public String getAccount() {
        return this.account;
    }

    public int getUploadAction() {
        return this.uploadAction;
    }

    public int getNameCollisionPolicyInt() {
        return this.nameCollisionPolicy;
    }

    public NameCollisionPolicy getNameCollisionPolicy() {
        return NameCollisionPolicy.deserialize(nameCollisionPolicy);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public long getEnabledTimestampMs() {
        return this.enabledTimestampMs;
    }

    public MediaFolderType getType() {
        return this.type;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public SubFolderRule getSubfolderRule() { return this.subfolderRule; }

    public void setId(long id) {
        this.id = id;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public void setWifiOnly(boolean wifiOnly) {
        this.wifiOnly = wifiOnly;
    }

    public void setChargingOnly(boolean chargingOnly) {
        this.chargingOnly = chargingOnly;
    }

    public void setExisting(boolean existing) {
        this.existing = existing;
    }

    public void setSubfolderByDate(boolean subfolderByDate) {
        this.subfolderByDate = subfolderByDate;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setUploadAction(int uploadAction) {
        this.uploadAction = uploadAction;
    }

    public void setNameCollisionPolicy(int nameCollisionPolicy) {
        this.nameCollisionPolicy = nameCollisionPolicy;
    }

    public void setType(MediaFolderType type) {
        this.type = type;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setSubFolderRule(SubFolderRule subFolderRule) { this.subfolderRule = subFolderRule; }

    public boolean isExcludeHidden() {
        return excludeHidden;
    }

    public void setExcludeHidden(boolean excludeHidden) {
        this.excludeHidden = excludeHidden;
    }

    public boolean containsTypedFile(String filePath){
        boolean isCorrectMediaType =
                (getType() == MediaFolderType.IMAGE && MimeTypeUtil.isImage(new File(filePath))) ||
                (getType() == MediaFolderType.VIDEO && MimeTypeUtil.isVideo(new File(filePath))) ||
                getType() == MediaFolderType.CUSTOM;
        return filePath.contains(localPath) && isCorrectMediaType;
    }

    public long getLastScanTimestampMs() { return lastScanTimestampMs; }

    public void setLastScanTimestampMs(long lastScanTimestampMs) { this.lastScanTimestampMs = lastScanTimestampMs; }
}
