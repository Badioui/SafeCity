package com.example.safecity.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.safecity.database.DatabaseHelper;
import com.example.safecity.models.Categorie;
import com.example.safecity.models.Incident;
import com.example.safecity.models.Utilisateur;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class IncidentDAO {

    private DatabaseHelper dbHelper;
    private UserDAO userDAO;
    private CategoryDAO categoryDAO;

    public IncidentDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
        userDAO = new UserDAO(context);
        categoryDAO = new CategoryDAO(context);
    }

    public long insert(Incident inc) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("user_id", inc.getUserId());
        cv.put("category_id", inc.getCategoryId());
        cv.put("description", inc.getDescription());
        cv.put("photo_url", inc.getPhotoUrl());
        cv.put("latitude", inc.getLatitude());
        cv.put("longitude", inc.getLongitude());
        cv.put("status", inc.getStatut());

        return db.insert("incidents", null, cv);
    }

    public Incident getById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, user_id, category_id, description, photo_url, latitude, longitude, status, created_at FROM incidents WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        if (cursor.moveToFirst()) {

            Utilisateur u = userDAO.getById(cursor.getInt(1));
            Categorie c = categoryDAO.getById(cursor.getInt(2));

            Incident inc = new Incident(
                    cursor.getInt(1),
                    cursor.getInt(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getDouble(5),
                    cursor.getDouble(6),
                    cursor.getString(7)
            );

            inc.setId(cursor.getInt(0));
            inc.setUtilisateur(u);
            inc.setCategorie(c);
            inc.setDateSignalement(new Date());

            cursor.close();
            return inc;
        }
        cursor.close();
        return null;
    }

    public List<Incident> getAll() {
        List<Incident> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM incidents", null);

        while (cursor.moveToNext()) {
            Incident i = getById(cursor.getInt(0));
            list.add(i);
        }
        cursor.close();
        return list;
    }
}
