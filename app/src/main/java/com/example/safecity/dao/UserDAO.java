package com.example.safecity.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.safecity.database.DatabaseHelper;
import com.example.safecity.models.Role;
import com.example.safecity.models.Utilisateur;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserDAO {

    private DatabaseHelper dbHelper;
    private RoleDAO roleDAO;

    public UserDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
        roleDAO = new RoleDAO(context);
    }

    public long insert(Utilisateur user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("name", user.getNom());
        cv.put("email", user.getEmail());
        cv.put("password_hash", user.getMotDePasseHash());
        cv.put("role_id", user.getRole().getId());

        return db.insert("users", null, cv);
    }

    public Utilisateur getById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, name, email, password_hash, role_id, created_at FROM users WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        if (cursor.moveToFirst()) {
            Role role = roleDAO.getById(cursor.getInt(4));

            Utilisateur u = new Utilisateur(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    role,
                    new Date()
            );
            cursor.close();
            return u;
        }
        cursor.close();
        return null;
    }

    public List<Utilisateur> getAll() {
        List<Utilisateur> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM users", null);

        while (cursor.moveToNext()) {
            Utilisateur u = getById(cursor.getInt(0));
            list.add(u);
        }
        cursor.close();
        return list;
    }
}
