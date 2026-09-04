package com.maternaltracker.india;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;

import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

final class HospitalPrintManager {
    private HospitalPrintManager() {
    }

    static boolean print(Activity activity, String jobName, String assetPath, boolean landscape) {
        PrintManager manager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
        if (manager == null) {
            return false;
        }
        PrintAttributes.MediaSize mediaSize = landscape
                ? PrintAttributes.MediaSize.ISO_A4.asLandscape()
                : PrintAttributes.MediaSize.ISO_A4.asPortrait();
        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(mediaSize)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
        manager.print(jobName, new AssetImagePrintAdapter(activity, jobName, assetPath), attributes);
        return true;
    }

    static File copyOriginalImage(Context context, String jobName, String assetPath) throws IOException {
        File directory = new File(context.getCacheDir(), "print_documents");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create the print cache");
        }
        File output = new File(directory, safeFileName(jobName) + ".jpg");
        byte[] buffer = new byte[32 * 1024];
        try (InputStream input = context.getAssets().open(assetPath);
             FileOutputStream stream = new FileOutputStream(output, false)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                stream.write(buffer, 0, read);
            }
            stream.flush();
        }
        return output;
    }

    private static String safeFileName(String name) {
        String safe = name == null ? "hospital-document" : name.trim();
        safe = safe.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", " ");
        return safe.isEmpty() ? "hospital-document" : safe;
    }

    private static void drawImage(Canvas canvas, Rect contentRect, Bitmap bitmap) {
        canvas.drawColor(Color.WHITE);
        float scale = Math.min(
                contentRect.width() / (float) bitmap.getWidth(),
                contentRect.height() / (float) bitmap.getHeight()
        );
        float width = bitmap.getWidth() * scale;
        float height = bitmap.getHeight() * scale;
        float left = contentRect.left + (contentRect.width() - width) / 2f;
        float top = contentRect.top + (contentRect.height() - height) / 2f;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, null, new RectF(left, top, left + width, top + height), paint);
    }

    private static final class AssetImagePrintAdapter extends PrintDocumentAdapter {
        private final Context context;
        private final String jobName;
        private final String assetPath;
        private PrintAttributes attributes;

        AssetImagePrintAdapter(Context context, String jobName, String assetPath) {
            this.context = context.getApplicationContext();
            this.jobName = jobName;
            this.assetPath = assetPath;
        }

        @Override
        public void onLayout(
                PrintAttributes oldAttributes,
                PrintAttributes newAttributes,
                CancellationSignal cancellationSignal,
                LayoutResultCallback callback,
                Bundle extras
        ) {
            if (cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }
            attributes = newAttributes;
            PrintDocumentInfo info = new PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build();
            callback.onLayoutFinished(info, !newAttributes.equals(oldAttributes));
        }

        @Override
        public void onWrite(
                PageRange[] pages,
                ParcelFileDescriptor destination,
                CancellationSignal cancellationSignal,
                WriteResultCallback callback
        ) {
            if (cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
                return;
            }
            if (!containsPage(pages, 0)) {
                callback.onWriteFinished(new PageRange[0]);
                return;
            }
            if (attributes == null) {
                callback.onWriteFailed("Print layout is unavailable");
                return;
            }

            PrintedPdfDocument document = new PrintedPdfDocument(context, attributes);
            Bitmap bitmap = null;
            try (InputStream input = context.getAssets().open(assetPath)) {
                bitmap = BitmapFactory.decodeStream(input);
                if (bitmap == null) {
                    callback.onWriteFailed("The selected hospital form could not be opened");
                    return;
                }
                PdfDocument.Page page = document.startPage(0);
                drawImage(page.getCanvas(), page.getInfo().getContentRect(), bitmap);
                document.finishPage(page);
                try (FileOutputStream output = new FileOutputStream(destination.getFileDescriptor())) {
                    document.writeTo(output);
                }
                callback.onWriteFinished(new PageRange[]{new PageRange(0, 0)});
            } catch (IOException | RuntimeException error) {
                callback.onWriteFailed(error.getMessage() == null ? "Unable to prepare the print" : error.getMessage());
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                document.close();
            }
        }

        private static boolean containsPage(PageRange[] ranges, int page) {
            if (ranges == null) {
                return false;
            }
            for (PageRange range : ranges) {
                if (range != null && page >= range.getStart() && page <= range.getEnd()) {
                    return true;
                }
            }
            return false;
        }
    }
}
