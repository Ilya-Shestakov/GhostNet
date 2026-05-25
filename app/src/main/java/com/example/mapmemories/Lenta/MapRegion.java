package com.example.mapmemories.Lenta;

import org.osmdroid.util.BoundingBox;

public class MapRegion {
    String id;
    String name;
    BoundingBox bbox;
    boolean isDownloaded;

    public MapRegion(String id, String name, BoundingBox bbox, boolean isDownloaded) {
        this.id = id;
        this.name = name;
        this.bbox = bbox;
        this.isDownloaded = isDownloaded;
    }
}