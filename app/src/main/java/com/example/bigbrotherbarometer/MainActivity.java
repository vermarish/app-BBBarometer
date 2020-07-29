/*

Gotta change one line at a time to implement multiple sensors.
Can one SensorEventListener be registered with multiple sensors?
I think so, because that's how the accelerometer sensors work;
there are three distinct sensors, so I should be able to add a fourth easily
by appending to the sensor list... right?
 */

package com.example.bigbrotherbarometer;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.TextView;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.hardware.Sensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    private final int TYPE_TOUCH = -27;
    private AppDatabase db;
    private TidbitDao dao;
    SensorManager sm;
    TextView textView1;
    List<Sensor> sensors;
    boolean recording;
    Set<Tidbit> data;  // TODO optimize List -> Set

    SensorEventListener sel = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            if (recording) {
                int type = event.sensor.getType();
                Tidbit tidbit = new Tidbit(type, event.timestamp, values);
                data.add(tidbit);
            }

            textView1.setText(valueString(values));
        }
        private String valueString(float[] values) {
            if (values.length == 1) {
                return "" + values[0];
            } else if (values.length == 2) {
                return "x: " + values[0] + "\n"
                        + "y: " + values[1];
            } else if (values.length == 3) {
                return "x: " + values[0] + "\n"
                        + "y: " + values[1] + "\n"
                        + "z: " + values[2];
            } else return Arrays.toString(values);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recording = false;
        db = AppDatabase.getInstance(this);
        dao = db.tidbitDao();
        /* Get a SensorManager instance */
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);

        textView1 = (TextView) findViewById(R.id.textView1);

        sensors = new LinkedList<Sensor>();
        sensors.add(sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        sensors.add(sm.getDefaultSensor(Sensor.TYPE_PRESSURE));
        sensors.add(sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

        for (Sensor sensor : sensors) {
            sm.registerListener(sel, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            sm.registerListener(sel, sm.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
            sm.registerListener(sel, sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
        }

        final Button toggle = findViewById(R.id.toggleRecording);
        toggle.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
               toggle();
           }
        });

        final Button logger = findViewById(R.id.logData);
        logger.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                log();
            }
        });

        final RelativeLayout relativeLayout = findViewById(R.id.relativeLayout);
        relativeLayout.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (recording) {
                    Tidbit tidbit = new Tidbit(TYPE_TOUCH, event.getDownTime(),
                            event.getX(), event.getY());
                    System.out.println(tidbit);
                    data.add(tidbit);
                }
                return true;
            }
        });
    }

    /* Create csv file from internal database */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void log() {
        List<Tidbit> tidbits = dao.tidbits();

        String filename = "db " + timeString() + ".csv";

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename);
        String filepath = file.getPath();
        try {
            OutputStream os = new FileOutputStream(file);
            for (Tidbit tidbit : tidbits) {
                String line = tidbit.toString() + "\n";
                os.write(line.getBytes()); // charset unspecified; may not use the correct encoding.
            }
            os.close();
            Toast.makeText(MainActivity.this, tidbits.size() + " tidbits stored to " + filepath,
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.w("ExternalStorage", "Error writing " + file, e);
        }
    }

    /* Returns M-D-YYYY H-M-S
    *  No leading zeros*/
    private String timeString() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        String date = month + "-" + day + "-" + year;
        String time = hour + "-" + minute + "-" + second;

        return date + " " + time;
    }

    private void toggle() {
        Button toggle = findViewById(R.id.toggleRecording);
        recording = !recording;
        if (recording) {
            System.out.println("Recording...");
            data = new TreeSet<>();
            toggle.setText("STOP");
        } else {
            Toast.makeText(MainActivity.this, "Logging..", Toast.LENGTH_SHORT).show();

            // write the data to database
            for (Tidbit tidbit : data) {
                dao.insert(tidbit);
            }
            // empty the data field
            data = null;
            Toast.makeText(MainActivity.this, "Logged!", Toast.LENGTH_SHORT).show();
            toggle.setText("START");
        }
    }


    @Override
    protected void onStop() {
        if (sensors.size() > 0) {
            sm.unregisterListener(sel);
        }
        dao.emptyTables();
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