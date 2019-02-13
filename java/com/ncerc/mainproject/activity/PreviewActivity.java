package com.ncerc.mainproject.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ncerc.mainproject.R;
import com.ncerc.mainproject.ResultHolder;
import com.ncerc.mainproject.models.Classification;
import com.ncerc.mainproject.models.Classifier;
import com.ncerc.mainproject.models.TensorFlowClassifier;
import com.udojava.evalex.Expression;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY_INV;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;

public class PreviewActivity extends AppCompatActivity
{
    private static final String TAG = "PreviewActivity";

    private ImageView previewImage;
    private Bitmap originalBitmap;
    private TextView tensorTextView;

    private List<Classifier> mClassifiers = new ArrayList<>();
    private List<Element> mElements = new ArrayList<>();

    //TODO remove Debug Elements
    private ImageView thresoldImageView, detectImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        //Get the image from result holder and display it
        originalBitmap = ResultHolder.getImage();
        previewImage = findViewById(R.id.preview_image);
        previewImage.setImageBitmap(originalBitmap);

        tensorTextView = findViewById(R.id.tensorflow_text);

        //Load the tensorflow trained model
        loadModel();

        //Debug
        thresoldImageView = findViewById(R.id.threshold_image);
        detectImageView = findViewById(R.id.detect_contour_image);
    }

    //creates a model object in memory using the saved tensorflow protobuf model file
    //which contains all the learned weights
    private void loadModel()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    //Tensorflow MNIST Classifier
//                    mClassifiers.add(
//                            TensorFlowClassifier.create(getAssets(), "TensorFlow",
//                                    "opt_mnist_convnet-tf.pb", "labels.txt", 28,
//                                    "input", "output", true));
                    //Keras MNIST Classifier
//                    mClassifiers.add(
//                            TensorFlowClassifier.create(getAssets(), "Number",
//                                    "opt_mnist_convnet-keras.pb", "labels.txt", 28,
//                                    "conv2d_1_input", "dense_2/Softmax", false));
                    //Keras full Classifier
                    mClassifiers.add(
                            TensorFlowClassifier.create(getAssets(), "Full",
                                    "opt_full_convnet.pb", "numbers.txt", 28,
                                    "conv2d_1_input", "dense_2/Softmax", false));
                    //Keras operator Classifier
//                    mClassifiers.add(
//                            TensorFlowClassifier.create(getAssets(), "Operators",
//                                    "opt_op_convnet.pb", "operators.txt", 28,
//                                    "conv2d_1_input", "dense_2/Softmax", false));
                }
                catch (final Exception e)
                {
                    //if they aren't found, throw an error!
                    throw new RuntimeException("Error initializing classifiers!", e);
                }
            }
        }).start();
    }

    public void onButtonPressed(View view)
    {
        preprocessImage(originalBitmap);

    }

    private void preprocessImage(Bitmap bitmap)
    {
        Mat srcMat = new Mat();

        Utils.bitmapToMat(bitmap, srcMat);
        Mat tempMat = srcMat.clone();

        // Grayscale and blur the image and display it
        tempMat = grayAndBlur(tempMat);
        Bitmap grayAndBlurBitmap = Bitmap.createBitmap(srcMat.width(), srcMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tempMat, grayAndBlurBitmap);

        //Threshold the image and display it
        tempMat = threshold(tempMat);
        Bitmap thresholdBitmap = Bitmap.createBitmap(srcMat.width(), srcMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tempMat, thresholdBitmap);
        thresoldImageView.setImageBitmap(thresholdBitmap);

        srcMat = detectDigits(tempMat, srcMat);
        Bitmap finalBitmap = Bitmap.createBitmap(srcMat.width(), srcMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(srcMat, finalBitmap);
        detectImageView.setImageBitmap(finalBitmap);

        srcMat.release();
        tempMat.release();
    }

    private Mat grayAndBlur(Mat src)
    {
        Mat temp = src.clone();
        Imgproc.cvtColor(src, temp, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(temp, src, new Size(5, 5), 0);
        return src;
    }

    private Mat threshold(Mat src)
    {
        Mat temp = src.clone();
        Imgproc.threshold(src, temp, 120, 255, THRESH_BINARY_INV | THRESH_OTSU);
        return temp;
    }

    private Mat detectDigits(Mat processedMat, Mat origMat)
    {
        mElements.clear();

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(processedMat, contours, new Mat(), Imgproc.RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

        for (int i = 0; i < contours.size(); i++)
        {
            if (Imgproc.contourArea(contours.get(i)) > 50)
            {
                Rect rect = Imgproc.boundingRect(contours.get(i));
                if (rect.height > 28)
                {
                    Imgproc.rectangle(origMat, new Point(rect.x, rect.y),
                            new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0));

                    //Crop over the rectangle and resize
                    Mat cropped = processedMat.submat(rect);
                    Mat dest = new Mat(28, 28, CvType.CV_8UC1);
                    dest = prepareForRecognition(cropped, dest);

                    Bitmap bitmap = Bitmap.createBitmap(dest.width(), dest.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(dest, bitmap);

                    String recogText = recognize(bitmap);

                    if (recogText.length() > 0)
                    {
                        Element element = new Element(recogText, rect.x, rect.y);
                        mElements.add(element);
                    }

                    Imgproc.putText(origMat, recogText, new Point(rect.x, rect.y), Core.FONT_HERSHEY_PLAIN,
                            1, new Scalar(255, 255, 255));
                }
            }
        }

        orderElements();
        elementsToString();

        return origMat;
    }

    private void orderElements()
    {
        Collections.sort(mElements, new Comparator<Element>()
        {
            @Override
            public int compare(Element lhs, Element rhs)
            {
                return lhs.getxPos() < rhs.getxPos() ? -1 : (lhs.getxPos() > rhs.getxPos()) ? 1 : 0;
            }
        });
    }

    private void elementsToString()
    {
        String result = "";
        for (Element element : mElements)
        {
            result += element.getElement();
        }
        tensorTextView.setText(result);

        displayResult(result);
    }

    private Mat prepareForRecognition(Mat cropped, Mat dest)
    {
        float factor;
        int rows = cropped.rows();
        int cols = cropped.cols();

        Log.d(TAG, "Before resizing : " + rows + " x " + cols);

        if (rows > cols)
        {
            factor = 20.0f / rows;
            rows = 20;
            cols = Math.round(cols * factor);
        }
        else
        {
            factor = 20.0f / cols;
            cols = 20;
            rows = Math.round(rows * factor);
        }
        Imgproc.resize(cropped, cropped, new Size(cols, rows));

        Log.d(TAG, "After resizing to 20px on one side : " + cropped.rows() + " x " + cropped.cols());

        int top = (int) Math.ceil((28 - rows) / 2.0);
        int bottom = (int) Math.floor((28 - rows) / 2.0);
        int left = (int) Math.ceil((28 - cols) / 2.0);
        int right = (int) Math.floor((28 - cols) / 2.0);

        Core.copyMakeBorder(cropped, dest, top, bottom, left, right, Core.BORDER_CONSTANT);

        Log.d(TAG, "After resizing to 28x28 : " + dest.rows() + " x " + dest.cols());

        return dest;
    }

    private String recognize(Bitmap bitmap)
    {
        int pixels[] = new int[28 * 28];
        float fPixels[] = new float[28 * 28];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < pixels.length; i++)
        {
            if (pixels[i] >= -10)
            {
                fPixels[i] = 1.0f;
            }
            else
            {
                fPixels[i] = 0.0f;
            }
        }

        //init an empty string to fill with the classification output
        String text = "";

        float conf = 0;
        float numConf = -1, opConf = -1;
        String num = "", op = "";
        //for each classifier in our array
        for (Classifier classifier : mClassifiers)
        {
            //perform classification on the image
            final Classification res = classifier.recognize(fPixels);
            //if it can't classify, output a question mark
//            if (res.getLabel() == null)
//            {
//                text += classifier.name() + " : ?";
//            }
//            else
//            {
            //else output its name
            if (res.getConf() > conf)
            {
                conf = res.getConf();
                text = res.getLabel();
                Log.d(classifier.name(), res.getLabel() + ", " + res.getConf());
            }
        }

        return text;
    }

    private void displayResult(final String expr)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Expression expression = new Expression(expr);
                    TextView resultText = findViewById(R.id.result_text);
                    resultText.setText(expression.eval().toPlainString());
                }
                catch (Exception e){}
            }
        }).start();
    }
}

class Element
{
    private String element;
    private int xPos, yPos;

    public Element(String element, int xPos, int yPos)
    {
        this.element = element;
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public String getElement()
    {
        return element;
    }

    public int getxPos()
    {
        return xPos;
    }

    public int getyPos()
    {
        return yPos;
    }
}