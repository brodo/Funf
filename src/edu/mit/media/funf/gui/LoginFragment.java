package edu.mit.media.funf.gui;


import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import edu.mit.media.funf.R;
import edu.mit.media.funf.util.LogUtil;

/**
 * Created with IntelliJ IDEA.
 * User: Julian Dax
 * Date: 15/11/13
 * Time: 08:11
 * To change this template use File | Settings | File Templates.
 */
public class LoginFragment extends Fragment {
    private EditText username;
    private EditText password;
    private Button login;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.login, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(getView().getContext());
        editor = preferences.edit();
        username = (EditText)getView().findViewById(R.id.usernameEditText);
        password = (EditText)getView().findViewById(R.id.passwordEditText);
        login    = (Button)getView().findViewById(R.id.loginButton);
        login.setOnClickListener(loginClick);
        username.setText(getUsername());
        password.setText(getPassword());

    }


    public String getUsername(){
        return preferences.getString("funf.username", "");
    }

    public String getPassword(){
        return preferences.getString("funf.password", "");
    }

    public void setUsername(String username){
        editor.putString("funf.username", username);
        editor.commit();
    }

    public void setPassword(String password){
        editor.putString("funf.password", password);
        editor.commit();
    }



    private View.OnClickListener loginClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setPassword(password.getText().toString());
            setUsername(username.getText().toString());
        }
    };



}
