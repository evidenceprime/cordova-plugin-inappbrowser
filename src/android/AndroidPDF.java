package org.apache.cordova.inappbrowser;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.apache.cordova.CordovaInterface;
import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Dariusz on 24/07/15.
 */
public class AndroidPDF {

    final String LOG_TAG;
    CordovaInterface cordova;
    public AndroidPDF(CordovaInterface cordova,String LOG_TAG) {
        this.cordova =cordova;
        this.LOG_TAG = LOG_TAG;
    }

    public static File fileFromAsset(Context context, String assetName) throws IOException {
        File outFile = File.createTempFile("temp", ".pdf",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        copy(context.getResources().getAssets().open(assetName), outFile);
        return outFile;
    }

    public static void copy(InputStream inputStream, File output) throws IOException {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(output);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        }
    }


    private File DownloadFromUrl(final Context context, final String file_http_url) {


        final File[] file = {null};
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ProgressDialog progress = new ProgressDialog(cordova.getActivity() );

                progress.setTitle("Please wait");
                progress.setCancelable(true);
                progress.setMessage("PDF file downloading...");

                final Thread t = new Thread() {
                    private boolean downloadCancel = false;
                    private boolean runned = false;
                    public void run() {
                        if(runned) {
                            downloadCancel = true;
                            return;
                        }
                        runned = true;
                        Intent intent = null;

                        try {
                            URL url = new URL(file_http_url); //you can write here any link
                            String fileName = "temp";
                            file[0] = File.createTempFile(fileName, ".pdf", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
                            long startTime = System.currentTimeMillis();
                            Log.d(LOG_TAG, "Starting download......from " + url);
                            URLConnection ucon = url.openConnection();
                            InputStream is = ucon.getInputStream();
                            BufferedInputStream bis = new BufferedInputStream(is);
                            if(downloadCancel) return;
                    /*
                     * Read bytes to the Buffer until there is nothing more to read(-1).
                     */
                            FileOutputStream fos = new FileOutputStream(file[0]);
                            ByteArrayBuffer baf = new ByteArrayBuffer(50);
                            int current = 0;

                            while ((current = bis.read()) != -1) {
                                if(downloadCancel) return;
                                baf.append((byte) current);
                                if (baf.isFull()) {
                                    fos.write(baf.toByteArray());
                                    baf.clear();
                                }
                            }

                            if (!baf.isEmpty()) {
                                fos.write(baf.toByteArray());
                            }
                            fos.close();
                            Uri uri = Uri.fromFile(file[0]);


                            if(downloadCancel) return;

                            intent = new Intent(Intent.ACTION_VIEW);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.setDataAndType(uri, "application/pdf");
                            cordova.getActivity().startActivity(intent);

                        } catch (IOException e) {
                            Log.d(LOG_TAG, "Error: " + e);
                        }

                        synchronized (AndroidPDF.this) {
                            progress.dismiss();

                            AndroidPDF.this.notifyAll();

                        }
                    }
                };

                ProgressDialog.OnCancelListener cancel = new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        t.run();
                        Log.d(LOG_TAG, "interrrurrprr ");
                    }
                };
                progress.setOnCancelListener(cancel);

                progress.show();
                t.start();

            }
        });






        try {
            synchronized (this) {
                this.wait();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return file[0];
    }

    public  Boolean openExternalWithSpinner(final String url) {
        String extension = url.substring(url.lastIndexOf(".") + 1);
        Boolean isHttp = url.substring(0,7).equals("http://") || url.substring(0,8).equals("https://");
        String type = "application/pdf";

        final Context context =  cordova.getActivity().getApplicationContext();

        if (extension.toLowerCase().equals("pdf") )   {

            if(!isHttp) { // local pdf


                Log.d(LOG_TAG, "PDF file local: " + url);
                Intent intent = null;
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);


                File file = null;

                if(url.startsWith("file:///android_asset")) {
                    String assetURL = url.substring(22);
                    Log.d(LOG_TAG,"File from assets :"+assetURL);
                    try {
                        file = this.fileFromAsset(context,assetURL);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                else {
                    Log.d(LOG_TAG,"File from local filesystem"+url);
                    file = new File(url);
                }

                Log.d(LOG_TAG, "length: "+ file.length());

                Uri uri = Uri.fromFile(file);


                Log.d(LOG_TAG,"URI url: "+uri.getPath());

                intent.setDataAndType(uri, "application/pdf");
                cordova.getActivity().startActivity(intent);
                return true;
            }

            Log.d(LOG_TAG, "File http url: " + url);

            new Thread() {
                public void run() {

                    File file = DownloadFromUrl(context, url);
                }
            }.start();
            return true;
        }
        Log.d(LOG_TAG,"File url is not from pdf on server");
        return false;
    }




}
