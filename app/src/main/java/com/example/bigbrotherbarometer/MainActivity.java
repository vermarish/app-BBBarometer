/*

Gotta change one line at a time to implement multiple sensors.
Can one SensorEventListener be registered with multiple sensors?
I think so, because that's how the accelerometer sensors work;
there are three distinct sensors, so I should be able to add a fourth easily
by appending to the sensor list... right?
 */

package com.example.bigbrotherbarometer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.hardware.Sensor;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    SensorManager sm;
    TextView textView1;
    List<Sensor> sensors;
    boolean recording;
    List<Tidbit> data;

    SensorEventListener sel = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            if (recording) {
                int type = event.sensor.getType();
                Tidbit tidbit = new Tidbit(type, event.timestamp, values);
                data.add(tidbit);
            }
            String display = "x: " + values[0] + "\n"
                           + "y: " + values[1] + "\n"
                           + "z: " + values[2];

            textView1.setText(display);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recording = false;
        /* Get a SensorManager instance */
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);

        textView1 = (TextView) findViewById(R.id.textView1);

        sensors = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(sel, (Sensor) sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);

        // TODO create database

        final Button button = findViewById(R.id.toggleRecording);
        button.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
               recording = !recording;
               if (recording) {
                   data = new LinkedList<>();
               } else {
                   textView1.setText("Logging...");
                   TidbitDao dao = db.tidbitDao();
                   for (Tidbit tidbit : data) {

                   }
                   // write the data to database
                   // empty the data field
                   textView1.setText("Logged!");
               }
           }
        });
    }

    @Override
    protected void onStop() {
        if (sensors.size() > 0) {
            sm.unregisterListener(sel);
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
}