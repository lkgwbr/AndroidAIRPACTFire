// Copyright © 2017,
// Laboratory for Atmospheric Research at Washington State University,
// All rights reserved.

package edu.wsu.lar.airpact_fire.data.interface_object;

import android.graphics.Bitmap;

import java.io.File;
import java.util.List;

/**
 * Image representation database object for UI to use.
 */
public interface ImageInterfaceObject extends InterfaceObject {

    double[] getGps();
    void setGps(double[] values);

    TargetInterfaceObject createTargetObject();
    List<TargetInterfaceObject> getTargetObjects();

    Bitmap getBitmap();
    File createImageFile();

    File createImageFile(File storageDir);

    void setImage(Bitmap value);

    void delete();

    Object getRaw();
}
