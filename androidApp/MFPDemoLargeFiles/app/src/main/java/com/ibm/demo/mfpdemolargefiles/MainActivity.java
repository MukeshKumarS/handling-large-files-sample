package com.ibm.demo.mfpdemolargefiles;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.worklight.common.Logger;
import com.worklight.wlclient.api.WLAccessTokenListener;
import com.worklight.wlclient.api.WLAuthorizationManager;
import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLLogoutResponseListener;
import com.worklight.wlclient.api.challengehandler.SecurityCheckChallengeHandler;
import com.worklight.wlclient.auth.AccessToken;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {

    private final static String LARGE_FILE_NAME = "big_buck_bunny.mp4";
    private final static String DOWNLOAD_RESOURCE_PATH = "/adapters/MFPDemoLargeFilesAdapter/resource/download";
    private final static String UPLOAD_RESOURCE_PATH = "/adapters/MFPDemoLargeFilesAdapter/resource/uploader?fileName=" + LARGE_FILE_NAME;
    private final static int BUFFER_SIZE = 8192;
    Logger logger = Logger.getInstance("MFPDemoLargeFiles");

    private String securityCheck = "UserLogin";
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init WLClient
        WLClient.createInstance(this);
        // Register ChallengeHandler
        WLClient.getInstance().registerChallengeHandler( new SecurityCheckChallengeHandler(securityCheck) {
            @Override
            public void handleChallenge(JSONObject jsonObject) {
                logger.debug("Challenged " + jsonObject);
                JSONObject credentials = new JSONObject();
                try {
                    credentials.put("username", "test");
                    credentials.put("password", "test");
                    submitChallengeAnswer(credentials);
                } catch (JSONException e) {
                    logger.error("error", e);
                }
            }

            @Override
            public void handleSuccess(JSONObject success){
                super.handleSuccess(success);
                logger.debug("Challenge handleSuccess " + success);
            }

            @Override
            public void handleFailure(JSONObject failure){
                super.handleFailure(failure);
                logger.error("Challenge handleFailure " + failure);
            }
        });

        // Set Logger ctx
        Logger.setContext(this);
        Logger.setLevel(Logger.LEVEL.DEBUG);

        final Button buttonDownload = (Button) findViewById(R.id.downloadBtn);
        buttonDownload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    logger.debug("Downloading large file from: " + DOWNLOAD_RESOURCE_PATH);
                    new FileDownloaderTask().execute(getConnection(DOWNLOAD_RESOURCE_PATH));
                } catch (Exception e) {
                    logger.error("error", e);
                }
            }
        });

        final Button buttonUpload = (Button) findViewById(R.id.uploadBtn);
        buttonUpload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    logger.debug("Uploading large file to: " + UPLOAD_RESOURCE_PATH);
                    new FileUploaderTask().execute(getConnection(UPLOAD_RESOURCE_PATH));
                } catch (Exception e) {
                    logger.error("error", e);
                }
            }
        });


        final Button buttonLogout = (Button) findViewById(R.id.logoutBtn);
        buttonLogout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                WLAuthorizationManager.getInstance().logout(securityCheck, new WLLogoutResponseListener() {
                    @Override
                    public void onSuccess() {
                        logger.log("Logout: onSuccess");
                    }

                    @Override
                    public void onFailure(WLFailResponse wlFailResponse) {
                        logger.error("Logout: error " + wlFailResponse.getErrorMsg());
                    }
                });
            }
        });
    }

    private URLConnection getConnection(String resourcePath) throws IOException {
        URLConnection conn = new URL(WLClient.getInstance().getServerUrl() + resourcePath).openConnection();
        // Add cached authorization token
        if(authToken != null) conn.addRequestProperty("Authorization", authToken);
        return (conn);
    }

    private void downloadFile(HttpURLConnection connection) throws IOException {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = connection.getInputStream(); // Input stream of the file to download
            byte[] data = new byte[BUFFER_SIZE];
            deleteFile(LARGE_FILE_NAME); // Delete old file
            outputStream = openFileOutput(LARGE_FILE_NAME, Context.MODE_PRIVATE); // Open stream for writing file
            int read, total = 0;
            while ((read = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, read);
                total += read;
            }
            logger.log("total bytes read: " + total);

        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * AsyncTask for downloading large file from MFP server
     */
    class FileDownloaderTask extends AsyncTask<URLConnection, Void, String[]> {

        @Override
        protected String[] doInBackground(URLConnection... conn) {
            try {
                // Check response status
                HttpURLConnection connection = (HttpURLConnection)conn[0];
                int status = connection.getResponseCode();
                if (status == 200) {
                    downloadFile(connection);
                    return (new String[]{"OK", connection.getResponseMessage()});
                }

                // Check if MFP OAuth authorization is required
                if (WLAuthorizationManager.getInstance().isAuthorizationRequired(connection)) {
                    logger.debug("Authorization required!");
                    return (new String[]{"authorizationRequired", WLAuthorizationManager.getInstance().getResourceScope(connection)});
                } else {
                    if (status < 400) {
                        return (new String[]{"missing handler for response", connection.getResponseMessage()});
                    } else {
                        return (new String[]{"error", connection.getResponseMessage()});
                    }
                }

            } catch (Exception ex) {
                // Exception handling
                return (new String[]{"error", ex.getMessage()});
            }
        }

        @Override
        protected void onPostExecute(String[] result) {
            if ("OK".equals(result[0])) {
                // Success !!
                logger.log("FileDownloaderTask: Success!");
            } else if ("error".equals(result[0])) {
                logger.error("FileDownloaderTask: Error " + result[1]);
            } else if ("authorizationRequired".equals(result[0])) {

                // Obtain scope
                String scope = result[1];
                logger.debug("Requesting access token for scope: " + scope);

                // Obtain access token
                WLAuthorizationManager.getInstance().obtainAccessToken(scope, new WLAccessTokenListener() {

                    // This method will be invoked if an access token was obtained successfully:
                    @Override
                    public void onSuccess(AccessToken accessToken) {
                        logger.log("Access token obtained. Resending request to download the large file with the access token");
                        // Resend request to download the large file with the access token
                        try {
                            // Create new connection
                            URLConnection conn = getConnection(DOWNLOAD_RESOURCE_PATH);
                            // Cache the access token
                            authToken = accessToken.getAsAuthorizationRequestHeader();
                            // Retry download the large file
                            new FileDownloaderTask().execute(conn);
                        } catch (Exception e) {
                            logger.error("Exception during request" + e.getMessage());
                        }
                    }

                    // This method will be invoked if an access token  was not obtained:
                    @Override
                    public void onFailure(WLFailResponse response) {
                        logger.error("Failed to obtain access token: " + response.getErrorMsg());
                    }
                });
            } else {
                // Unhandled response
                logger.log(result[0] + ": " + result[1]);
            }
        }
    }


    /**
     * AsyncTask for uploading large file to the MFP server
     */
    class FileUploaderTask extends AsyncTask<URLConnection, Void, String[]> {

        @Override
        protected String[] doInBackground(URLConnection... conn) {

            DataOutputStream outputStream = null;
            FileInputStream inputStream = null;

            try {
                // Open stream for reading file from the device
                inputStream = openFileInput(LARGE_FILE_NAME);

                // Use HttpURLConnection API
                HttpURLConnection connection = (HttpURLConnection)conn[0];

                // Prepare the request
                connection.setChunkedStreamingMode(BUFFER_SIZE);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type","application/octet-stream");

                // Open output stream
                outputStream = new DataOutputStream(connection.getOutputStream());

                // Write file to output stream
                int read;
                byte[] data = new byte[BUFFER_SIZE];
                while ((read = inputStream.read(data)) != -1) {
                    outputStream.write(data, 0, read);
                }
                // Write request suffix

                // Read response
                int status = connection.getResponseCode();
                if (status == 200) {
                    return (new String[]{"OK", connection.getResponseMessage()});
                }

                // Check if MFP OAuth authorization is required
                if (WLAuthorizationManager.getInstance().isAuthorizationRequired(connection)) {
                    logger.debug("Authorization required!");
                    return (new String[]{"authorizationRequired", WLAuthorizationManager.getInstance().getResourceScope(connection)});
                } else {
                    if (status < 400) {
                        return (new String[]{"missing handler for response", connection.getResponseMessage()});
                    } else {
                        return (new String[]{"error", connection.getResponseMessage()});
                    }
                }

            } catch (Exception ex) {
                // Exception handling
                return (new String[]{"error", ex.getMessage()});
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.flush();
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String[] result) {

            if ("OK".equals(result[0])) {
                // Success !!
                logger.log("FileUploader: Success!");
            } else if ("error".equals(result[0])) {
                logger.error("FileUploader: Error " + result[1]);
            } else if ("authorizationRequired".equals(result[0])) {

                // Obtain scope
                String scope = result[1];
                logger.debug("Requesting access token for scope: " + scope);

                // Obtain access token
                WLAuthorizationManager.getInstance().obtainAccessToken(scope, new WLAccessTokenListener() {

                    // This method will be invoked if an access token was obtained successfully:
                    @Override
                    public void onSuccess(AccessToken accessToken) {
                        logger.log("Access token obtained. Resending request to upload the large file with the access token");
                        // Resend request to download the large file with the access token
                        try {
                            // Create new connection
                            URLConnection conn = getConnection(UPLOAD_RESOURCE_PATH);
                            // Cache the access token
                            authToken = accessToken.getAsAuthorizationRequestHeader();
                            // Retry download the large file
                            new FileUploaderTask().execute(conn);
                        } catch (Exception e) {
                            logger.error("Exception during upload request" + e.getMessage());
                        }
                    }

                    // This method will be invoked if an access token  was not obtained:
                    @Override
                    public void onFailure(WLFailResponse response) {
                        logger.error("Failed to obtain access token: " + response.getErrorMsg());
                    }
                });
            } else {
                // Unhandled response
                logger.log(result[0] + ": " + result[1]);
            }
        }
    }
}
