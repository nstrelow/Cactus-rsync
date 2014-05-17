package de.cactus.rsync;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class Settings extends PreferenceActivity implements
        OnPreferenceChangeListener {

    public static final String LOCAL_SYNCFOLDER_PATH_PREF = "pref_lcl_syncfolder_path";
    String lclSyncPath = prefs.getString(LOCAL_SYNCFOLDER_PATH_PREF,
            "cactussync");
    public static final String SERVER_SYNCFOLDER_PATH_PREF = "pref_server_syncfolder_path";
    String serverSyncPath = prefs.getString(SERVER_SYNCFOLDER_PATH_PREF,
            "/path/to/server/syncdir");
    public static final String USERNAME_PREF = "pref_username";
    String username = prefs.getString(USERNAME_PREF, "user");
    public static final String SERVER_IP_PREF = "pref_server_ip";
    String serverIP = prefs.getString(SERVER_IP_PREF, "xx.xx.xx.xx");
    public static final String SSH_KEY_PATH_PREF = "pref_ssh_key_path";
    String sshKeyPath = prefs.getString(SSH_KEY_PATH_PREF, "/path/to/sshkey");
    public static final String HAS_PREF_CHANGED_EXTRA = "has_pref_changed";
    SharedPreferences prefs = PreferenceManager
            .getDefaultSharedPreferences(RsyncInterface.context);
    private EditTextPreference mLocalSyncfolderPathEdPref;
    private EditTextPreference mServerSyncfolderPathEdPref;
    private EditTextPreference mUsernameEdPref;
    private EditTextPreference mServerIPEdPref;
    private EditTextPreference mSSHKeyEdPref;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        final PreferenceScreen prefSet = getPreferenceScreen();

        mLocalSyncfolderPathEdPref = (EditTextPreference) prefSet
                .findPreference(LOCAL_SYNCFOLDER_PATH_PREF);
        mServerSyncfolderPathEdPref = (EditTextPreference) prefSet
                .findPreference(SERVER_SYNCFOLDER_PATH_PREF);
        mUsernameEdPref = (EditTextPreference) prefSet
                .findPreference(USERNAME_PREF);
        mServerIPEdPref = (EditTextPreference) prefSet
                .findPreference(SERVER_IP_PREF);
        mSSHKeyEdPref = (EditTextPreference) prefSet
                .findPreference(SSH_KEY_PATH_PREF);

        mLocalSyncfolderPathEdPref.setSummary(lclSyncPath);
        mServerSyncfolderPathEdPref.setSummary(serverSyncPath);
        mUsernameEdPref.setSummary(username);
        mServerIPEdPref.setSummary(serverIP);
        mSSHKeyEdPref.setSummary(sshKeyPath);

        mLocalSyncfolderPathEdPref.setOnPreferenceChangeListener(this);
        mServerSyncfolderPathEdPref.setOnPreferenceChangeListener(this);
        mUsernameEdPref.setOnPreferenceChangeListener(this);
        mServerIPEdPref.setOnPreferenceChangeListener(this);
        mServerIPEdPref.setOnPreferenceChangeListener(this);


    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLocalSyncfolderPathEdPref) {

            String val = newValue.toString();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(LOCAL_SYNCFOLDER_PATH_PREF, val);
            editor.commit();
            mLocalSyncfolderPathEdPref.setText(val);

            mLocalSyncfolderPathEdPref.setSummary(prefs.getString(
                    LOCAL_SYNCFOLDER_PATH_PREF, "cactussync"));

        }

        if (preference == mServerSyncfolderPathEdPref) {

            String val = newValue.toString();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(SERVER_SYNCFOLDER_PATH_PREF, val);
            editor.commit();
            mServerSyncfolderPathEdPref.setText(val);

            mServerSyncfolderPathEdPref.setSummary(prefs.getString(
                    SERVER_SYNCFOLDER_PATH_PREF, "/path/to/server/syncdir"));

        }

        if (preference == mUsernameEdPref) {

            String val = newValue.toString();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(USERNAME_PREF, val);
            editor.commit();
            mUsernameEdPref.setText(val);

            mUsernameEdPref.setSummary(prefs.getString(USERNAME_PREF, "user"));

        }

        if (preference == mServerIPEdPref) {

            String val = newValue.toString();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(SERVER_IP_PREF, val);
            editor.commit();
            mServerIPEdPref.setText(val);

            mServerIPEdPref.setSummary(prefs.getString(SERVER_IP_PREF,
                    "xx.xx.xx.xx"));
        }

        if (preference == mSSHKeyEdPref) {

            String val = newValue.toString();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(SSH_KEY_PATH_PREF, val);
            editor.commit();
            mSSHKeyEdPref.setText(val);

            mSSHKeyEdPref.setSummary(prefs.getString(SSH_KEY_PATH_PREF,
                    "/path/to/sshkey"));
        }

        Intent data = new Intent();
        data.putExtra(HAS_PREF_CHANGED_EXTRA, true);
        setResult(Activity.RESULT_OK, data);

        return false;
    }
}