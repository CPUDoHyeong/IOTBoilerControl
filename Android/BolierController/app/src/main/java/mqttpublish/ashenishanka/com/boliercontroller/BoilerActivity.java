package mqttpublish.ashenishanka.com.boliercontroller;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.triggertrap.seekarc.SeekArc;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class BoilerActivity extends AppCompatActivity {

    private SeekArc mSeekArc, mSeekArc2;
    private TextView mSeekProgress, mSeekProgress2;
    private final String RESERVE = "予約";
    private final String WATER = "温水";
    private final String ROOM = "室温";
    private SharedPreferences preference;
    private TextView temperature;
    private Button resetBtn;
    private String status;
    private int status_val = 0;
    private String controlStatus;
    private int progressVal = 0;
    private int progress2Max = 30;
    private int maxAngle = 180;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.constraint_layout);

        // 상태바 글자색 변경
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        mSeekArc = (SeekArc) findViewById(R.id.seekArc);
        mSeekArc2 = (SeekArc) findViewById(R.id.seekArc2);
        mSeekProgress = (TextView) findViewById(R.id.seekProgress);
        mSeekProgress2 = (TextView) findViewById(R.id.seekProgress2);
        temperature = (TextView) findViewById(R.id.temperature);
        resetBtn = (Button) findViewById(R.id.resetBtn);

        // set preference
        preference = getSharedPreferences("setting", Activity.MODE_PRIVATE);

        // preference 에 저장된 temperature 값이 있으면 보여준다.
        final String temp = preference.getString("temperature", null);
        if(temp == null) {}
        else {
            temperature.setText(temp);
        }

        // preference 에 저장된 status 값과 status_val 값이 있으면 그 값을 보여준다.
        status = preference.getString("status", null);
        status_val = preference.getInt("status_val", 0);
        if(status == null) {}
        else {
            mSeekProgress.setText(status);      // set text
            mSeekArc.setProgress(status_val);   // set progress
        }

        // preference 에 저장된 controlStatus 값과 progressVal 값이 있으면 그 값을 보여준다.
        controlStatus = preference.getString("controlStatus", null);
        progressVal = preference.getInt("progressVal", 0);
        if(controlStatus == null) {
            Toast.makeText(BoilerActivity.this,"empty value",Toast.LENGTH_SHORT).show();
        }
        else {
            mSeekProgress2.setText(controlStatus);
            mSeekArc2.setProgress(progressVal);
        }

        String clientId = MqttClient.generateClientId();
        final MqttAndroidClient client =
                new MqttAndroidClient(this.getApplicationContext(), "tcp://52.79.235.179:1883",
                        clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    //    Log.d(TAG, "onSuccess");
                    Toast.makeText(BoilerActivity.this, "MQTT connected", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    //   Log.d(TAG, "onFailure");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String topic = "temperature";
                String payload = "call_temperature";
                setPublish(client, topic, payload);
            }
        });

        // 라즈베리파이로부터 값이 올 때.
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String msg = new String(message.getPayload());
                if(topic.equals("response")) {
                    temperature.setText(msg + "℃");
                    Toast.makeText(BoilerActivity.this, "Success in receiving", Toast.LENGTH_SHORT).show();

                    // save status to preference
                    SharedPreferences.Editor editor = preference.edit();
                    editor.putString("temperature", msg + "℃");
                    editor.commit();

                } else if(topic.equals("check")) {
                    Toast.makeText(BoilerActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        // 기능선택 seekBar 리스너
        mSeekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekArc, int i, boolean b) {
                // 조절 중의 이벤트 처리

                if( i <= 60) {
                    status = RESERVE;
                } else if( i > 60 && i <= 120) {
                    status = WATER;
                } else if( i > 120) {
                    status = ROOM;
                }

                status_val = i;
                mSeekProgress.setText(status);
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {

            }

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {
                // 조절한다음에 손땟을때 이벤트 처리
                // 그 때의 기능값과 i의 값을 저장해놓는다.

                // preference check
                if(preference == null) {
                    Toast.makeText(BoilerActivity.this, "Check Preference", Toast.LENGTH_SHORT).show();
                    return;
                }

                // save status to preference
                SharedPreferences.Editor editor = preference.edit();
                editor.putString("status", status);
                editor.putInt("status_val", status_val);
                editor.commit();

                /**
                 * status에 따라 다른 값들을 보여준다.
                 * 예를 들어 예약모드이면 6h ~ 3h 을 보여주고
                 * 온수와 실온모드라면 온도를 보여준다.
                 */
                setControlStatus(status, progressVal);

                String topic = "func_control";
                String payload = "0";
                if(status.equals(RESERVE)) {
                    payload = "20";
                } else if(status.equals(WATER)) {
                    payload = "90";
                } else if(status.equals(ROOM)) {
                    payload = "180";
                }

                setPublish(client, topic, payload);
            }
        });

        // 난방조절 seekBar 리스너
        mSeekArc2.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekArc, int i, boolean b) {
                setControlStatus(status, i);
                progressVal = i;

            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {

            }

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {
                // 조절한다음에 손땟을때 이벤트 처리
                // 그 때의 선택값과 i의 값을 저장해놓는다.

                // preference check
                if(preference == null) {
                    Toast.makeText(BoilerActivity.this, "Check Preference", Toast.LENGTH_SHORT).show();
                    return;
                }

                // save value to preference
                SharedPreferences.Editor editor = preference.edit();
                editor.putString("controlStatus", controlStatus);
                editor.putInt("progressVal", progressVal);
                editor.commit();

                String topic = "temp_control";
                // 여기서 payload는 각도를 말한다.
                String payload = String.valueOf(progressVal * maxAngle/progress2Max);
                setPublish(client, topic, payload);
            }
        });
    }


    // status에 따라 progress2의 값 컨트롤하는 함수
    public void setControlStatus(String status, int value) {
        if(status.equals(RESERVE)) {
            setControlStatusForReserve(value);
        } else {
            setControlStatusForNonReserve(value);
        }
    }

    // 예약기능에대한 progress2의 text값 변경
    public void setControlStatusForReserve(int value) {
        String msg = "";

        if(value < 5) {
            msg = "外出";
        } else if(value >= 5 && value < progress2Max * 1/3) {
            msg = "6H";
        } else if(value >= progress2Max * 1/3 && value < progress2Max * 2/3) {
            msg = "5H";
        } else {
            msg = "4H";
        }

        // 데이터 저장
        controlStatus = msg;
        mSeekProgress2.setText(msg);

    }


    // 예약기능 외에 대한 progress2의 text값 변경
    public void setControlStatusForNonReserve(int value) {
        String msg = "";
        if(value < 5) {
            msg = "外出";
        } else {
            msg = String.valueOf(value) + "℃";
        }

        // 데이터 저장
        controlStatus = msg;
        mSeekProgress2.setText(msg);
    }

    // set payload
    public void setPayload() {

    }

    // publish
    public void setPublish(MqttAndroidClient client, String topic, String payload) {
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }

        if(topic.equals("temperature")) {
            topic = "response";
        } else if(topic.equals("function_control") || topic.equals("temp_control")) {
            topic = "check";
        }
        subscribe(client, topic);
    }

    // 구독 메소드
    public void subscribe(MqttAndroidClient client, String topic) {
        // subscribe
        String r_topic = topic;
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(r_topic, qos);

            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i("connection","connected");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                    Log.w("connection","disconnected");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

}
