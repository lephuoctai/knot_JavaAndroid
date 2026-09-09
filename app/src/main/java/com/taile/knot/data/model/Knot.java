package com.taile.knot.data.model;

public class Knot {
    private String id;
    private String description;
    private String imageUrl;
    private int imageResId; // Dùng để nạp drawable test hoặc local
    private long memoryDate;

    // Bắt buộc constructor rỗng cho Firebase Firestore deserialization sau này
    public Knot() {
    }

    public Knot(String id, String description, int imageResId, long memoryDate) {
        this.id = id;
        this.description = description;
        this.imageResId = imageResId;
        this.memoryDate = memoryDate;
    }

    public Knot(String id, String description, String imageUrl, long memoryDate) {
        this.id = id;
        this.description = description;
        this.imageUrl = imageUrl;
        this.memoryDate = memoryDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }

    public long getMemoryDate() { return memoryDate; }
    public void setMemoryDate(long memoryDate) { this.memoryDate = memoryDate; }
}