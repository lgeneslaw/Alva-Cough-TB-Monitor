package com.paullcchang.tbmoniter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class RegisterActivity extends ActionBarActivity {

    private Button registerButton;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText repeatPasswordEditText;

    private View mProgressView;
    private View mRegisterFormView;

    private View mErrorView;
    private TextView errorTextView;

    private UserRegisterTask mAuthTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        String email = getIntent().getStringExtra(LoginActivity.EMAIL_TAG);
        String password = getIntent().getStringExtra(LoginActivity.PASSWORD_TAG);

        emailEditText = (EditText) findViewById(R.id.emailEditText);
        emailEditText.setText(email);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        passwordEditText.setText(password);
        repeatPasswordEditText = (EditText) findViewById(R.id.repeatPasswordEditText);

        if(email == null || TextUtils.isEmpty(email)){
            emailEditText.requestFocus();
        }else if(password == null || TextUtils.isEmpty(password)){
            passwordEditText.requestFocus();
        }else{
            repeatPasswordEditText.requestFocus();
        }

        registerButton = (Button) findViewById(R.id.registerButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegisterUser();
            }
        });

        mRegisterFormView = findViewById(R.id.register_form);
        mProgressView = findViewById(R.id.register_progress);

        mErrorView = findViewById(R.id.error_layout);
        errorTextView = (TextView) findViewById(R.id.error_textView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = getIntent();
                setResult(Activity.RESULT_CANCELED, intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void attemptRegisterUser(){
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        emailEditText.setError(null);
        passwordEditText.setError(null);
        repeatPasswordEditText.setError(null);

        // Store values at the time of the login attempt.
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String repeatPassword = repeatPasswordEditText.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid repeat password, if the user entered one.
        if (TextUtils.isEmpty(repeatPassword) || !isRepeatPassword(password, repeatPassword)) {
            repeatPasswordEditText.setError(getString(R.string.error_invalid_password));
            focusView = repeatPasswordEditText;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            passwordEditText.setError(getString(R.string.error_invalid_password));
            focusView = passwordEditText;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.error_field_required));
            focusView = emailEditText;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailEditText.setError(getString(R.string.error_invalid_email));
            focusView = emailEditText;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserRegisterTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        boolean isValid = true;
        isValid = email.contains("@");
        return isValid;
    }

    private boolean isPasswordValid(String password) {
        boolean isValid = true;
        isValid = password.length() > 4;
        return isValid;
    }

    private boolean isRepeatPassword(String password, String repeatPassword) {
        boolean isRepeat = false;
        isRepeat = password.equals(repeatPassword);
        return isRepeat;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mErrorView.setVisibility(View.GONE);

            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mRegisterFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });

        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mErrorView.setVisibility(View.GONE);
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous registration task used to authenticate
     * the user.
     */
    public class UserRegisterTask extends AsyncTask<Void, Void, Boolean> {
        private final String email;
        private final String password;

        UserRegisterTask(String email, String password) {
            this.email = email;
            this.password = password;
        }

        // Creating JSON Parser object
        private JSONParser jParser = new JSONParser();

        // url to register account
        private static final String URL = "http://skysip.org/p539/php/";
        private static final String url_register = URL + "register.php";

        // JSON Node names
        public static final String TAG_SUCCESS = "success";
        public static final String TAG_MESSAGE = "message";

        private static final String PARAM_EMAIL = "email";
        private static final String PARAM_PASSWORD = "pass";

        @Override
        protected Boolean doInBackground(Void... params) {
            List<NameValuePair> registerParams = new ArrayList<NameValuePair>();
            registerParams.add(new BasicNameValuePair(PARAM_EMAIL, email));
            registerParams.add(new BasicNameValuePair(PARAM_PASSWORD, password));
            // getting JSON string from URL
            JSONObject json = jParser.makeHttpRequest(url_register, JSONParser.POST, registerParams);

            // Check your log cat for JSON reponse
            if(json != null) {
                Log.d("http", "return data: " + json.toString());
            }

            boolean success = false;
            try {
                if(json != null) {
                    success = json.getBoolean(TAG_SUCCESS);
                    Log.d("http", "return success code: " + success);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return success;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {
                Intent intent = getIntent();
                intent.putExtra(LoginActivity.EMAIL_TAG, emailEditText.getText().toString());
                setResult(Activity.RESULT_OK, intent);
                finish();
            } else {
                showProgress(false);

                errorTextView.setText(getApplicationContext().getResources().getString(R.string.error_registering));
                emailEditText.requestFocus();
                mErrorView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

}
