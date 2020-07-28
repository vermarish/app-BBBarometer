package com.example.bigbrotherbarometer;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

@Entity(tableName = "tidbit",
        primaryKeys = {"sensor_type", "time"},
        indices = {@Index(value = "time")})
public class Tidbit implements Comparable<Tidbit> {
    @ColumnInfo(name = "sensor_type")
    public int sensorType;

    public long time;
    public float data1;
    public float data2;
    public float data3;

    public Tidbit() {
    }

    @Ignore
    public Tidbit(int sensorType, long time, float data1, float data2, float data3) {
        this.sensorType = sensorType;
        this.time = time;
        this.data1 = data1;
        this.data2 = data2;
        this.data3 = data3;
    }

    @Ignore
    public Tidbit(int sensorType, long time, float data1, float data2) {
        this.sensorType = sensorType;
        this.time = time;
        this.data1 = data1;
        this.data2 = data2;
    }

    @Ignore
    public Tidbit(int sensorType, long time, float data1) {
        this.sensorType = sensorType;
        this.time = time;
        this.data1 = data1;
    }

    @Ignore
    public Tidbit(int sensorType, long time, float[] data) {
        this.data1 = data[0];
        if (data.length >= 2) {
            this.data2 = data[1];
        } else {
            this.data2 = 0;
        }
        if (data.length == 3) {
            this.data3 = data[2];
        } else {
            this.data3 = 0;
        }
        this.sensorType = sensorType;
        this.time = time;
    }

    public int compareTo(Tidbit other) {
        return (int) (this.getTime() - other.getTime());
    }

    public int getSensorType() {
        return sensorType;
    }
    // TODO do I even need setter methods?
    public void setSensorType(int sensorType) {
        this.sensorType = sensorType;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public float getData1() {
        return data1;
    }

    public void setData1(float data1) {
        this.data1 = data1;
    }

    public float getData2() {
        return data2;
    }

    public void setData2(float data2) {
        this.data2 = data2;
    }

    public float getData3() {
        return data3;
    }

    public void setData3(float data3) {
        this.data3 = data3;
    }

    @Override
    public String toString() {
        return sensorType +
                "," + time +
                "," + data1 +
                "," + data2 +
                "," + data3;
    }
}