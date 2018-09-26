package com.mash.api.entity;

public class BidDiffTime {

    private long diffTime;

    /**
     * 0 未开始
     * 1 已开始
     */
    private int type;

    public long getDiffTime() {
        return diffTime;
    }

    public void setDiffTime(long diffTime) {
        this.diffTime = diffTime;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
