package com.udacity.catpoint.service;

import java.awt.image.BufferedImage;

public interface ImageService {
    boolean imageContainsCat(BufferedImage image, float confidenceThreshold);
}
