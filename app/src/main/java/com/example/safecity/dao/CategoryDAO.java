package com.example.safecity.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.safecity.database.DatabaseHelper;
import com.example.safecity.models.Categorie;

import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    private DatabaseHelper dbHelper;

    public CategoryDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public long insert(Categorie cat) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", cat.getNom());
        return db.insert("categories", null, cv);
    }

    public Categorie getById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, name FROM categories WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        if (cursor.moveToFirst()) {
            Categorie c = new Categorie(
                    cursor.getInt(0),
                    cursor.getString(1)
            );
            cursor.close();
            return c;
        }
        cursor.close();
        return null;
    }

    public List<Categorie> getAll() {
        List<Categorie> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name FROM categories", null);

        while (cursor.moveToNext()) {
            list.add(new Categorie(
                    cursor.getInt(0),
                    cursor.getString(1)
            ));
        }
        cursor.close();
        return list;
    }
}
