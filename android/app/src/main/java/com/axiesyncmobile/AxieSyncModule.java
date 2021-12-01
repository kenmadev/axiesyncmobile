package com.axiesyncmobile;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

public class AxieSyncModule extends ReactContextBaseJavaModule {

    private ReactContext mReactContext;
    ActivityEventListener activityEventListener;
    private int REQUEST_CODE = 27867;
    private int DOCUMENT_REQUEST_CODE = 27868;
    private int REQUEST_ACTION_OPEN_DOCUMENT_TREE = 27869;
    private String TAG = "AXIETAG";

    private static final String PATH_TREE = "tree";
    private static final String PRIMARY_TYPE = "primary";
    private static final String RAW_TYPE = "raw";

    AxieSyncModule(ReactApplicationContext context) {
        super(context);
        mReactContext = context;
    }

    @Override
    public String getName() {
        return "AxieSyncModule";
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @ReactMethod
    public void openDocumentTree(String initialPath, boolean persist, Promise promise) {
        Intent intent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            StorageManager sm = (StorageManager) mReactContext.getSystemService(Context.STORAGE_SERVICE);
            intent = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
            String startDir = initialPath == null ? "Android/data" : initialPath;
            Uri initialUri = intent.getParcelableExtra(DocumentsContract.EXTRA_INITIAL_URI);
            String scheme = initialUri.toString();
            Log.d(TAG, "INITIAL_URI scheme: " + scheme);
            scheme = scheme.replace("/root/", "/document/");
            startDir = startDir.replace("/", "%2F");
            scheme += "%3A" + startDir;
            Uri uri = Uri.parse(scheme);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
            Log.d(TAG, "FINAL URI: " + uri.toString());
        } else {
            // lower than android 11
            intent = new Intent();
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT_TREE);
            String EXTERNAL_STORAGE_PROVIDER_AUTHORITY = "com.android.externalised.documents";
            Uri uri = DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_PROVIDER_AUTHORITY, "primary:" + initialPath);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
        }

        if (activityEventListener != null) {
            mReactContext.removeActivityEventListener(activityEventListener);
            activityEventListener = null;
        }

        activityEventListener = new ActivityEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
                if (requestCode == REQUEST_ACTION_OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Uri uri = data.getData();
                        Log.d(TAG, "SELECTED URI: " + uri);
                        if (persist) {
                            final int takeFlags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            mReactContext.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        }

                        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
                        Log.d(TAG, "children: " + children);
                        try (Cursor c = mReactContext.getContentResolver().query(children, null, null, null, null)) {
                            if (c != null) {
                                Log.d(TAG, "count:" + c.getCount());
                                while (c.moveToNext()) {
                                    String documentId = c.getString(0);
                                    Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId);
                                    Log.d(TAG, "documentUri=" + documentUri);
                                    Log.d(TAG, "  documentId=" + documentId);
                                    Log.d(TAG, "  isTreeUri(" + documentUri + ")=" + DocumentsContract.isTreeUri(documentUri));
                                    Log.d(TAG, "  isDocumentUri(this, " + documentUri + ")=" + DocumentsContract.isDocumentUri(mReactContext, documentUri));
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Exception: " + e.getMessage());
                        }

                        DocumentFile dir = DocumentFile.fromTreeUri(mReactContext, uri);
                        WritableMap params = Arguments.createMap();
                        params.putString("uri", uri.toString());
                        params.putString("name", dir.getName());
                        params.putString("path", getDirectoryPathFromUri(mReactContext, uri));
                        params.putString("type", dir.isDirectory() ? "directory" : "file");
                        params.putDouble("lastModified", dir.lastModified());
                        Log.d(TAG, "PARAMS: " + params.toString());
                        promise.resolve(params);
                    } else {
                        promise.resolve(null);
                    }
                } else {
                    promise.resolve(null);
                }
                mReactContext.removeActivityEventListener(activityEventListener);
                activityEventListener = null;
            }

            @Override
            public void onNewIntent(Intent intent) {

            }
        };

        mReactContext.addActivityEventListener(activityEventListener);
        mReactContext.getCurrentActivity().startActivityForResult(intent, REQUEST_ACTION_OPEN_DOCUMENT_TREE);

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @ReactMethod
    public void stat(String path, final Promise promise) {
        try {
            boolean hasPermission = hasPermission(path);
            if (!hasPermission) {
                promise.reject("ENOENT", "'" + path + "'does not have permission to read/write");
                return;
            }

            DocumentFile dir = DocumentFile.fromSingleUri(mReactContext, Uri.parse(path));

            WritableMap fileMap = Arguments.createMap();
            fileMap.putString("uri", dir.getUri().toString());
            fileMap.putString("name", dir.getName());
            fileMap.putString("type", dir.isDirectory() ? "directory" : "file");
            if (dir.isFile()) {
                fileMap.putString("mime", dir.getType());
            }
            fileMap.putDouble("lastModified", dir.lastModified());
            promise.resolve(fileMap);

        } catch (Exception e) {
            promise.reject("EUNSPECIFIED", e.getLocalizedMessage());
        }
    }

    @ReactMethod
    public void getDocumentUriFromTree(String uriTree, String documentId, Promise promise) {
        try {
            Log.d(TAG, "URI PREP:" + uriTree);
            Uri uri = Uri.parse(uriTree);
            Log.d(TAG, "URI PARSED:" + uri);
            Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId);
            promise.resolve(documentUri.toString());
        } catch (Exception e) {
            promise.reject("ENOENT", "Unable to build document uri using " +
                    uriTree + " for " + documentId + " err:" + e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean hasPermission(String string) {
        // list of all persisted permissions for our app
        List<UriPermission> uriList = mReactContext.getContentResolver().getPersistedUriPermissions();

        for (UriPermission uriPermission : uriList) {
            String uriString = uriPermission.getUri().toString();

            if ((uriString.startsWith(string) || string.startsWith(uriString)) && uriPermission.isReadPermission() && uriPermission.isWritePermission()) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @ReactMethod
    public void getPersistedUriPermissions(Promise promise) {

        List<UriPermission> uriList = mReactContext.getContentResolver().getPersistedUriPermissions();

        WritableArray array = Arguments.createArray();
        if (uriList.size() != 0) {
            for (UriPermission uriPermission : uriList) {
                array.pushString(uriPermission.getUri().toString());
            }
        }
        promise.resolve(array);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @ReactMethod
    public void releasePersistableUriPermission(String uri) {

        Uri uriToRevoke = Uri.parse(uri);

        final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        mReactContext.getContentResolver().releasePersistableUriPermission(uriToRevoke, takeFlags);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @ReactMethod
    public void readFile(String path, String encoding, final Promise promise) {
        try {
            boolean withPermission = hasPermission(path);
            if (!withPermission) {
                promise.reject("ENOENT", "'" + path + "'does not have permission to read/write");
                return;
            }
            DocumentFile dir = DocumentFile.fromTreeUri(mReactContext, Uri.parse(path));

            if (!dir.isFile()) {
                promise.reject("ENOENT", "'" + path + "'is not a file");
                return;
            }
            Uri uri = Uri.parse(path);

            if (encoding != null) {
                if (encoding == "ascii") {
                    WritableArray arr = (WritableArray) readFromUri(uri, encoding);
                    promise.resolve((arr));
                } else {
                    promise.resolve((String) readFromUri(uri, encoding));
                }
            } else {
                promise.resolve((String) readFromUri(uri, "utf8"));
            }
        } catch (Exception e) {
            promise.reject("EUNSPECIFIED", e.getLocalizedMessage());
        }
    }

    private Object readFromUri(Uri uri, String encoding) throws IOException {
        byte[] bytes;
        int bytesRead;
        int length;

        InputStream inputStream =
                mReactContext.getContentResolver().openInputStream(uri);

        length = inputStream.available();
        bytes = new byte[length];
        bytesRead = inputStream.read(bytes);
        inputStream.close();

        switch (encoding.toLowerCase()) {
            case "base64":
                return Base64.encodeToString(bytes, Base64.NO_WRAP);
            case "ascii":
                WritableArray asciiResult = Arguments.createArray();
                for (byte b : bytes) {
                    asciiResult.pushInt((int) b);
                }
                return asciiResult;
            case "utf8":
            default:
                return new String(bytes);
        }


    }

    public static String getDirectoryPathFromUri(Context context, Uri uri) {

        if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && isTreeUri(uri)) {

            String treeId = getTreeDocumentId(uri);

            if (treeId != null) {


                String[] paths = treeId.split(":");
                String type = paths[0];
                String subPath = paths.length == 2 ? paths[1] : "";

                if (RAW_TYPE.equalsIgnoreCase(type)) {
                    return treeId.substring(treeId.indexOf(File.separator));
                } else if (PRIMARY_TYPE.equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + File.separator + subPath;
                } else {
                    StringBuilder path = new StringBuilder();
                    String[] pathSegment = treeId.split(":");
                    if (pathSegment.length == 1) {
                        path.append(getRemovableStorageRootPath(context, paths[0]));
                    } else {
                        String rootPath = getRemovableStorageRootPath(context, paths[0]);
                        path.append(rootPath).append(File.separator).append(pathSegment[1]);
                    }

                    return path.toString();
                }
            }
        }
        return null;
    }

    public static String getTreeDocumentId(Uri uri) {
        final List<String> paths = uri.getPathSegments();
        if (paths.size() >= 2 && PATH_TREE.equals(paths.get(0))) {
            return paths.get(1);
        }
        return null;
    }

    public static boolean isTreeUri(Uri uri) {
        final List<String> paths = uri.getPathSegments();
        return (paths.size() == 2 && PATH_TREE.equals(paths.get(0)));
    }

    private static String getRemovableStorageRootPath(Context context, String storageId) {
        StringBuilder rootPath = new StringBuilder();
        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null);
        for (File fileDir : externalFilesDirs) {
            if (fileDir.getPath().contains(storageId)) {
                String[] pathSegment = fileDir.getPath().split(File.separator);
                for (String segment : pathSegment) {
                    if (segment.equals(storageId)) {
                        rootPath.append(storageId);
                        break;
                    }
                    rootPath.append(segment).append(File.separator);
                }
                break;
            }
        }
        return rootPath.toString();
    }

}
