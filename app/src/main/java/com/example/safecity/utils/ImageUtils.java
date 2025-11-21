package com.example.safecity.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    /**
     * Compresse un fichier image existant (ex: photo caméra)
     */
    public static File compressImage(File originalFile) throws IOException {
        Bitmap bitmap = decodeSampledBitmap(originalFile.getAbsolutePath(), 1024, 1024);
        File compressedFile = new File(originalFile.getParent(), "compressed_" + originalFile.getName());
        return saveBitmapToFile(bitmap, compressedFile);
    }

    /**
     * Convertit une URI (ex: galerie) en fichier compressé local
     */
    public static File compressUri(Context context, Uri uri) throws IOException {
        // 1. Lire l'URI en Bitmap
        InputStream input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        if (input != null) input.close();

        // 2. Créer un fichier temporaire
        File storageDir = context.getExternalFilesDir(null);
        File tempFile = File.createTempFile("gallery_upload", ".jpg", storageDir);

        // 3. Sauvegarder/Compresser
        return saveBitmapToFile(bitmap, tempFile);
    }

    private static File saveBitmapToFile(Bitmap bitmap, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        // Compression JPEG à 80% de qualité
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        fos.flush();
        fos.close();
        return file;
    }

    private static Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}