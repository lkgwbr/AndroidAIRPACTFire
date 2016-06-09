package lar.wsu.edu.airpact_fire;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/* LOGIN SCREEN */
/*
 * [INSERT DESCRIPTION]
 *
 */

// NOTE: Problem is with retrieving <isAuth /> field in *XML*, not the network auth. I'm pretty sure
// it has to do with the image. This happened after I took a picture for that user and then logged out.
// TODO: Address above NOTE
// TODO: Internet status (color-coded) on home, view gallery option (web browser), as well as last login time and other stats
// TODO: Custom Toast display, to make it more obvious to user
// TODO: More responsive buttons
// TODO: "Last logged in X days ago" on home screen
// TODO: Figure out why picture details activity doesn't repopulate estimated visual range and description.
//  This way we'll only deal with the image when we take a new one, view it, or post it.
// TODO: Check for pre-existing user. If user doesn't exist, app must authenticate with server to continue and
//  be added to the local user database. This eliminates the need for an <isAuth /> field and decreases the space
//  consumed by the local user XML file.
// TODO: Make code more modular and expandable
// TODO: Make separate files for the image, linked to by the <image /> tag by the user (e.g. "test_image.jpg")
// TODO: Add "Created by Luke Weber" signature to all files
// TODO: Add notification when we have connection to server, and not just internet access. Although,
//  we still want to know about internet access so we can know when to queue posts? We could just check
//  to see if
// TODO: Add notifications for when server comes up. Have a batch of checks and actions done by the app occasionally,
//  say, every 3 hours, like posting for backlogged posts. Also have frequent checks while app is running
//  that give toast/notifications when server is up. Maybe do something with notifications as well.
// TODO: Add home button to each page
// TODO: Find better way to do data management (i.e. better data structures and algorithms)
// TODO: Make it so we don't have to do GetUserData(USER, element), because we will always use lastUser
// TODO: Show post trends (location, time, etc.)
// TODO: Have auto-fill for login page and post page
// TODO: Allow user to view post coordinates in Google Maps
// TODO: When queued post is submitted, don't create a new post entirely for SQL database. Rather, just change the original.
// TODO: Have loading icon for SignInActivity on first-time install (because it takes a little while).
// TODO: Be able to handle null inputs on PictureDetailsActivity
// TODO: Be able to check for valid inputs on same activity.

public class SignInActivity extends AppCompatActivity {

    // UI references
    private EditText mPasswordView, mUsernameView;
    private Button mEmailSignInButton, mRegisterButton;

    // Startup progress
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // TODO: Remove
        testProceed();

        // Attach objects to UI
        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mRegisterButton = (Button) findViewById(R.id.register_button);

        progress = new ProgressDialog(SignInActivity.this);

        // Show loader
//        progress.setTitle("Starting up");
//        progress.setMessage("Give us a minute to set things up...");
//        progress.show();

        // XML Stuff -- create XML if necessary
        UserDataManager.init(getApplicationContext());

        // Hide
        //progress.hide();

        // Set up the login form
        populateLoginFields();

        // Proceeds to home
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Store values at the time of the login attempt.
                String username = mUsernameView.getText().toString();
                String password = mPasswordView.getText().toString();

                // Update user's password and login time (and create one if we need to)
                UserDataManager.setUserData(username, "password", password);
                UserDataManager.setUserData(username, "loginTime", (new Date()).toString());
                // Set as last user
                UserDataManager.setRecentUser(username);

                // For regulars; no network auth. required
                if (Boolean.parseBoolean(UserDataManager.getUserData(UserDataManager.getRecentUser(), "isAuth")))
                {
                    Toast.makeText(SignInActivity.this, "Welcome back!", Toast.LENGTH_LONG).show();
                    openHomeScreen();
                    return;
                }

                // Attempt first-time authentication
                AuthenticateManager authenticateManager = new AuthenticateManager();
                authenticateManager.execute();
            }
        });

        // Open web URL
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(Post.SERVER_REGISTER_URL);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    // Set credentials of last user
    private void populateLoginFields() {
        String lastUser = UserDataManager.getRecentUser();
        String lastPassword = UserDataManager.getUserData(lastUser, "password");

        mUsernameView.setText(lastUser);
        mPasswordView.setText(lastPassword);
    }

    // Open main page
    private void openHomeScreen() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    // Move on from home screen
    private void testProceed() {
        // Get input data
        String username = "test";
        String password = "1234567890";

        // Create new authenticated user
        UserDataManager.setRecentUser(username);
        UserDataManager.setUserData(username, "isAuth", "true");
        UserDataManager.setUserData(username, "password", password);
        UserDataManager.setUserData(username, "loginTime", (new Date()).toString());

        // Open home
        openHomeScreen();
    }

    // Deals with server
    class AuthenticateManager extends AsyncTask<Void, Void, Void> {

        private ProgressDialog progress = new ProgressDialog(SignInActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Show loader
            progress.setTitle("Logging In");
            progress.setMessage("Please wait while we attempt authentication...");
            progress.show();

            // Make sure it displays before doing work
            while (!progress.isShowing()) try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Authentication URL
                URL authUrl = new URL(Post.SERVER_AUTH_URL);

                // JSON authentication (send) package
                JSONObject authSendJSON = new JSONObject();
                authSendJSON.put("username", UserDataManager.getRecentUser());
                authSendJSON.put("password",
                        UserDataManager.getUserData(UserDataManager.getRecentUser(), "password"));

                String sendMessage = authSendJSON.toJSONString(),
                        serverResponse,
                        userKey;
                Boolean isUser;

                // JSON receive package
                JSONObject authReceiveJSON;

                // Establish HTTP connection
                HttpURLConnection authConn = (HttpURLConnection) authUrl.openConnection();

                // Set connection properties
                authConn.setReadTimeout(10000);
                authConn.setConnectTimeout(15000);
                authConn.setRequestMethod("POST");
                authConn.setDoInput(true);
                authConn.setDoOutput(true);
                authConn.setFixedLengthStreamingMode(sendMessage.getBytes().length);
                authConn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                authConn.setRequestProperty("X-Requested-With", "XMLHttpRequest");

                // Connect to server
                authConn.connect();

                // JSON package sent
                OutputStream authOutputStream = new BufferedOutputStream(authConn.getOutputStream());
                authOutputStream.write(sendMessage.getBytes());
                // NOTE: Had an error here before because I didn't flush
                authOutputStream.flush();

                // Server reply
                InputStream in = null;
                try {
                    in = authConn.getInputStream();
                    int ch;
                    StringBuffer sb = new StringBuffer();
                    while ((ch = in.read()) != -1) {
                        sb.append((char) ch);
                    }
                    serverResponse = sb.toString();
                } catch (IOException e) {
                    throw e;
                }
                if (in != null) {
                    in.close();
                }

                // Parse JSON
                authReceiveJSON = (JSONObject) new JSONParser().parse(serverResponse);

                // Get SubmitFieldVars and see if server authenticated us
                isUser = Boolean.parseBoolean((String) authReceiveJSON.get("isUser"));
                if (isUser) {
                    // Don't do anything with key for now
                    //userKey = authReceiveJSON.get("secretKey").toString();
                    UserDataManager.setUserData(UserDataManager.getRecentUser(), "isAuth", "true");
                } else { // Exit if not a user
                    UserDataManager.setUserData(UserDataManager.getRecentUser(), "isAuth", "false");
                    return null;
                }

                authOutputStream.flush();
                authOutputStream.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void context) {

            // To dismiss the dialog
            progress.dismiss();

            // Open up home screen
            if (Boolean.parseBoolean(UserDataManager.getUserData(UserDataManager.getRecentUser(),
                    "isAuth"))) {
                Toast.makeText(SignInActivity.this, "Authentication successful.\nWelcome!",
                        Toast.LENGTH_LONG).show();

                openHomeScreen();
            } else
                Toast.makeText(SignInActivity.this, "Could not authenticate user.\nPlease try again.",
                        Toast.LENGTH_SHORT).show();
        }
    }
}