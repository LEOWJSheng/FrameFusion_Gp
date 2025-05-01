package org.example.framefusion_gp;

public class ImageData {
    private final int id;
    private final String image_path;
    private final String annotation;

    public ImageData(int id, String path, String annotation) {
        this.id = id;
        this.image_path = path;
        this.annotation = annotation;
    }

    public int getId() { return id; }
    public String getPath() { return image_path; }
    public String getAnnotation() { return annotation; }
}