package com.ncerc.mainproject;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

public class ResultHolder
{
    private static Bitmap image;

    public static void setImage(@Nullable Bitmap image)
    {
        ResultHolder.image = image;
    }

    @Nullable
    public static Bitmap getImage()
    {
        return image;
    }

    public static void dispose()
    {
        setImage(null);
    }

}
