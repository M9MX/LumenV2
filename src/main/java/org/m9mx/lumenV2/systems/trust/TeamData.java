package org.m9mx.lumenV2.systems.trust;

/**
 * Represents a single team
 */
public class TeamData {
    private String id;
    private String displayName;
    private String creatorUuid;
    private long createdAt;
    
    public TeamData(String id, String displayName, String creatorUuid, long createdAt) {
        this.id = id;
        this.displayName = displayName;
        this.creatorUuid = creatorUuid;
        this.createdAt = createdAt;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getCreatorUuid() {
        return creatorUuid;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
}
