package jp.jaxa.iss.kibo.rpc.defaultapk;

import android.graphics.Bitmap;
import android.util.Log;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;


/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class YourService extends KiboRpcService {


    @Override
    protected void runPlan1() {
        api.judgeSendStart();

        moveToWrapper(11.37, -5.75, 4.5, 0, 0, 0, 1);
        decodeQRCode(0);
        moveToWrapper(11, -6, 5.45, 0, -0.7071068, 0, 0.7071068);
        decodeQRCode(1);
        moveToWrapper(11, -5.5, 4.43, 0, 0.7071068, 0, 0.7071068);
        decodeQRCode(2);

        moveToWrapper(10.45, -6.45, 4.7, 0, 0, 0.7071068, -0.7071068);
        moveToWrapper(10.45, -6.75, 4.7, 0, 0, 0, 1);
        moveToWrapper(10.95, -6.75, 4.7, 0, 0, 0, 1);
        moveToWrapper(10.95, -7.7, 4.7, 0, 0, 0.7071068, -0.7071068);

        moveToWrapper(10.40, -7.5, 4.7, 0, 0, 1, 0);
        decodeQRCode(3);
        moveToWrapper(11.40, -8, 5, 0, 0, 0, 1);
        decodeQRCode(4);
        moveToWrapper(11, -7.7, 5.45, 0, -0.7071068, 0, 0.7071068);
        decodeQRCode(5);


        moveToWrapper(11.05, -7.7, 4.65, 0, 0, 0.7071068, -0.7071068);



        double qr_pos_x = Double.valueOf(QRData.pos_x);
        double qr_pos_y = Double.valueOf(QRData.pos_y);
        double qr_pos_z = Double.valueOf(QRData.pos_z);

        moveToWrapper(11.05, qr_pos_y, 4.65, 0, 0, 0.7071068, -0.7071068);

        //get to the actual P3
        moveToWrapper(qr_pos_x, qr_pos_y, qr_pos_z, 0, 0, 0.7071068, -0.7071068);

        decode_AR();


        api.judgeSendFinishSimulation();
    }


    @Override
    protected void runPlan2() {

        api.judgeSendStart();

        decode_AR();

    }

    @Override
    protected void runPlan3() {




        //ImageSaver.save_image(undistored);


    }

    private void moveToWrapper(double pos_x, double pos_y, double pos_z,
                               double qua_x, double qua_y, double qua_z,
                               double qua_w) {


        final int LOOP_MAX = 3;
        final Point point = new Point(pos_x, pos_y, pos_z);
        final Quaternion quaternion = new Quaternion((float) qua_x, (float) qua_y,
                (float) qua_z, (float) qua_w);

        Result result = api.moveTo(point, quaternion, false);

        int loopCounter = 0;
        while (!result.hasSucceeded() || loopCounter < LOOP_MAX) {

            result = api.moveTo(point, quaternion, false);
            ++loopCounter;

        }
    }


    private Void decodeQRCode(int target_qr) {

        QRCodeReader reader = new QRCodeReader();

        //get camera data once to initialize width, height and pixels
        Bitmap navcam = api.getBitmapNavCam();
        BinaryBitmap navcam_bin = null;
        com.google.zxing.Result result = null;
        RGBLuminanceSource navcam_luminance;

        int MAX_RETRY_TIMES = 7;
        int retryTimes = 0;
        int width = navcam.getWidth();
        int height = navcam.getHeight();
        int[] navcam_pixels = new int[width * height];
        String qr_string = "";

        while (qr_string == "" && retryTimes < MAX_RETRY_TIMES) {

            try{

            reader.reset();

            //getting navigation cam bitmap data for real time decoding
            navcam = api.getBitmapNavCam();

            //get the pixels out of bitmap
            navcam.getPixels(navcam_pixels, 0, width, 0, 0, width, height);

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

                    Log.i("QR Scanner","[" + retryTimes + "] QR Code is found: " + qr_string);
                }

            } catch (Exception e) {
                Log.i("QR Scanner", "[" + retryTimes + "] QR Code is not found");
                qr_string = "";
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            retryTimes++;
        }
        return null;
    }

    private String remove_identifier(String rec_data){
        String[] arrOfStr;

        //split the data to acquire only the component's value
        arrOfStr = rec_data.split(",", 2);

        //remove all white spaces
        arrOfStr[1].replaceAll("\\s+","");

        return arrOfStr[1];

    }

    private double calculate_w(double x, double y, double z){

        //Calculate quaternion W rotation based on x, y and z
        return Math.sqrt( 1 - (Math.pow(x, 2) + Math.pow(y, 2)+ Math.pow(z, 2)) ) ;

    }

    private Mat undistort_camera(Mat img){

        //Declaring camera matrix and distortion coefficient
        Mat camMatrix = new Mat(3, 3, CvType.CV_32F);
        Mat distCoeff = new Mat(1, 5, CvType.CV_32F);

        camMatrix.put(0,0, new float[]{344.173397f, 0.000000f, 630.793795f});
        camMatrix.put(1,0, new float[]{0.000000f, 344.277922f, 487.033834f});
        camMatrix.put(2,0, new float[]{0.000000f, 0.000000f, 1.000000f});

        distCoeff.put(0,0, new float[]{-0.152963f, 0.017530f, -0.001107f, -0.000210f, 0.000000f});

        Mat nav_cam;
        nav_cam = api.getMatNavCam();

        Mat dst = new Mat(nav_cam.rows(), nav_cam.cols(), nav_cam.type());

        Imgproc.undistort(nav_cam, dst, camMatrix, distCoeff);

        return dst;
    }

    private void decode_AR(){
        Mat nav_cam;
        nav_cam = api.getMatNavCam();

        Mat dst;

        dst = undistort_camera(nav_cam);

        Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_50);

        /*
        DetectorParameters parameters = DetectorParameters.create();
        parameters.set_minDistanceToBorder(0);
        parameters.set_adaptiveThreshWinSizeMax(400);
        */

        List<Mat> corners= new ArrayList<>();
        List<Mat> rejected= new ArrayList<>();
        Mat markerIds = new Mat();

        Aruco.detectMarkers(dst, dictionary, corners, markerIds /*, parameters, rejected*/);

        QRData.ar_id = markerIds.get(0,0).toString();
        
        api.judgeSendDiscoveredAR(QRData.ar_id);

        Log.i("Corners", Integer.toString(corners.size()));
        Log.i("id", markerIds.dump());
        Log.i("rejected", Integer.toString(rejected.size()));
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