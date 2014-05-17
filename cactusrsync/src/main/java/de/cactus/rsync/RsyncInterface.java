package de.cactus.rsync;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RsyncInterface extends Activity {

    public static final String LOG_TAG = "CactusRsync";
    // consts to detect : "Which RsyncTask am I ?"
    public static final int RSYNC_ADD_TO_SERVER_TASK = 0;
    public static final int RSYNC_DELETE_FROM_SERVER_TASK = 1;
    public static final int RSYNC_CHANGES_TO_SERVER_TASK = 2;
    public static final int RSYNC_ALL_TO_DEVICE_TASK = 3;
    public static final int SYNC_STARTED = 0;
    public static final int SYNC_FINISHED = 1;
    public static TextView rsyncLogOutput;
    public static Handler handler;
    public static int settingsRequestCode = 17587;
    public static ScrollView scrollView;
    public static File tempSyncFilesRelPaths;
    public static Context context;
    static File dir = Environment.getExternalStorageDirectory();
    static String Paths = "";
    private static File SYNC_DIR;
    private static String syncFilesListDir;
    private static File syncFilesListFile;
    private final String ID_RSA = "id";
    String newFiles = "";
    private Menu optionsMenu;
    private String BIN_DIR = "";
    private String RSYNC_BIN = "rsync";
    private String RSYNCD_BIN;
    private int RSYNC_RAW_ID;
    private String ID_RSA_PATH = "";
    private String ID_RSA_DIR;
    private String SSH_BIN = "ssh";
    private String SSH_BIN_DIR;
    private int SSH_RAW_ID;
    private File tempSSHKey;
    private String remoteUser;
    private String remoteIP;
    private String remoteUserAtIP;
    private String remoteDir;
    private boolean isLoading = false;

    static void writeSyncFilesList() {
        Paths = "";
        String syncFilesList = listPath(SYNC_DIR);

        Log.i("syncFilesList", syncFilesList);

        try {
            FileWriter writeNames = new FileWriter(syncFilesListFile);
            BufferedWriter out = new BufferedWriter(writeNames);
            out.write(syncFilesList);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String sSdLog = syncFilesList;
        File sdLog = new File(dir, "sdLog");

        try {
            FileWriter writeNames = new FileWriter(sdLog);
            BufferedWriter out = new BufferedWriter(writeNames);
            out.write(sSdLog);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String listPath(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                Paths += fileEntry.getAbsolutePath() + "\n";
                listPath(fileEntry);
            } else {
                // System.out.println(fileEntry.getName());
                Paths += fileEntry.getAbsolutePath() + "\n";
            }
        }
        return Paths;
    }

    public static boolean InternetAvailable(Context context) {

        NetworkInfo info = (NetworkInfo) ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo();

        if (info == null || !info.isConnected()) {
            return false;
        }
        if (info.isRoaming()) {

            return false;
        }
        return true;
    }

    /**
     * Loads a file to String
     *
     * @param file
     * @return
     * @throws IOException
     */
    private static String loadFileToString(File file) throws IOException {
        StringBuffer content = new StringBuffer();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(file));
            String s = null;

            while ((s = reader.readLine()) != null) {
                content.append(s).append(System.getProperty("line.separator"));
            }
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                throw e;
            }
        }
        return content.toString();
    }

    public static void writeLog(String line) {
        rsyncLogOutput.setText(rsyncLogOutput.getText() + line + "\n");
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private static void copyfile(String srFile, String dtFile) {
        try {
            File f1 = new File(srFile);
            File f2 = new File(dtFile);
            InputStream in;

            in = new FileInputStream(f1);

            // For Append the file.
            // OutputStream out = new FileOutputStream(f2,true);

            // For Overwrite the file.
            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();

            Runtime.getRuntime().exec("chmod 600 " + f2);

            Log.i(LOG_TAG, "SSH key copied");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rsync_interface);

        loadPrefs();

        syncFilesListDir = sync_files_list_path();

        syncFilesListFile = new File(syncFilesListDir + SYNC_DIR.getName());

        if (!SYNC_DIR.exists()) {
            SYNC_DIR.mkdir();
            if (syncFilesListFile.exists()) {
                syncFilesListFile.delete();
            }
        }

        handler = new Handler() {
            // Create handleMessage function
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SYNC_STARTED:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            invalidateOptionsMenu();
                        }
                        isLoading = true;
                        setRefreshActionButtonState(isLoading);
                        break;
                    case SYNC_FINISHED:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            invalidateOptionsMenu();
                        }
                        isLoading = false;
                        setRefreshActionButtonState(isLoading);
                        break;
                }
            }
        };

        BIN_DIR = bin_path();

        RSYNCD_BIN = BIN_DIR + RSYNC_BIN;
        SSH_BIN_DIR = BIN_DIR + SSH_BIN;
        SSH_RAW_ID = R.raw.ssh;
        RSYNC_RAW_ID = R.raw.rsync;

        if (!new File(RSYNCD_BIN).exists()) {
            new File(RSYNCD_BIN).delete();
            installFile(RSYNCD_BIN, RSYNC_RAW_ID);
        }

        if (!new File(SSH_BIN_DIR).exists()) {

            new File(SSH_BIN_DIR).delete();
            installFile(SSH_BIN_DIR, SSH_RAW_ID);
        }

		/*
         * Initialize TextView for Rsync Output
		 */
        rsyncLogOutput = (TextView) findViewById(R.id.rsync_log);

        scrollView = (ScrollView) findViewById(R.id.scroll);

        context = this;

        tempSyncFilesRelPaths = new File(context.getCacheDir(), "tempRelPaths");

        tempSSHKey = new File(context.getCacheDir(), "key");

    }

    @Override
    protected void onStop() {
        new File(ID_RSA_DIR).delete();
        super.onStop();
    }

    private void loadPrefs() {

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        String lclSyncPath = prefs.getString(
                Settings.LOCAL_SYNCFOLDER_PATH_PREF, "cactussync");

        String serverSyncPath = prefs
                .getString(Settings.SERVER_SYNCFOLDER_PATH_PREF,
                        "/path/to/server/syncdir");

        String username = prefs.getString(Settings.USERNAME_PREF, "user");

        String serverIP = prefs.getString(Settings.SERVER_IP_PREF,
                "xx.xx.xx.xx");

        String sshKeyOnSDPath = Environment.getExternalStorageDirectory() + "/" + prefs.getString(Settings.SSH_KEY_PATH_PREF,
                "/path/to/sshkey");

        ID_RSA_PATH = id_rsa_path();
        ID_RSA_DIR = ID_RSA_PATH + ID_RSA;

        File sshKeyOnSD = new File(sshKeyOnSDPath);
        if (sshKeyOnSD.exists()) {
            copyfile(sshKeyOnSDPath, ID_RSA_DIR);
        }

        remoteDir = serverSyncPath;

        SYNC_DIR = new File(dir, "/" + lclSyncPath);

        remoteUser = username;

        remoteIP = serverIP;

        remoteUserAtIP = remoteUser + "@" + remoteIP;

    }

    public void start() {
        if (InternetAvailable(this)) {

            if (syncFilesListFile.exists()) {

                // files is the name String from syncFilesList
                String files[] = loadSyncFilesList().split("\n");

                checkForNewFiles(SYNC_DIR, files);

				/*
                 * delete files on server with not modified snycFilesListFile
				 * entries,no syncing of files just deleting
				 */
                deleteFromServer();

                // sync changed files from android to server
                writeLog("Updating changed files from Device to Server...\n");
                String[] rsyncToServer = {
                        RSYNCD_BIN + " -rvtlz --stats --progress --checksum"
                                + " --update --ignore-non-existing -e \""
                                + SSH_BIN_DIR + " -i " + ID_RSA_DIR + " -y\" "
                                + SYNC_DIR + "/ " + remoteUserAtIP + ":"
                                + remoteDir + "\n",
                        String.valueOf(RSYNC_CHANGES_TO_SERVER_TASK)};

                startNewSyncTask(rsyncToServer);

            } else {
                writeLog("Initializing directory for the first time...\n");
            }

            // sync + delete changed & new files from server to android
            writeLog("Deleting removed files, adding new files and updating changed files from Server to Device...\n");
            String[] rsyncToAndroid = {
                    RSYNCD_BIN
                            + " -rvlz --stats --progress --checksum --update --delete -e \""
                            + SSH_BIN_DIR + " -i " + ID_RSA_DIR + " -y\" "
                            + remoteUserAtIP + ":" + remoteDir + " " + SYNC_DIR
                            + "\n", String.valueOf(RSYNC_ALL_TO_DEVICE_TASK)};

            startNewSyncTask(rsyncToAndroid);
        } else {
            Toast.makeText(this,
                    getResources().getString(R.string.toast_no_internet),
                    Toast.LENGTH_LONG).show();
        }
    }

    public void onClickStart(View v) {
        start();

    }

    public void clearLogClick(View v) {

        rsyncLogOutput.setText("");
    }

    private void checkForNewFiles(File dirToSync, String[] syncFilesList) {

        Boolean isFileThere = false;

        File[] localSyncFiles = dirToSync.listFiles();

        // i for
        for (int i = 0; i < localSyncFiles.length; i++) {

            // k for
            for (int k = 0; k < syncFilesList.length; k++) {

                String locPath = localSyncFiles[i].getAbsolutePath();
                if (locPath.equals(syncFilesList[k])) {

                    isFileThere = true;
                    break;
                }

            } // k round finished

            if (localSyncFiles[i].isDirectory()) {

                if (!isFileThere) {
                    // if the dir is emtpy and not yet synced, so sync it
                    syncNewFiles(
                            localSyncFiles[i].getAbsolutePath(),
                            dirToSync.getAbsolutePath().replace(
                                    SYNC_DIR.getAbsolutePath(), "")
                    );
                } else {

                    int len = localSyncFiles[i].list().length;
                    if (len > 0) {

                        if (newFiles != "") {
                            syncNewFiles(newFiles, dirToSync.getAbsolutePath()
                                    .replace(SYNC_DIR.getAbsolutePath(), ""));
                            newFiles = "";
                        }

                        // check for new files in the next directory
                        checkForNewFiles(localSyncFiles[i].getAbsoluteFile(),
                                syncFilesList);
                    }

                    // file is there, setting isFileThere to false for the
                    // next
                    // i round
                    isFileThere = false;
                }

            } else {
                if (!isFileThere) {
                    newFiles += localSyncFiles[i].getAbsolutePath()/*
                                                                     * .replace(
																	 * dir.
																	 * getAbsolutePath
																	 * () + "/",
																	 * "")
																	 */ + " ";
                } else {
                    // file is there, setting isFileThere to false for the next
                    // i round
                    isFileThere = false;
                }
            }

        } // i round finished

        // sync all new Files to the Server, if newFiles != null
        if (newFiles != "") {
            syncNewFiles(
                    newFiles,
                    dirToSync.getAbsolutePath().replace(
                            SYNC_DIR.getAbsolutePath(), "")
            );
            newFiles = "";
        }
    }

    private void startNewSyncTask(String[] command) {

        new RsyncTask().execute(command);
    }

    public void syncNewFiles(String newFilesToSync, String serverDir) {
        writeLog("Found new Files. Adding to Server...\n");
        String[] rsyncAddToServer = {
                RSYNCD_BIN
                        + " -rvtlpz --stats"
                        + "--ignore-existing --progress --chmod=ug=rwx --chmod=o=rx -e \""
                        + SSH_BIN_DIR + " -i " + ID_RSA_DIR + " -y\" "
                        + newFilesToSync + " " + remoteUserAtIP + ":"
                        + remoteDir + serverDir + "\n",
                String.valueOf(RSYNC_ADD_TO_SERVER_TASK)};

        startNewSyncTask(rsyncAddToServer);
    }

    private void deleteFromServer() {

        // just deleting files, that are no longer in SYNC_DIR, but still in
        // syncFilesList
        String[] syncFilesAbsPaths = null;
        try {
            syncFilesAbsPaths = loadFileToString(syncFilesListFile).split("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String syncFilesRelPaths = "";
        for (int i = 0; i < syncFilesAbsPaths.length; i++) {
            syncFilesRelPaths += syncFilesAbsPaths[i].replace(
                    SYNC_DIR.getAbsolutePath() + "/", "")
                    + "\n";
        }

        Log.i("syncFilesRelPaths", syncFilesRelPaths);

        try {
            FileWriter writeNames = new FileWriter(tempSyncFilesRelPaths);
            BufferedWriter out = new BufferedWriter(writeNames);
            out.write(syncFilesRelPaths);
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        writeLog("Checking for deleted files...\n");
        String[] deleteFromServer = {
                RSYNCD_BIN
                        + " -rvlz --stats --progress --existing --ignore-existing --delete"
                        + " --include-from="
                        + tempSyncFilesRelPaths.getAbsolutePath()
                        + " --exclude=* -e \"" + SSH_BIN_DIR + " -i "
                        + ID_RSA_DIR + " -y\" " + SYNC_DIR + "/ "
                        + remoteUserAtIP + ":" + remoteDir + "\n",
                String.valueOf(RSYNC_DELETE_FROM_SERVER_TASK)};
        startNewSyncTask(deleteFromServer);
    }

    public String loadSyncFilesList() {

        String names = null;
        try {
            names = loadFileToString(syncFilesListFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return names;

    }

    private String bin_path() {
        File directory = getDir("bin", Context.MODE_PRIVATE);
        String rsync_abspath = directory + "/";
        return rsync_abspath;
    }

    private String id_rsa_path() {
        File directory = getDir("ssh", Context.MODE_PRIVATE);
        String id_rsa_abspath = directory + "/";
        return id_rsa_abspath;
    }

    private String sync_files_list_path() {
        File directory = getDir("sync", Context.MODE_PRIVATE);
        String sync_files_list_abspath = directory + "/";
        return sync_files_list_abspath;
    }

    /*
     * installs file from an rawid to data directory
     */
    private void installFile(String ff_file, int rawid) {

        Log.d(LOG_TAG, "installFile() **file_abspath=" + ff_file + "**");

        if (!new File(ff_file).exists()) {
            InputStream fileRaw;
            Log.d(LOG_TAG, "installFile() **no exists, copy...**");
            try {
                fileRaw = getResources().openRawResource(rawid);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(LOG_TAG, "installFile() **Exception**");
                return;
            } finally {
                Log.d(LOG_TAG, "installFile() ** OPENED file_abspath="
                        + ff_file + "**");
            }
            BufferedOutputStream fOut = null;
            try {
                fOut = new BufferedOutputStream(new FileOutputStream(ff_file));
                byte[] buffer = new byte[32 * 1024];
                int bytesRead = 0;
                while ((bytesRead = fileRaw.read(buffer)) != -1) {
                    fOut.write(buffer, 0, bytesRead);
                }
                Log.d(LOG_TAG, "installFile() **no exists, copy done**");
                switch (rawid) {
                    case R.raw.rsync:
                    case R.raw.ssh:
                        Runtime.getRuntime().exec("chmod 777 " + ff_file);
                        break;

                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(LOG_TAG, "installFile() **Exception**");
            } finally {
                try {
                    fileRaw.close();
                    fOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(LOG_TAG, "installFile() **Exception**");
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == settingsRequestCode
                && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(Settings.HAS_PREF_CHANGED_EXTRA, false)) {
                restartInterface();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void restartInterface() {

        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();

        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_rsync_interface, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_settings:
                Intent intent = new Intent(this, Settings.class);
                startActivityForResult(intent, settingsRequestCode);
                break;
            case R.id.action_refresh:
                start();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.optionsMenu == null)
            this.optionsMenu = menu;
        setRefreshActionButtonState(isLoading);
        return super.onPrepareOptionsMenu(menu);
    }

    public void setRefreshActionButtonState(final boolean refreshing) {
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu.findItem(R.id.action_refresh);
            if (refreshItem != null) {
                if (refreshing) {
                    if (MenuItemCompat.getActionView(refreshItem) == null) {
                        MenuItemCompat.setActionView(refreshItem, R.layout.actionbar_indeterminate_progress);
                    }
                } else {
                    MenuItemCompat.setActionView(refreshItem, null);
                }
            }
        }
    }

}