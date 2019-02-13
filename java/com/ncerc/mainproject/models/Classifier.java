package com.ncerc.mainproject.models;

public interface Classifier
{
    String name();
    Classification recognize(final float[] pixels);
}
