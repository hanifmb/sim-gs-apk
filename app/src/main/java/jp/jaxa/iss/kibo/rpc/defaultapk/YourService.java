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


/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class YourService extends KiboRpcService {

    @Override
    protected void runPlan1(){
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


        moveToWrapper(11.40, -8, 5, 0, 0, 0, 1);
        decodeQRCode(4);
        moveToWrapper(11, -7.7, 5.45, 0, -0.7071068, 0, 0.7071068);
        decodeQRCode(5);
        moveToWrapper(10.40, -7.5, 4.7, 0, 0, 1, 0);
        decodeQRCode(3);

        api.judgeSendFinishSimulation();
    }

    @Override
    protected void runPlan2(){
        // write here your plan 2
    }

    @Override
    protected void runPlan3(){
        // write here your plan 3
    }

    private void moveToWrapper(double pos_x, double pos_y, double pos_z,
                               double qua_x, double qua_y, double qua_z,
                               double qua_w){

        final int LOOP_MAX = 3;
        final Point point = new Point(pos_x, pos_y, pos_z);
        final Quaternion quaternion = new Quaternion((float)qua_x, (float)qua_y,
                                                     (float)qua_z, (float)qua_w);

        Result result = api.moveTo(point, quaternion, false);

        int loopCounter = 0;
        while(!result.hasSucceeded() || loopCounter < LOOP_MAX){

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

        int width = navcam.getWidth();
        int height = navcam.getHeight();
        int[] navcam_pixels = new int[width * height];

        String qr_string = "not found";

        for (int i=0; i<5; i++){
            try {

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

                //get the string data
                qr_string = result.getText();

                if (qr_string != "not found"){
                    api.judgeSendDiscoveredQR(target_qr, qr_string);
                    break;
                }
            } catch (Exception e) {
                qr_string = "not found";
            }

            Log.d("QR Code", qr_string);
        }

        return null;
    }
}

