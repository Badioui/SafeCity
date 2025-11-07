package com.example.safecity.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.safecity.database.DatabaseHelper;
import com.example.safecity.models.Role;

import java.util.ArrayList;
import java.util.List;

public class RoleDAO {

    private DatabaseHelper dbHelper;

    public RoleDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public long insert(Role role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", role.getNomRole());
        return db.insert("roles", null, cv);
    }

    public Role getById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, name FROM roles WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        if (cursor.moveToFirst()) {
            Role r = new Role(
                    cursor.getInt(0),
                    cursor.getString(1)
            );
            cursor.close();
            return r;
        }
        cursor.close();
        return null;
    }

    public List<Role> getAll() {
        List<Role> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name FROM roles", null);

        while (cursor.moveToNext()) {
            list.add(new Role(
                    cursor.getInt(0),
                    cursor.getString(1)
            ));
        }
        cursor.close();
        return list;
    }
}
