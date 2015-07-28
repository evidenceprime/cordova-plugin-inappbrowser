package org.apache.cordova.inappbrowser;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
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

    private String  pdfExternalURl = "";
    public void displayPDFinNewActivity(final String localUrl) {
        Log.d(LOG_TAG,"localUrl = "+localUrl);
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Context appContext = cordova.getActivity().getApplicationContext();
                Intent pdfIntent = new Intent(appContext, PDFViewerActivity.class);
                pdfIntent.putExtra("url", localUrl);
                pdfIntent.putExtra("link",pdfExternalURl );
                pdfIntent.putExtra("LOG_TAG", LOG_TAG);
                cordova.getActivity().startActivity(pdfIntent);
            }
        });
    }


    public AndroidPDF(CordovaInterface cordova, String LOG_TAG) {
        this.cordova = cordova;
        this.LOG_TAG = LOG_TAG;
    }

    public static PDFSource checkSource(String url) {
        Boolean isHttp = url.startsWith("http://") || url.startsWith("https://");
        if(isHttp) return  PDFSource.INTERNET;
        if(url.startsWith("file:///android_asset/")) return  PDFSource.APP_ASSETS;
        return PDFSource.DEVICE_STORAGE;
    }

    private final String  tempFolder = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS) +"/temp_aria/";

//    private  final String tempFolder = cordova.getActivity().getCacheDir()+"/temp_aria/";

    void checkSpace( ) {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = (long)stat.getBlockSize() *(long)stat.getBlockCount();
        long megAvailable = bytesAvailable / 1048576;
        if(megAvailable<5) {
            File folderFile = new File(tempFolder);
            folderFile.delete();
        }
    }
//
//    void checkTempFolder() {
//        File folderFile = new File(tempFolder);
//        Log.d(LOG_TAG,"Folder: "+folderFile.getAbsolutePath());
//        if(!folderFile.exists()) {
//            Log.d(LOG_TAG,"Folde not exist");
//            folderFile.mkdirs();
//        }
//    }

    void deleteTempFolder() {
        File folderFile = new File(tempFolder);
        folderFile.deleteOnExit();
    }

    File createFrom(String url) {
        String filename = Uri.parse(url).getLastPathSegment();
        Log.d(LOG_TAG, "filename: " + filename);
        deleteTempFolder();
        File file = new File(
                tempFolder,
                filename
        );
        if(!file.exists()) {
            Log.d(LOG_TAG,"File not exist: "+file.getAbsolutePath());
        }
        return file;
    }


    private void DownloadFromUrl(final Context context, final String file_http_url) {

        final File[] file = {null};
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ProgressDialog progress = new ProgressDialog(cordova.getActivity());

                progress.setTitle("Please wait");
                progress.setCancelable(true);
                progress.setMessage("PDF file downloading...");

                final Thread t = new Thread() {
                    private boolean downloadCancel = false;
                    private boolean runned = false;
                    public void run() {
                        if (runned) {
                            downloadCancel = true;
                            file[0].delete();
                            return;
                        }
                        runned = true;
                        try {
                            URL url = new URL(file_http_url);
                            file[0] = createFrom(file_http_url);
                            Uri uri = Uri.fromFile(file[0]);
                            if(file[0].exists()) {
                                downloadCancel = true;
                                progress.dismiss();
                                displayPDFinNewActivity(uri.getPath());
                            }
                            if (downloadCancel) return;
                            long startTime = System.currentTimeMillis();
                            Log.d(LOG_TAG, "Starting download......from " + url);
                            URLConnection ucon = url.openConnection();
                            InputStream is = ucon.getInputStream();
                            BufferedInputStream bis = new BufferedInputStream(is);
                            if (downloadCancel) return;
                            //Read bytes to the Buffer until there is nothing more to read(-1).
                            FileOutputStream fos = new FileOutputStream(file[0]);
                            ByteArrayBuffer baf = new ByteArrayBuffer(50);
                            int current = 0;

                            while ((current = bis.read()) != -1) {
                                if (downloadCancel) return;
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

                            if (downloadCancel) return;
                            displayPDFinNewActivity(uri.getPath());

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
    }

    public Boolean pdfViewer(final String url) {
        Log.d(LOG_TAG, "File http url: " + url);
        pdfExternalURl = url;
        String extension = url.substring(url.lastIndexOf(".") + 1);
        final Context context = cordova.getActivity().getApplicationContext();

        if (extension.toLowerCase().equals("pdf")) {
            if ( checkSource(url) == PDFSource.APP_ASSETS) {
                String tempUrl = url.replace("file:///android_asset/", "");
                File file = null;
                try {
                    file = createFrom(tempUrl);
                    if(!file.exists()) {
                        copy( context.getResources().getAssets().open(tempUrl), file );
                    }
                }
                catch (IOException ex) {
                    Log.d(LOG_TAG,"Cannot create local file from assets file, fil:"+ex.getMessage());
                    return false;
                }
                displayPDFinNewActivity(file.getAbsolutePath());
                return true;
            }
            if(  checkSource(url) == PDFSource.INTERNET ) {
                new Thread() {
                    public void run() {
                        DownloadFromUrl(context, url);
                    }
                }.start();
                return true;
            }
            if(  checkSource(url) == PDFSource.DEVICE_STORAGE ) {
                displayPDFinNewActivity(url);
                return true;
            }

        }
        Log.d(LOG_TAG, "File url is not from pdf on server");
        return false;
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


}
