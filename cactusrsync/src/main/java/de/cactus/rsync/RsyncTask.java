package de.cactus.rsync;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class RsyncTask extends AsyncTask<String, String, Boolean> {

    public static String RSYNC_TASK_LOG = "RSYNCTASK";
    //private ProgressDialog progressDialog;
    private int rsyncTaskType;

    @Override
    protected void onPreExecute() {

        //progressDialog = ProgressDialog.show(RsyncInterface.context,
        //       "Computing...", "Syncing File", true);
        RsyncInterface.handler.sendEmptyMessage(RsyncInterface.SYNC_STARTED);
        Log.e(RSYNC_TASK_LOG, getStatus().toString());
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(String... command) {

        rsyncTaskType = Integer.parseInt(command[1]);

        Process process = null;
        try {
            process = new ProcessBuilder().directory(RsyncInterface.dir)
                    .command("sh").redirectErrorStream(true).start();

        } catch (IOException e) {
            e.printStackTrace();
        }

        OutputStream os = process.getOutputStream();
        try {
            os.write(command[0].getBytes());
            os.flush();

            String line;
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));

            while ((line = input.readLine()) != null) {
                Log.i(RsyncInterface.LOG_TAG, line);
                publishProgress(line);
                Log.e(RSYNC_TASK_LOG, getStatus().toString());
                if (line.contains("speedup")) {
                    break;
                }
            }

            input.close();
            process.destroy();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        RsyncInterface.writeLog("\n\n");
        Log.e(RSYNC_TASK_LOG, getStatus().toString());

        RsyncInterface.handler.sendEmptyMessage(RsyncInterface.SYNC_FINISHED);

		/*
         * writing new syncFileList:
		 * o after delete(before old file is needed to detect removed files)
		 * o after the files got updated by the server
		 * files for the next sync
		 */
        switch (rsyncTaskType) {
            case RsyncInterface.RSYNC_ALL_TO_DEVICE_TASK:
                RsyncInterface.writeSyncFilesList();
                RsyncInterface.writeLog("Sync finished.\n");
                break;

            case RsyncInterface.RSYNC_DELETE_FROM_SERVER_TASK:

                RsyncInterface.writeSyncFilesList();

                // delete temp file
                RsyncInterface.tempSyncFilesRelPaths.delete();

                break;
        }
        //Dialog.dismiss();
        super.onPostExecute(result);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        RsyncInterface.writeLog(values[0]);
        super.onProgressUpdate(values);
    }

}
