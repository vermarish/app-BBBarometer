package com.example.bigbrotherbarometer;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TidbitDao {
    @Query("SELECT * FROM tidbit")
    List<Tidbit> tidbits();

    @Query("SELECT * FROM tidbit LIMIT :n")
    List<Tidbit> tidbits(int n);

    @Insert
    void insertAll(Tidbit... tidbits);

    @Insert
    void insert(Tidbit tidbit);

    @Query("DELETE FROM tidbit WHERE 1=1")
    void emptyTables();
}
