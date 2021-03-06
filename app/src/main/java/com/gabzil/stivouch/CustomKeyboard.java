package com.gabzil.stivouch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.Editable;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

class CustomKeyboard implements OnTaskCompleted {
    private KeyboardView mKeyboardView;
    private Activity mHostActivity;
    Entities e = new Entities();

    public CustomKeyboard(Activity host, int viewid, int layoutid) {
        mHostActivity= host;
        mKeyboardView= (KeyboardView)mHostActivity.findViewById(viewid);
        mKeyboardView.setKeyboard(new Keyboard(mHostActivity, layoutid));
        mKeyboardView.setPreviewEnabled(false); // NOTE Do not show the preview balloons
        mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);
        // Hide the standard keyboard initially
        mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /** The key (code) handler. */
    private OnKeyboardActionListener mOnKeyboardActionListener = new OnKeyboardActionListener() {
        public final static int CodeDelete   = -5;
        public final static int Submit    = 55006;

        @Override public void onKey(int primaryCode, int[] keyCodes) {
            View focusCurrent = mHostActivity.getWindow().getCurrentFocus();
            if( focusCurrent==null || focusCurrent.getClass()!=EditText.class )
                return;
            EditText edittext = (EditText) focusCurrent;
            Editable editable = edittext.getText();
            int start = edittext.getSelectionStart();

            if( primaryCode==CodeDelete ) {
                if( editable!=null && start>0 ) editable.delete(start - 1, start);
            } else if( primaryCode==Submit ) {
                e.setMobileNo(edittext.getText().toString().trim());
                if(!IsValidation())
                    CallSubmit(e.getMobileNo());
            } else {
                editable.insert(start, Character.toString((char) primaryCode));
            }
        }

        @Override public void onPress(int arg0) {
        }

        @Override public void onRelease(int primaryCode) {
        }

        @Override public void onText(CharSequence text) {
        }

        @Override public void swipeDown() {
        }

        @Override public void swipeLeft() {
        }

        @Override public void swipeRight() {
        }

        @Override public void swipeUp() {
        }
    };

    private boolean IsValidation() {
        boolean error = false;
        if (e.getMobileNo().length() < 10) {
            Toast.makeText(mHostActivity, "Please enter 10 digits mobile number", Toast.LENGTH_SHORT).show();
            error = true;
        } else if(e.getMobileNo().equals("0000000000")) {
            Toast.makeText(mHostActivity, "Please enter valid mobile number", Toast.LENGTH_SHORT).show();
            error = true;
        }
        return error;
    }

    /** Make the CustomKeyboard visible, and hide the system keyboard for view v. */
    public void showCustomKeyboard(View v) {
        mKeyboardView.setVisibility(View.VISIBLE);
        mKeyboardView.setEnabled(true);
        if (v != null)
            ((InputMethodManager) mHostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    /** Make the CustomKeyboard invisible. */
    public void hideCustomKeyboard() {
        mKeyboardView.setVisibility(View.GONE);
        mKeyboardView.setEnabled(false);
    }

    /** Register EditText with resource id (on the hosting activity) for using this custom keyboard. */
    EditText edittext;
    public void registerEditText(int resid) {
        edittext= (EditText)mHostActivity.findViewById(resid);
        edittext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomKeyboard(v);
                InputMethodManager imm = (InputMethodManager) mHostActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);
            }
        });

        // Make the custom keyboard appear
        edittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            // NOTE By setting the on focus listener, we can show the custom keyboard when the edit box gets focus, but also hide it when the edit box loses focus
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    showCustomKeyboard(v);
                else
                    hideCustomKeyboard();
            }
        });
    }

    public void CallSubmit(String mobileno) {
        final SubmitNumber p=new SubmitNumber(mHostActivity, this);
        p.execute(mobileno);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (p.getStatus() == AsyncTask.Status.RUNNING) {
                    // My AsyncTask is currently doing work in doInBackground()
                    p.cancel(true);
                    p.mProgress.dismiss();
                    Toast.makeText(mHostActivity, "Network Problem, Please try again", Toast.LENGTH_SHORT).show();
                }
            }
        }, 1000 * 30);
    }

    @Override
    public void OnTaskCompleted(String results) {
        try {

            if (results != "null" && results.length() > 0) {
                if (results.equals("true")) {
                    e.setOTP("Yes");
                    e.setPin("No");
                    e.setLogin("No");
                    DataHelp dh = new DataHelp(mHostActivity);
                    if (dh.UpdateSelection(e)) {
                        Intent i = new Intent(mHostActivity, MobileRegistration.class);
                        mHostActivity.startActivity(i);
                    }
                } else {
                    Toast.makeText(mHostActivity, "Some problem occured,please try again", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(mHostActivity, "Some problem occured,please try again", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(mHostActivity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}