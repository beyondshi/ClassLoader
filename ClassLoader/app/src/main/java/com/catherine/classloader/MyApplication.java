package com.catherine.classloader;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Stack;

/**
 * Created by Catherine on 2017/2/13.
 * yacatherine19@gmail.com
 */

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private Context cc;
    private Object j;
    private Stack<ClassLoader> CLs;
    private static Stack<Object> parents;
    private Smith<Object> sm;
    private Resources mResources;
    private String dexPath;
    private Resources.Theme mTheme;
    private AssetManager mAssetManager;
    private static boolean isopen = true;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        cc = base;
    }

    @Override
    public String getPackageName() {
        if (!isopen) {
            return "";
        } else {
            return getBaseContext().getPackageName();
        }
    }

    @Override
    public synchronized void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        this.isopen = true;
        CLs = new Stack<>();
        parents = new Stack<>();
        createPath();
    }


    @Override
    public void onConfigurationChanged(Configuration paramConfiguration) {
        super.onConfigurationChanged(paramConfiguration);
        if (this.j != null) {
            try {
                this.j.getClass().getDeclaredMethod("onConfigurationChanged", new Class[]{Configuration.class})
                        .invoke(this.j, new Object[]{paramConfiguration});
                return;
            } catch (Exception localException) {
                Log.e(TAG, "onLowMemory()" + localException);
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (this.j == null) {
            try {
                this.j.getClass().getDeclaredMethod("onLowMemory", new Class[0]).invoke(this.j, new Object[0]);
                return;
            } catch (Exception localException) {
                Log.w(TAG, "onLowMemory()" + localException);
            }
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (this.j != null) {
            try {
                this.j.getClass().getDeclaredMethod("onTerminate", new Class[0]).invoke(this.j, new Object[0]);
                return;
            } catch (Exception localException) {
                Log.e(TAG, "onTerminate()" + localException);
            }
        }
    }

    // Dynamically load apk
    public void LoadApk(String fileName) {
        Log.d(TAG, "LoadApk()");
        Log.d(TAG, "CLs:" + CLs.size());
        Log.d(TAG, "parents:" + parents.size());
        dexPath = new File(getFilesDir(), fileName).getAbsolutePath();

        ClassLoader defaultCL = getClass().getClassLoader();
        Log.d(TAG, "defaultCL:" + defaultCL.toString());
        Log.d(TAG, "parent:" + defaultCL.getParent().toString());
        sm = new Smith(defaultCL);
        if (CLs.size() == 0)
            CLs.push(defaultCL);

        try {
            Object defaultP = sm.get();
            if (parents.size() == 0)
                parents.push(defaultP);

            Object layerP = new MyDexClassLoader(this, dexPath, cc.getFilesDir().getAbsolutePath(),
                    (ClassLoader) sm.get()); //4:the parent class loader
            if (!parents.peek().equals(layerP)) {
                parents.push(layerP);
                CLs.push((ClassLoader) layerP);
            }
            sm.set(parents.peek());

            loadResources();
        } catch (Exception e) {
            Log.e(TAG, "onCreate catch " + e.toString());
        }
    }

    // Remove classLoader from loaded apk and recovery
    public void RemoveApk() {
        Log.d(TAG, "RemoveApk()");
        try {
            if (CLs.size() > 1) {
                CLs.pop();
                parents.pop();
            }
            Smith<Object> sm = new Smith<>(CLs.peek());
            sm.set(parents.peek());
        } catch (Exception e) {
            Log.e(TAG, "RemoveApk : " + e.toString());
            if (e instanceof IllegalArgumentException)
                return;
        }

        mTheme = null;
        mResources = null;
        mAssetManager = null;
    }

    // Load resources from apks
    protected void loadResources() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        Log.d(TAG, "MyApplication : loadResources()");
        AssetManager am = (AssetManager) AssetManager.class.newInstance();
        am.getClass().getMethod("addAssetPath", String.class).invoke(am, dexPath);
        mAssetManager = am;
        Constructor<?> constructor_Resources = Resources.class.getConstructor(AssetManager.class, cc.getResources()
                .getDisplayMetrics().getClass(), cc.getResources().getConfiguration().getClass());
        mResources = (Resources) constructor_Resources.newInstance(am, cc.getResources().getDisplayMetrics(), cc.getResources().getConfiguration());
        mTheme = mResources.newTheme();
        mTheme.applyStyle(android.R.style.Theme_Light_NoTitleBar_Fullscreen, true);
    }

    public void createPath() {
        // 从 assets 中拿出插件 apk 放到内部存储空间
        try {
            InputStream inputStream1 = getAssets().open("Resource1.apk");
            File file1 = new File(getFilesDir().getAbsolutePath(), "Resource1.apk");
            FileOutputStream fos = new FileOutputStream(file1);
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = inputStream1.read(buffer)) != -1) {// 循环从输入流读取
                // buffer字节
                fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
            }
            fos.flush();// 刷新缓冲区
            inputStream1.close();
            fos.close();

            InputStream inputStream2 = getAssets().open("Resource2.apk");
            File file2 = new File(getFilesDir().getAbsolutePath(), "Resource2.apk");
            FileOutputStream fos2 = new FileOutputStream(file2);
            byte[] buffer2 = new byte[1024];
            int byteCount2;
            while ((byteCount2 = inputStream2.read(buffer2)) != -1) {// 循环从输入流读取
                // buffer字节
                fos2.write(buffer2, 0, byteCount2);// 将读取的输入流写入到输出流
            }
            fos2.flush();// 刷新缓冲区
            inputStream2.close();
            fos2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        File file;
//        if (getExternalFilesDir(null) != null) {
//            file = getExternalFilesDir(null);
//        } else {
//            file = getFilesDir();
//        }
//
//        if (!file.exists()) {
//            file.mkdir();
//        }
    }

    @Override
    public ClassLoader getClassLoader() {
        if (CLs == null || CLs.size() == 0) {
            Log.d(TAG, "super.getClassLoader()");
            return super.getClassLoader();
        }
        Log.d(TAG, "It's in the " + CLs.size() + " layer");
        return CLs.peek();
    }

    @Override
    public AssetManager getAssets() {
        Log.e(TAG, mAssetManager == null ? "super.getAssets()" : "mAssetManager");
        return mAssetManager == null ? super.getAssets() : mAssetManager;
    }

    @Override
    public Resources getResources() {
        Log.e(TAG, mResources == null ? "super.getResources()" : "mResources");
        return mResources == null ? super.getResources() : mResources;

    }

    @Override
    public Resources.Theme getTheme() {
        Log.e(TAG, mTheme == null ? "super.getTheme()" : "mTheme");
        return mTheme == null ? super.getTheme() : mTheme;
    }
}