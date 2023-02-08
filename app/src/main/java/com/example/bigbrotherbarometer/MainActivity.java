package com.example.bigbrotherbarometer;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    // constants
    private final int TYPE_TOUCH = -27;
    private final float DIGLETT_VISIBLE = (float) 1;
    private final float DIGLETT_HIDDEN = (float) 0;
    private final float CIRCLE_VISIBLE = (float) 0.3;
    private final float CIRCLE_HIDDEN = (float) 0;
    private final String[] phaseNames = {"diglett", "10key"};
    private final String DARK_TEXT = "#333333";
    private final String LIGHT_TEXT = "#BBBBBB";

    // initialized variables
    private int numInputs = 0;
    private boolean recording = false;
    private int currentPhaseID = 0;

    // Views and widgets
    private RelativeLayout relativeLayout;
    private TextView inputCounter;
    private Button phaseSwitch;
    private TextView currentPhase;
    private Button toggleRecording;
    private ImageView field;
    private ImageView[] digletts;
    private Set<ImageView> hiddenDigletts;

    // objects
    private AppDatabase db;
    private TidbitDao dao;
    SensorManager sm;
    List<Sensor> sensors;
    Set<Tidbit> data;

    SensorEventListener sel = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            if (recording) {
                int type = event.sensor.getType();
                Tidbit tidbit = new Tidbit(type, event.timestamp, values);
                data.add(tidbit);
            }
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

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get widgets, store as field variables
        this.relativeLayout = findViewById(R.id.relativeLayout);
        this.inputCounter = findViewById(R.id.inputCounter);
        this.phaseSwitch = findViewById(R.id.phaseSwitch);
        this.currentPhase = findViewById(R.id.currentPhase);
        this.toggleRecording = findViewById(R.id.toggleRecording);
        this.field = findViewById(R.id.field);
        this.digletts = new ImageView[10];
        this.hiddenDigletts = new HashSet<ImageView>();
        for (int i = 0; i < 10; i++) {
            String diglettID = "diglett" + i;
            int resid = getResources().getIdentifier(diglettID, "id", getPackageName());
            ImageView diglett = (ImageView) findViewById(resid);
            this.digletts[i] = diglett;
            this.hiddenDigletts.add(diglett);
        }

        // initialize database
        db = AppDatabase.getInstance(this);
        dao = db.tidbitDao();

        // Get sensors and register sensor listeners
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensors = new LinkedList<Sensor>();
        int[] sensorCodes = {Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_PRESSURE, Sensor.TYPE_GYROSCOPE};
        for (int sensorCode : sensorCodes) {
            Sensor currSensor = sm.getDefaultSensor(sensorCode);
            sensors.add(currSensor);
            sm.registerListener(sel, currSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        // Set listener to capture all touch input
        relativeLayout.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (recording) {
                    // event.getDownTime() uses a different clock than the sensor clock. That's okay,
                    // we can just fix this in R using two clocking tidbits.
                    Tidbit tidbit = new Tidbit(TYPE_TOUCH,  event.getDownTime(),
                            event.getX(), event.getY());

                    data.add(tidbit);
                }
                return true;
            }
        });

        // Create phase button, start with first phase
        phaseSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switchPhase();
            }
        });
        switchPhaseTo(0);

        // Create toggle button
        toggleRecording.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
               toggle();
           }
        });

        // Make digletts disappear when touched
        for (int i = 0; i < 10; i++) {
            this.digletts[i].setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    double alpha = v.getAlpha();
                    if (alpha != DIGLETT_HIDDEN) {
                        whackDiglett((ImageView) v);
                    }
                    if (recording) {
                        Tidbit tidbit = new Tidbit(TYPE_TOUCH, event.getDownTime(),
                                event.getX(), event.getY());
                        data.add(tidbit);
                    }
                    return true;
                }
            });
        }
    }


    private void switchPhase() {
        currentPhaseID = (currentPhaseID + 1) % phaseNames.length;
        switchPhaseTo(currentPhaseID);
    }

    private void switchPhaseTo(int phaseID) {
        String phase = phaseNames[phaseID];
        switch(phase) {
            case "diglett":
                this.relativeLayout.setBackgroundResource(R.drawable.grass);
                this.currentPhase.setText(R.string.collecting);
                this.currentPhase.setTextColor(Color.parseColor(DARK_TEXT));
                this.inputCounter.setTextColor(Color.parseColor("#F8F8F8"));
                this.field.setAlpha((float) 0);
                for (int i = 0; i < 10; i++) {
                    this.digletts[i].setImageResource(this.getResources().getIdentifier("diglett", "drawable", this.getPackageName()));
                }
                break;
            case "10key":
                this.relativeLayout.setBackgroundColor(Color.parseColor("#080808"));
                this.currentPhase.setText(R.string.cracking);
                this.currentPhase.setTextColor(Color.parseColor(LIGHT_TEXT));
                // this.inputCounter.setTextColor(Color.parseColor(LIGHT_TEXT));
                this.inputCounter.setAlpha(0);  // input counter looks too "game-y" when cracking
                this.field.setAlpha((float) 1);
                for (int i = 0; i < 10; i++) {
                    this.digletts[i].setImageResource(this.getResources().getIdentifier("circle", "drawable", this.getPackageName()));
                }
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void toggle() {
        recording = !recording;
        if (recording) {
            data = new TreeSet<>();
            timestamp();
            this.toggleRecording.setText("STOP");
            Thread game = new Game();
            game.start();
        } else {
            Toast.makeText(MainActivity.this, "Logging..", Toast.LENGTH_SHORT).show();
            timestamp();
            // write the data to database
            for (Tidbit tidbit : data) {
                dao.insert(tidbit);
            }
            // empty the data field
            data = null;
            // Toast.makeText(MainActivity.this, "Logged!", Toast.LENGTH_SHORT).show();
            this.toggleRecording.setText("START");
            this.log();
        }
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
            String firstline = "type,time,one,two,three\n";
            os.write(firstline.getBytes());
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void spawnDiglett() {
        if (hiddenDigletts.size() > 0) {
            ImageView nextDiglett = hiddenDigletts.stream().skip(new Random().nextInt(hiddenDigletts.size())).findFirst().orElse(null);
            if (nextDiglett != null) {
                if (currentPhaseID == 0) {
                    nextDiglett.setAlpha(DIGLETT_VISIBLE);
                } else {
                    nextDiglett.setAlpha(CIRCLE_VISIBLE);
                }
                hiddenDigletts.remove(nextDiglett);
            }
        }
    }

    public void whackDiglett(ImageView diglett) {
        diglett.setAlpha(DIGLETT_HIDDEN);
        hiddenDigletts.add(diglett);
        numInputs++;
        inputCounter.setAlpha((float) 0.85);
        inputCounter.setText(Integer.toString(numInputs));
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void timestamp() {
        Tidbit millis = new Tidbit(-1, SystemClock.uptimeMillis(), 0);
        Tidbit nanos = new Tidbit(-2, SystemClock.elapsedRealtimeNanos(), 0);
        data.add(millis);
        data.add(nanos);
    }

    private class Game extends Thread {
        private Random rand = new Random();
        // private ImageView[] digletts;

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            while (recording) {
                this.snooze();
                if (recording) {
                    spawnDiglett();
                }
            }
        }
        private void snooze() {
            double duration = 1000*(rexp(1));
            try {
                Thread.sleep((long) duration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // returns duration in seconds
        private double rexp(long lambda) {
            double exp_value = Math.log(1-rand.nextDouble())/(-lambda);
            return Math.max(Math.min(exp_value, 2), 0.1);
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