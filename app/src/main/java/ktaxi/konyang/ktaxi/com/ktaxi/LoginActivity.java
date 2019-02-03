package ktaxi.konyang.ktaxi.com.ktaxi;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends Activity {
    private static final String TAG = "OAuthSampleActivity";
    private static final String OAUTH_CLIENT_ID = "dMCMQvLsPMo_GrP1cIGw";
    private static final String OAUTH_CLIENT_SECRET = "4ZY1doeHrB";
    private static final String OAUTH_CLIENT_NAME = "ky_kakau";
    private static OAuthLogin mOAuthLoginInstance;
    private static Context mContext;

    private TextView mApiResultText;
    private static TextView mOauthAT;
    private static TextView mOauthRT;
    private static TextView mOauthExpires;
    private static TextView mOauthTokenType;
    private static TextView mOAuthState;

    private OAuthLoginButton mOAuthLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mContext = this;
        initData();
        initView();

        this.setTitle("OAuthLoginSample Ver." + OAuthLogin.getVersion());
    }
    private void initData() {
        mOAuthLoginInstance = OAuthLogin.getInstance();

        mOAuthLoginInstance.showDevelopersLog(true);
        mOAuthLoginInstance.init(mContext, OAUTH_CLIENT_ID, OAUTH_CLIENT_SECRET, OAUTH_CLIENT_NAME);
    }
    private void initView() {
        mApiResultText = (TextView) findViewById(R.id.api_result_text);

        mOauthAT = (TextView) findViewById(R.id.oauth_access_token);
        mOauthRT = (TextView) findViewById(R.id.oauth_refresh_token);
        mOauthExpires = (TextView) findViewById(R.id.oauth_expires);
        mOauthTokenType = (TextView) findViewById(R.id.oauth_type);
        mOAuthState = (TextView) findViewById(R.id.oauth_state);

        mOAuthLoginButton = (OAuthLoginButton) findViewById(R.id.buttonOAuthLoginImg);
        mOAuthLoginButton.setOAuthLoginHandler(mOAuthLoginHandler);

        updateView();

    }
    private void updateView() {
        mOauthAT.setText(mOAuthLoginInstance.getAccessToken(mContext));
        mOauthRT.setText(mOAuthLoginInstance.getRefreshToken(mContext));
        mOauthExpires.setText(String.valueOf(mOAuthLoginInstance.getExpiresAt(mContext)));
        mOauthTokenType.setText(mOAuthLoginInstance.getTokenType(mContext));
        mOAuthState.setText(mOAuthLoginInstance.getState(mContext).toString());
    }

    @Override
    protected void onResume() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        super.onResume();

    }
    static private OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
        @Override
        public void run(boolean success) {
            if (success) {
                String accessToken = mOAuthLoginInstance.getAccessToken(mContext);
                String refreshToken = mOAuthLoginInstance.getRefreshToken(mContext);
                long expiresAt = mOAuthLoginInstance.getExpiresAt(mContext);
                String tokenType = mOAuthLoginInstance.getTokenType(mContext);
                mOauthAT.setText(accessToken);
                mOauthRT.setText(refreshToken);
                mOauthExpires.setText(String.valueOf(expiresAt));
                mOauthTokenType.setText(tokenType);
                mOAuthState.setText(mOAuthLoginInstance.getState(mContext).toString());
            } else {
                String errorCode = mOAuthLoginInstance.getLastErrorCode(mContext).getCode();
                String errorDesc = mOAuthLoginInstance.getLastErrorDesc(mContext);
                Toast.makeText(mContext, "errorCode:" + errorCode + ", errorDesc:" + errorDesc, Toast.LENGTH_SHORT).show();
            }
        }

    };

    public void buttonOAuth(View view) {
        mOAuthLoginInstance.startOauthLoginActivity(LoginActivity.this, mOAuthLoginHandler);
    } // 로그인

    public void buttonVerifier(View view) {
        new RequestApiTask().execute();
    } // 내정보 확인

    public void buttonRefresh(View view) {
        new RefreshTokenTask().execute();
    } // 토큰 다시받기

    public void buttonOAuthLogout(View view) {
        mOAuthLoginInstance.logout(mContext);
        updateView();
    } // 로그아웃

    public void buttonOAuthDeleteToken(View view) {
        new DeleteTokenTask().execute();
    } // 회원탈퇴

    private class DeleteTokenTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            boolean isSuccessDeleteToken = mOAuthLoginInstance.logoutAndDeleteToken(mContext);

            if (!isSuccessDeleteToken) {
                // 서버에서 token 삭제에 실패했어도 클라이언트에 있는 token 은 삭제되어 로그아웃된 상태이다
                // 실패했어도 클라이언트 상에 token 정보가 없기 때문에 추가적으로 해줄 수 있는 것은 없음
                Log.d(TAG, "errorCode:" + mOAuthLoginInstance.getLastErrorCode(mContext));
                Log.d(TAG, "errorDesc:" + mOAuthLoginInstance.getLastErrorDesc(mContext));
            }

            return null;
        }

        protected void onPostExecute(Void v) {
            updateView();
        }
    }

    private class RequestApiTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            mApiResultText.setText((String) "");
        }

        @Override
        protected String doInBackground(Void... params) {
            String url = "https://openapi.naver.com/v1/nid/me";
            String at = mOAuthLoginInstance.getAccessToken(mContext);
            return mOAuthLoginInstance.requestApi(mContext, at, url);
        }

        protected void onPostExecute(String content) {
            String id = " ";
            String age = " ";
            String gender = " ";
            String email = " ";
            String name = " ";
            try{
                JSONObject object = new JSONObject(content);
                JSONObject contents = (JSONObject)object.get("response");

                id = contents.getString("id");
                age = contents.getString("age");
                gender = contents.getString("gender");
                email = contents.getString("email");
                name = contents.getString("name");
            }catch (JSONException e){
            }
            mApiResultText.setText((String) content + "\n id : "+id+"\n 연령대 : "+age+"\n 성별 : "+gender+"\n 이메일 : "+email+"\n 이름 : "+name);
        }
    }

    private class RefreshTokenTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return mOAuthLoginInstance.refreshAccessToken(mContext);
        }

        protected void onPostExecute(String res) {
            updateView();
        }
    }


}

