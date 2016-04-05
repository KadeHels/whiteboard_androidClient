package com.umdcs4995.whiteboard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.umdcs4995.whiteboard.driveOps.DriveSaveFragment;
import com.umdcs4995.whiteboard.services.SocketService;
import com.umdcs4995.whiteboard.services.SocketService.Messages;
import com.umdcs4995.whiteboard.uiElements.ContactListFragment;
import com.umdcs4995.whiteboard.uiElements.JoinBoardFragment;
import com.umdcs4995.whiteboard.uiElements.LoadURLFragment;
import com.umdcs4995.whiteboard.uiElements.LoadURLFragment.OnFragmentInteractionListener;
import com.umdcs4995.whiteboard.uiElements.LoadURLFragment.OnOkBtnClickedListener;
import com.umdcs4995.whiteboard.uiElements.LoginFragment;
import com.umdcs4995.whiteboard.uiElements.LoginFragment.GoogleSignInActivityResult;
import com.umdcs4995.whiteboard.uiElements.LoginFragment.OnLoginBtnClickedListener;
import com.umdcs4995.whiteboard.uiElements.NewBoardFragment;
import com.umdcs4995.whiteboard.uiElements.WhiteboardDrawFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import io.socket.emitter.Emitter.Listener;

//import com.umdcs4995.whiteboard.uiElements.LoginFragment.OnLoginBtnClickedListener;



public class MainActivity extends AppCompatActivity
        implements OnNavigationItemSelectedListener,
        OnOkBtnClickedListener,
        OnFragmentInteractionListener, OnLoginBtnClickedListener, LoginFragment.OnFragmentInteractionListener,
        ConnectionCallbacks, OnConnectionFailedListener {

    Fragment whiteboardDrawFragment = new WhiteboardDrawFragment();
    Fragment contactListFragment = new ContactListFragment();
    Fragment joinBoardFragment = new JoinBoardFragment();
    Fragment newBoardFragment = new NewBoardFragment();
    Fragment loadURLFragment = new LoadURLFragment();
    Fragment loginFragment = new LoginFragment();
    Fragment driveSaveFragment = new DriveSaveFragment();

    private SocketService socketService = Globals.getInstance().getSocketService();

    private GoogleSignInActivityResult pendingGoogleSigninResult;
    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 9001;
    private boolean isSignedIn;
    private OnFragmentInteractionListener onFragmentInteractionListener;
    private GoogleApiClient googleApiClient;
    private GoogleSignInOptions gso;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //SET THE TOOLBAR BELOW
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Whiteboard");
        setSupportActionBar(toolbar);

        /**
         *Hides or makes visible the draw components and toolbar
         */
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                WhiteboardDrawFragment.fabHideMenu(view);//Function used to set draw components visibility
                //Statement used to set toolbars visibility
                if(toolbar.getVisibility()==view.GONE){
                    toolbar.setVisibility(view.VISIBLE);
                }
                else{
                    toolbar.setVisibility(view.GONE);
                }
            }
        });

        //SET THE NAVIGATION DRAWER BELOW
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //SETUP THE DEFAULT FRAGMENT
        changeMainFragment(whiteboardDrawFragment);

        //Create the Google Api Client
        // Configure sign-in to request the user's ID, email address, and
        // basic profile.
        gso = new Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestScopes(new Scope(Scopes.DRIVE_APPFOLDER), Drive.SCOPE_FILE).build();

        googleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addApi(AppIndex.API).addApi(Drive.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this)
                .build();
   ;
//        builder.addScope(SCOPE_FILE);


        if (googleApiClient.isConnected() == false) {
            googleApiClient.connect();
        }

        //credential = GoogleAccountCredential.usingOAuth2(getActivity().getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff()).setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
//        googleApiClient.connect();

    }

    /*
     * This function handles the back button closing the navigation drawer and
     * then calling the parent back pressed function
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * NavItem selected method
     *
     * @param item
     * @return
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {

            // TODO: move to NewBoardFragment class and call changeMainFragment()
            // The client tries to create new whiteboard by sending the server the name of the whiteboard.
            // The server then replies with a error message or a create successful message.
            case R.id.add_board:
                JSONObject createWbRequest = new JSONObject();
                try {
                    // TODO: make a whiteboard name chooser and use its input here
                    String uuid = UUID.randomUUID().toString();
                    createWbRequest.put("name", uuid);
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "Error making createWhiteboard request - this is bad...", Toast.LENGTH_LONG);
                }
                socketService.sendMessage(Messages.CREATE_WHITEBOARD, createWbRequest);

                socketService.addListener(Messages.CREATE_WHITEBOARD, new Listener() {
                    @Override
                    public void call(Object... args) {
                        // TODO: Set up the whiteboard + join it here
                        JSONObject recvd = (JSONObject) args[0];
                        try {
                            Log.i("createWhiteboard", "received message: " + recvd.getString("message"));
                        } catch (JSONException e) {
                            Log.w("createWhiteboard", "error parsing received message");
                        }
                        socketService.clearListener(Messages.CREATE_WHITEBOARD);
                    }
                });
                break;

            // The client tries to join a whiteboard by sending the server the name of the whiteboard.
            // The server then replies with a error message or a join successful message.
            case R.id.join_board:
                changeMainFragment(joinBoardFragment);
                break;

            case R.id.nav_contacts://Navigates to list of contacts
                changeMainFragment(contactListFragment);
                break;

            case R.id.nav_settings://Navigates to Settings Activity
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;

            case R.id.add_url:
                //loadURLFragment.startActivity(new Intent(this, LoadURLFragment.class));
                changeMainFragment(loadURLFragment);
                break;

            case R.id.login:
                changeMainFragment(loginFragment);
                break;
            case R.id.google_drive:
                Bundle bundle = new Bundle();
                //bundle.putParcelable("bitmap", findViewById(R.id.drawing).getDrawingCache());
                Bitmap b = findViewById(R.id.drawing).getDrawingCache();
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                b.compress(CompressFormat.PNG, 50, bs);
                bundle.putByteArray("byteArray", bs.toByteArray());
//                bundle.putParcelable("byteArray", bs.toByteArray());
                driveSaveFragment.setArguments(bundle);
                changeMainFragment(driveSaveFragment);
                break;

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Replaces the current fragment on the mainFrame layout.
     * @param fragment
     */
    private void changeMainFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.mainFrame, fragment);
        transaction.addToBackStack(fragment.toString());
        transaction.commit();
    }

    /*
     * This function handles the "ok" button for loading images from a URL
     * it creates a temporary fragment and sets the new background to the
     * specified url and changes the fragment to the new one
     */
    @Override
    public void onOkBtnClicked(String urlString) {
        WhiteboardDrawFragment tempFragment = (WhiteboardDrawFragment) whiteboardDrawFragment;
        //tempFragment.setNewBackground(urlString);
       tempFragment.loadBackgroundFromURL(urlString);
        changeMainFragment(whiteboardDrawFragment);

    }

    @Override
    public void onLoginBtnClicked() {
        changeMainFragment(whiteboardDrawFragment);
    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        // LoginFragment doesn't get all of the onActivityResults for google sign in
        // so the activity needs to proxy them through but only after the LoginFragment has
        // been registered with the event bus.
        if (requestCode == RC_SIGN_IN) {
            pendingGoogleSigninResult = new GoogleSignInActivityResult(requestCode,
                    resultCode, data);
        }
    }

//    private void handleSignInResult(GoogleSignInResult result) {
//        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
//        if (result.isSuccess()) {
//            //Signed in successfully, show authenticated UI.
//            GoogleSignInAccount acct = result.getSignInAccount();
//            isSignedIn = true;
//            //mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
//            //updateUI(true);
//
//        } else {
//            //Signed Out, show unathenticated UI.
//            isSignedIn = false;
//        }
//    }

    public boolean openLoginDialogIfLoggedOut() {
        if (!isSignedIn) {
            //LoginFragment.newInstance().show(getSupportFragmentManager(), "LoginFragment");
            return true;
        } else {
            return false;
        }
    }

    public int getActivityid() {
        return 0;
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        googleApiClient.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.umdcs4995.whiteboard/http/host/path")
        );
        AppIndex.AppIndexApi.start(googleApiClient, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.umdcs4995.whiteboard/http/host/path")
        );
        AppIndex.AppIndexApi.end(googleApiClient, viewAction);
        googleApiClient.disconnect();
    }
}
