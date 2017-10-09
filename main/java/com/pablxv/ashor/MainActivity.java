package com.pablxv.ashor;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.pdf.PdfDocument;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;


public class MainActivity extends ActionBarActivity {
    WebView wb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView.enableSlowWholeDocumentDraw();
        wb = new WebView(MainActivity.this);// webview in mainactivity

        setContentView(wb);// set the webview as the layout
        wb.loadUrl("file:///android_asset/index.html");
        wb.getSettings().setJavaScriptEnabled(true);
        JavaScriptInterface js = new JavaScriptInterface(this, wb);
        js.setMA(this);
        wb.addJavascriptInterface(js, "Android");
        //setContentView(findViewById(R.id.webView));//R.layout.activity_main);
    }

    protected void callJS(String t) {
        wb.loadUrl("javascript:showToast('" + t + "')");
    }
}

class JavaScriptInterface {
    Context mContext;
    WebView wb;
    MainActivity ma;
    Resources res; //if you are in an activity
    AssetManager am;
    private String filepath = "asamList";
    File myExternalFile;
    boolean externalSD;
    /** Instantiate the interface and set the context */

    JavaScriptInterface(Context c,WebView w) {
        mContext = c;
        wb=w;
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            showToast("La tarjeta SD no está disponible...");
            wb.post(new Runnable() {
                public void run() {
                    wb.loadUrl("javascript:sdNoDisp();");
                }
            });
            externalSD=false;
        }
        else externalSD=true;
    }

    void setMA(MainActivity m){
        ma=m;
        res = ma.getResources(); //if you are in an activity
        am = res.getAssets();
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    @android.webkit.JavascriptInterface
    public void showToast(String text) {
        Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
    }

    @android.webkit.JavascriptInterface
    public void recFileInter(String file, String text) {
        Log.i("APP", text);
        if(!externalSD) return;
        //guardar archivo
        try {
            myExternalFile = new File(mContext.getExternalFilesDir(filepath), file);
            FileOutputStream fos = new FileOutputStream(myExternalFile);
            fos.write(text.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @android.webkit.JavascriptInterface
    public void cargarFile(String file) {
        BufferedReader reader = null;
        String content="";
        if(file.indexOf(".asm*")<0) {//archivo en assets, sin encriptar
            try {
                reader = new BufferedReader(
                        new InputStreamReader(am.open("store/" + file), "UTF-8"));

                // do reading, usually loop until end of file reading

                String mLine;
                while ((mLine = reader.readLine()) != null) {
                    content += mLine;
                }
            } catch (IOException e) {
                //log the exception
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        //log the exception
                    }
                }
            }
        }

        else{
            if(externalSD) {
                try {
                    myExternalFile = new File(mContext.getExternalFilesDir(filepath), file);
                    FileInputStream fis = new FileInputStream(myExternalFile);
                    DataInputStream in = new DataInputStream(fis);
                    BufferedReader br =
                            new BufferedReader(new InputStreamReader(in));
                    String strLine;
                    while ((strLine = br.readLine()) != null) {
                        content += strLine;
                    }
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        final String cf=content;
        wb.post(new Runnable() {
            public void run() {
                wb.loadUrl("javascript:process('" + cf + "',true);");
            }
        });
    }

    @android.webkit.JavascriptInterface
    public void deleteFile(String file) {
        if(!externalSD) return;
        myExternalFile = new File(mContext.getExternalFilesDir(filepath), file);
        boolean deleted = myExternalFile.delete();
        if(deleted) showToast("Fichero borrado");
        else showToast("Ups! Error al borrar el fichero...");
    }

    @android.webkit.JavascriptInterface
    public void listFiles() {
        // de assets
        String fileList[] = new String[0];
        try {
            fileList = am.list("store");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String sl="";
        if (fileList != null)
        {
            for ( int i = 0;i<fileList.length;i++)
            {
                sl+=fileList[i]+'¡';
            }
        }
        // de SDcard
        if(externalSD) {
            String path = Environment.getExternalStorageDirectory().toString()+"/Android/data/com.pablxv.ashor/files/asamList";
            File f = new File(path);
            File files[] = f.listFiles();
            if(files!=null) {
                for (int i = 0; i < files.length; i++)
                    sl += files[i].getName() + '¡';
            }
        }
        final String s = sl;
        wb.post(new Runnable() {
            public void run() {
                wb.loadUrl("javascript:fileList('" + s + "');");
            }
        });
    }

    @android.webkit.JavascriptInterface
    public void saveResults(){
        wb.post(new Runnable() {
            public void run() {
                saveImage();
            }
        });
    }

    private void saveImage() {
        wb.measure(View.MeasureSpec.makeMeasureSpec(
                        View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        wb.layout(0, 0, wb.getMeasuredWidth(), wb.getMeasuredHeight());
        PdfDocument document = new PdfDocument();
        int pageNumber = 1;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(wb.getMeasuredWidth(),
                wb.getMeasuredHeight(), pageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        //wb.draw(page.getCanvas());
        wb.draw(page.getCanvas());
        document.finishPage(page);

        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();
        File myDir = new File(root + "/Positivimetro");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Resultado-"+ n +".pdf";
        showToast("Resultados guardados como:" + myDir + "/" + fname);
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            //FileOutputStream out = new FileOutputStream(file);
            //finalBitmap.compress(Bitmap.CompressFormat.PNG,100, out);
            FileOutputStream out = new FileOutputStream(file);
            document.writeTo(out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        MediaScannerConnection.scanFile(mContext, new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }
}
