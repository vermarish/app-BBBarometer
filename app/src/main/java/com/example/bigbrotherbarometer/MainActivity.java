package com.example.bigbrotherbarometer;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.hardware.Sensor;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    SensorManager sm = null;
    TextView textView1 = null;
    List sensors;  // TODO rename

    SensorEventListener sel = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
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


        /* Get a SensorManager instance */
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);

        textView1 = (TextView) findViewById(R.id.textView1);

        sensors = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (sensors.size() > 0) {
            sm.registerListener(sel, (Sensor) sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getBaseContext(), "Error: No Accelerometer.", Toast.LENGTH_LONG).show();
        }
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