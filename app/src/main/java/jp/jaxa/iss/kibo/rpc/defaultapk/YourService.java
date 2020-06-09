package jp.jaxa.iss.kibo.rpc.defaultapk;

import android.graphics.Bitmap;
import android.util.Log;

import gov.nasa.arc.astrobee.Kinematics;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */



public class YourService extends KiboRpcService {


    @Override
    protected void runPlan1() {

        api.judgeSendStart();
        //First transition point
        Boolean first = moveToWrapper(11, -4.7, 4.53, 0.05, 0.049, -0.705, 0.705, 99);

        //Scanning the first 3 QR Codes
        moveToWrapper(11, -5.5, 4.40, 0.500, 0.500, -0.500, 0.500, 2);
        decodeQRCode(2, true);
        moveToWrapper(11.37, -5.70, 4.5, 0, 0, 0, 1, 0);
        decodeQRCode(0, true);
        moveToWrapper(11, -6, 5.45,  0.707, 0,  0.707, 0, 1);
        decodeQRCode(1, true);

        //Another three transition point
        moveToWrapper(10.45, -6.25, 4.7, 0, 0, 0.7071068, -0.7071068, 99);
        moveToWrapper(10.45, -6.75, 4.7, 0, 0, 0, 1, 99);
        moveToWrapper(10.95, -6.75, 4.7, 0, 0, 0, 1, 99);

        //Scanning the last 3 QR Codes
        moveToWrapper(11.40, -8, 5, 0, 0, 1, 0, 4);
        decodeQRCode(4, false);
        moveToWrapper(11, -7.7, 5.45, 0.707, 0,  0.707, 0, 5);
        decodeQRCode(5, true);
        moveToWrapper(10.40, -7.5, 4.7, 0, 0, 1, 0, 3);
        decodeQRCode(3, true);

        //Move to AR transition point
        moveToWrapper(10.95, -9.1, 4.9, 0, 0, 0.7071068, -0.7071068, 99);

        //Get the final AR position
        double qr_pos_x = Double.valueOf(QRData.pos_x);
        double qr_pos_y = Double.valueOf(QRData.pos_y);
        double qr_pos_z = Double.valueOf(QRData.pos_z);

        //Move to AR
        moveToWrapper(qr_pos_x, qr_pos_y , qr_pos_z , 0, 0, 0.707, -0.707, 99);

        //Scanning AR Code (returning translation vector)
        Mat offset_ar = decode_AR();

        if (offset_ar != null){

            //Offset data for lasering
            double offset_target_laser_x = 0.141421356; //0.1*sqrt(2) m
            double offset_target_laser_z = 0.141421356;
            double offset_camera_x = -0.0572;
            double offset_camera_z = 0.1111;
            double added_offset_x = -0.051;
            double added_offset_z = -0.075;

            //Acquiring translation vector as the error offset
            double[] offset_ar_largest = offset_ar.get(0, 0);

            double p3_posx = qr_pos_x + offset_ar_largest[0] + offset_target_laser_x + offset_camera_x + added_offset_x;
            double p3_posz = qr_pos_z + offset_ar_largest[1] + offset_target_laser_z + offset_camera_z + added_offset_z;

            //move to target p3 for lasering (with plan B)
            if(!moveToWrapper(p3_posx, qr_pos_y , p3_posz ,
                0, 0, 0.707, -0.707, 99)){

                moveToWrapper(p3_posx, qr_pos_y+0.1 , p3_posz , 0, 0, 0.707, -0.707, 99);

            }

            api.laserControl(true);

            api.judgeSendFinishSimulation();

        } else{

            api.judgeSendFinishSimulation();
        }


    }



    @Override
    protected void runPlan2() {


    }

    @Override
    protected void runPlan3() {


    }



    private Boolean moveToWrapper(double pos_x, double pos_y, double pos_z,
                               double qua_x, double qua_y, double qua_z,
                               double qua_w, int abs_point) {

        Log.i("SPACECAT", "moveToWrapper function is called, moving bee to QR" + abs_point);

        final int LOOP_MAX = 30;
        final Point point = new Point(pos_x, pos_y, pos_z);
        final Quaternion quaternion = new Quaternion((float) qua_x, (float) qua_y,
                (float) qua_z, (float) qua_w);

        Log.i("SPACECAT", "[" + 0 + "] Calling api.moveTo");
        Result result = api.moveTo(point, quaternion, false);
        Log.i("SPACECAT", "[" + 0 + "] result: " + result.getMessage());

        int loopCounter = 1;
        while (!result.hasSucceeded() && loopCounter <= LOOP_MAX) {

            Log.i("SPACECAT", "[" + loopCounter + "] Calling API moveTo");
            result = api.moveTo(point, quaternion, false);
            Log.i("SPACECAT", "[" + loopCounter + "] result: " + result.getMessage());
            ++loopCounter;

        }

        Boolean arrived = result.hasSucceeded() ? true : false;
        return arrived;
    }

    private Boolean decodeQRCode(int target_qr, boolean useNavcam) {

        QRCodeReader reader = new QRCodeReader();

        Mat navcam_mat;
        Mat navcam_mat_undistort = new Mat();
        Bitmap navcam_bit_undistort;

        Size sz = new Size(640,480);

        //get camera data once to initialize width, height and pixels
        BinaryBitmap navcam_bin;
        com.google.zxing.Result result;
        RGBLuminanceSource navcam_luminance;

        int MAX_RETRY_TIMES = 7;
        int retryTimes = 0;

        String qr_string = "";

        while (qr_string == "" && retryTimes < MAX_RETRY_TIMES) {

            try{

                reader.reset();

                //get navcam matrix
                    if(useNavcam){
                        navcam_mat = api.getMatNavCam();
                    }else{
                        navcam_mat = api.getMatDockCam();
                    }


                //navcam_mat_undistort = navcam_mat;

                Imgproc.resize(navcam_mat, navcam_mat_undistort, sz );

                //Convert navcam to bitmap
                navcam_bit_undistort = matToBitmap(navcam_mat_undistort);

                //Getting pixels data
                int width = navcam_bit_undistort.getWidth();
                int height = navcam_bit_undistort.getHeight();
                int[] navcam_pixels = new int[width * height];

                //get the pixels out of bitmap
                navcam_bit_undistort.getPixels(navcam_pixels, 0, width, 0, 0, width, height);

                //get the luminance data
                navcam_luminance = new RGBLuminanceSource(width, height, navcam_pixels);

                //convert to binary image
                navcam_bin = new BinaryBitmap(new HybridBinarizer(navcam_luminance));

                //decoding QR result
                result = reader.decode(navcam_bin);
                qr_string = result.getText();

                if (qr_string != "") {

                    api.judgeSendDiscoveredQR(target_qr, qr_string);

                    switch(target_qr){
                        case 0: QRData.pos_x = remove_identifier(qr_string); break;
                        case 1: QRData.pos_y = remove_identifier(qr_string); break;
                        case 2: QRData.pos_z = remove_identifier(qr_string); break;
                        case 3: QRData.qua_x= remove_identifier(qr_string); break;
                        case 4: QRData.qua_y = remove_identifier(qr_string); break;
                        case 5: QRData.qua_z = remove_identifier(qr_string); break;
                    }

                    Log.i("QR FOUND: ", qr_string);
                    return true;
                }

            } catch (Exception e) {
                Log.i("QR NOT FOUND", Integer.toString(retryTimes));
                qr_string = "";
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            retryTimes++;
        }

        return false;
    }

    private String remove_identifier(String rec_data){
        String[] arrOfStr;

        //split the data to acquire only the component's value
        arrOfStr = rec_data.split(",", 2);

        //remove all white spaces
        arrOfStr[1].replaceAll("\\s+","");

        return arrOfStr[1];
    }


    private Mat decode_AR(){
        Mat markerIds = new Mat();
        List<Mat> corners= new ArrayList<>();
        List<Mat> rejected= new ArrayList<>();
        Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);

        Mat camMatrix = new Mat(3, 3, CvType.CV_32F);
        Mat distCoeff = new Mat(1, 5, CvType.CV_32F);

        Mat rvec = new Mat();
        Mat tvec = new Mat();

        camMatrix.put(0,0, new float[]{344.173397f, 0.000000f, 630.793795f});
        camMatrix.put(1,0, new float[]{0.000000f, 344.277922f, 487.033834f});
        camMatrix.put(2,0, new float[]{0.000000f, 0.000000f, 1.000000f});
        distCoeff.put(0,0, new float[]{-0.152963f, 0.017530f, -0.001107f, -0.000210f, 0.000000f});

        DetectorParameters parameters = DetectorParameters.create();
        parameters.set_minDistanceToBorder(0);
        parameters.set_adaptiveThreshWinSizeMax(400);


        for (int i = 0; i < 5 && markerIds.cols() == 0 && markerIds.rows() == 0; i++){

            Mat nav_cam = api.getMatNavCam();

            Aruco.detectMarkers(nav_cam/*dst*/, dictionary, corners, markerIds, parameters, rejected, camMatrix, distCoeff);

            if(markerIds.cols() != 0 && markerIds.rows() != 0){

                //Converting AR value from double to string
                double ARDouble = markerIds.get(0, 0)[0];
                int ARint = (int) ARDouble;
                QRData.ar_id = Integer.toString(ARint);

                Log.i("AR FOUND: ", QRData.ar_id);
                api.judgeSendDiscoveredAR(QRData.ar_id);

                //Pose estimation
                Aruco.estimatePoseSingleMarkers(corners, 0.05f, camMatrix, distCoeff, rvec, tvec);

                /*
                Log.i("Corners", Integer.toString(corners.size()));
                Log.i("id", markerIds.dump());
                Log.i("rejected", Integer.toString(rejected.size()));
                */

                return tvec;
            }

            Log.i("AR NOT FOUND", "TRIAL " + (i + 1));

        }
        return null;
    }

    private Bitmap matToBitmap(Mat in){

        Bitmap bmp = null;
        try {

            bmp = Bitmap.createBitmap(in.cols(), in.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(in, bmp);

        }
        catch (CvException e){Log.d("Exception",e.getMessage());}

        return bmp;
    }

}

//Struct like container for QR position and quaternion string data
class QRData {

    static String pos_x;
    static String pos_y;
    static String pos_z;
    static String qua_x;
    static String qua_y;
    static String qua_z;
    static String qua_w;
    static String ar_id;

}