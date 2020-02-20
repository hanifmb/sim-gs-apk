package jp.jaxa.iss.kibo.rpc.defaultapk;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import gov.nasa.arc.astrobee.Kinematics;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.decoder.Decoder;
import com.google.zxing.MultiFormatReader;
// other Result import com.google.zxing.Result;

import org.opencv.core.Mat;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class YourService extends KiboRpcService {

    Quaternion current_orientation;
    Point current_position;

    @Override
    protected void runPlan1(){
        api.judgeSendStart();

        //kinematics_thread thread = new kinematics_thread();
        //thread.execute();

        moveToWrapper(11.37, -5.75, 4.5, 0, 0, 0, 1, 0, true);
        moveToWrapper(11, -6, 5.45, 0, -0.7071068, 0, 0.7071068, 1, true);
        moveToWrapper(11, -5.5, 4.43, 0, 0.7071068, 0, 0.7071068, 2, true);

        moveToWrapper(10.45, -6.45, 4.7, 0, 0, 0.7071068, -0.7071068, 0, false);
        moveToWrapper(10.45, -6.75, 4.7, 0, 0, 0, 1, 0, false);
        moveToWrapper(10.95, -6.75, 4.7, 0, 0, 0, 1, 0, false);

        moveToWrapper(11.40, -8, 5, 0, 0, 0, 1, 4, true);
        moveToWrapper(11, -7.7, 5.45, 0, -0.7071068, 0, 0.7071068, 5, true);
        moveToWrapper(10.40, -7.5, 4.7, 0, 0, 1, 0, 3, true);

        api.laserControl(true);
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

    // You can add your method
    private void moveToWrapper(double pos_x, double pos_y, double pos_z,
                               double qua_x, double qua_y, double qua_z,
                               double qua_w, int targetQR, boolean scanQR){

        final int LOOP_MAX = 3;
        final Point point = new Point(pos_x, pos_y, pos_z);
        final Quaternion quaternion = new Quaternion((float)qua_x, (float)qua_y,
                                                     (float)qua_z, (float)qua_w);

        Result result = api.moveTo(point, quaternion, false);
        String QRString = "not found";

        int loopCounter = 0;
        while(!result.hasSucceeded() || loopCounter < LOOP_MAX){
            result = api.moveTo(point, quaternion, false);

            if (scanQR == true){
                QRString = decodeQRCode();
            }

            if(QRString != "not found"){
                api.judgeSendDiscoveredQR(targetQR, QRString);
                Log.d("QR Data",QRString);

                break;
            }
            else{
                Log.d("QR Data:",QRString);
            }

            ++loopCounter;
        }
    }


    private String decodeQRCode() {
        try {
            QRCodeReader reader = new QRCodeReader();

            //getting navigation cam bitmap data
            Bitmap navcam = api.getBitmapNavCam();

            BinaryBitmap dicek = null;
            com.google.zxing.Result result = null;

            //Getting navigation cam parameters
            int width = navcam.getWidth();
            int height = navcam.getHeight();
            int[] pixels = new int[width * height];


            navcam.getPixels(pixels, 0, width, 0, 0, width, height);
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            dicek = new BinaryBitmap(new HybridBinarizer(source));

            //Decoding QR data
            result = reader.decode(dicek);

            return result.getText();
        } catch (Exception e) {
            return "not found";
        }
    }


}

