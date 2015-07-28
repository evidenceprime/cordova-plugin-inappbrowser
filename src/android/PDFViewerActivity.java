package org.apache.cordova.inappbrowser;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.ShareActionProvider;

import com.evidenceprime.ksa.R;
import com.joanzapata.pdfview.PDFView;
import com.joanzapata.pdfview.listener.OnDrawListener;
import com.joanzapata.pdfview.listener.OnLoadCompleteListener;
import com.joanzapata.pdfview.listener.OnPageChangeListener;
import com.joanzapata.pdfview.util.FileUtils;

import org.apache.cordova.LOG;
import org.apache.cordova.inappbrowser.AndroidPDF;
import org.apache.cordova.inappbrowser.PDFSource;

import java.io.File;
import java.io.IOException;


public class PDFViewerActivity extends ActionBarActivity {

    class UrlDelegate {
        public String path;
        public File file;
    }

    UrlDelegate res = new UrlDelegate();

    private PDFSource pdfSource = PDFSource.UNKNOW;
    private String pdfLocalURL = "";
    private String pdfExternalURL = "";
    private String LOG_TAG = "";
    private File localFileForSharing = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("PDF Viewer");

        setContentView(R.layout.activity_pdfviewer);
        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        pdfLocalURL = args.getString("url");
        pdfExternalURL =  args.getString("link");
        LOG_TAG = args.getString("LOG_TAG");
        pdfSource = AndroidPDF.checkSource(pdfExternalURL);
        localFileForSharing = new File(pdfLocalURL);
        PDFView pdfView = (PDFView) findViewById(R.id.pdfview);

        Log.d(LOG_TAG,"PDF: "+localFileForSharing.getAbsolutePath());
        pdfView.fromFile(localFileForSharing)
                .defaultPage(1)
                .showMinimap(false)
                .enableSwipe(true)
                .swipeVertical(true)
                .load();




    }
    private ShareActionProvider mShareActionProvider;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pdfviewer, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = new ShareActionProvider(this);
        MenuItemCompat.setActionProvider(menuItem, mShareActionProvider);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.menu_item_share){
            onShareAction();
            return  true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void onShareAction(){
        Intent shareIntent = null;

        // Create the share Intent
        if(pdfSource == PDFSource.INTERNET) {
            Log.d(LOG_TAG,"Trying to share link:"+pdfExternalURL);
            String yourShareText = pdfExternalURL;
            shareIntent = ShareCompat.IntentBuilder.from(this).setType("text/plain").setText(yourShareText).getIntent();
        }
        else if(pdfSource == PDFSource.APP_ASSETS || pdfSource == PDFSource.DEVICE_STORAGE ) {
            Log.d(LOG_TAG,"Trying to share file from:"+localFileForSharing.getAbsolutePath());
            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_TEXT, pdfExternalURL );
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + localFileForSharing.toString() ));
        }
        // Set the share Intent
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }


//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
//
//

}
