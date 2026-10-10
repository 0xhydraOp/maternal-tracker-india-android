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
import android.os.Handler;
import android.os.Looper;
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
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class HospitalPrintManager {
    private HospitalPrintManager() {
    }

    static boolean print(Activity activity, String jobName, String assetPath, boolean landscape) {
        return print(activity, jobName, new String[]{assetPath}, landscape);
    }

    static boolean print(Activity activity, String jobName, String[] assetPaths, boolean landscape) {
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
        manager.print(jobName, new AssetImagePrintAdapter(activity, jobName, assetPaths), attributes);
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

    static File createImagePdf(Context context, String jobName, String[] assetPaths, boolean landscape) throws IOException {
        File directory = new File(context.getCacheDir(), "print_documents");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create the print cache");
        }
        File output = new File(directory, safeFileName(jobName) + ".pdf");
        PrintAttributes.MediaSize mediaSize = landscape
                ? PrintAttributes.MediaSize.ISO_A4.asLandscape()
                : PrintAttributes.MediaSize.ISO_A4.asPortrait();
        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(mediaSize)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
        PrintedPdfDocument document = new PrintedPdfDocument(context, attributes);
        try {
            for (int i = 0; i < assetPaths.length; i++) {
                Bitmap bitmap = null;
                try (InputStream input = context.getAssets().open(assetPaths[i])) {
                    bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap == null) {
                        throw new IOException("The selected hospital form could not be opened");
                    }
                    PdfDocument.Page page = document.startPage(i);
                    drawImage(page.getCanvas(), page.getInfo().getContentRect(), bitmap);
                    document.finishPage(page);
                } finally {
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                }
            }
            try (FileOutputStream stream = new FileOutputStream(output, false)) {
                document.writeTo(stream);
            }
        } finally {
            document.close();
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
        private final String[] assetPaths;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        // Adapter entry points and terminal delivery run on the main thread.
        private final Set<WriteRequest> writes = new HashSet<>();
        private volatile boolean finished;
        private PrintAttributes attributes;

        AssetImagePrintAdapter(Context context, String jobName, String[] assetPaths) {
            this.context = context.getApplicationContext();
            this.jobName = jobName;
            this.assetPaths = assetPaths.clone();
        }

        @Override
        public void onLayout(
                PrintAttributes oldAttributes,
                PrintAttributes newAttributes,
                CancellationSignal cancellationSignal,
                LayoutResultCallback callback,
                Bundle extras
        ) {
            if (finished || cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }
            cancelWrites();
            attributes = newAttributes;
            PrintDocumentInfo info = new PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(assetPaths.length)
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
            cancelWrites();
            if (finished || cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
                return;
            }
            if (attributes == null) {
                callback.onWriteFailed("Print layout is unavailable");
                return;
            }

            final ParcelFileDescriptor ownedDestination;
            try {
                ownedDestination = ParcelFileDescriptor.dup(destination.getFileDescriptor());
            } catch (IOException | RuntimeException error) {
                callback.onWriteFailed("Unable to open print destination");
                return;
            }
            WriteRequest request = new WriteRequest(pages, ownedDestination,
                    cancellationSignal, callback, attributes);
            writes.add(request);
            cancellationSignal.setOnCancelListener(request::cancel);
            try {
                worker.execute(request);
            } catch (RejectedExecutionException error) {
                request.cancel();
                request.complete("Print worker is unavailable", new PageRange[0]);
            }
        }

        @Override
        public void onFinish() {
            finished = true;
            cancelWrites();
            worker.shutdown();
            attributes = null;
        }

        private void cancelWrites() {
            for (WriteRequest request : new ArrayList<>(writes)) {
                request.cancel();
            }
        }

        private final class WriteRequest implements Runnable {
            private final PageRange[] pages;
            private final ParcelFileDescriptor destination;
            private final CancellationSignal signal;
            private final WriteResultCallback callback;
            private final PrintAttributes layout;
            private volatile boolean cancelled;
            private boolean delivered;

            WriteRequest(PageRange[] pages, ParcelFileDescriptor destination,
                    CancellationSignal signal, WriteResultCallback callback,
                    PrintAttributes layout) {
                this.pages = pages == null ? new PageRange[0] : pages.clone();
                this.destination = destination;
                this.signal = signal;
                this.callback = callback;
                this.layout = layout;
            }

            void cancel() {
                cancelled = true;
                // Release our handle without closing the framework-owned descriptor.
                closeDestination();
                if (worker.remove(this)) {
                    complete(null, new PageRange[0]);
                }
            }

            private void closeDestination() {
                try {
                    destination.close();
                } catch (IOException ignored) {
                    // The worker's stream may already have closed this descriptor.
                }
            }

            private void checkCancelled() throws InterruptedIOException {
                if (cancelled || finished || signal.isCanceled()) {
                    throw new InterruptedIOException("Print cancelled");
                }
            }

            @Override
            public void run() {
                PrintedPdfDocument document = null;
                String failure = null;
                List<PageRange> written = new ArrayList<>();
                try {
                    checkCancelled();
                    document = new PrintedPdfDocument(context, layout);
                    for (int i = 0; i < assetPaths.length; i++) {
                        checkCancelled();
                        if (!containsPage(pages, i)) continue;
                        Bitmap bitmap = null;
                        try (InputStream input = context.getAssets().open(assetPaths[i])) {
                            bitmap = BitmapFactory.decodeStream(input);
                            checkCancelled();
                            if (bitmap == null) {
                                throw new IOException("The selected hospital form could not be opened");
                            }
                            PdfDocument.Page page = document.startPage(i);
                            try {
                                drawImage(page.getCanvas(), page.getInfo().getContentRect(), bitmap);
                            } finally {
                                document.finishPage(page);
                            }
                            appendWrittenPage(written, i);
                        } finally {
                            if (bitmap != null) bitmap.recycle();
                        }
                    }
                    checkCancelled();
                    if (!written.isEmpty()) {
                        try (ParcelFileDescriptor.AutoCloseOutputStream raw =
                                     new ParcelFileDescriptor.AutoCloseOutputStream(destination)) {
                            java.io.OutputStream output = new java.io.FilterOutputStream(raw) {
                                @Override public void write(int value) throws IOException {
                                    checkCancelled();
                                    out.write(value);
                                }
                                @Override public void write(byte[] bytes, int offset, int length)
                                        throws IOException {
                                    // Bound each write so cancellation is checked during serialization.
                                    while (length > 0) {
                                        checkCancelled();
                                        int count = Math.min(length, 32 * 1024);
                                        out.write(bytes, offset, count);
                                        offset += count;
                                        length -= count;
                                    }
                                }
                            };
                            document.writeTo(output);
                            output.flush();
                            checkCancelled();
                        }
                    }
                } catch (IOException | RuntimeException | OutOfMemoryError error) {
                    failure = error.getMessage() == null ? "Unable to prepare the print" : error.getMessage();
                } finally {
                    try {
                        if (document != null) document.close();
                    } catch (RuntimeException error) {
                        if (failure == null) failure = "Unable to close print document";
                    } finally {
                        closeDestination();
                        complete(failure, written.toArray(new PageRange[0]));
                    }
                }
            }

            void complete(String failure, PageRange[] written) {
                mainHandler.post(() -> {
                    if (delivered) return;
                    delivered = true;
                    writes.remove(this);
                    // Never unregister inside the cancellation listener itself.
                    signal.setOnCancelListener(null);
                    if (cancelled || finished || signal.isCanceled()) {
                        callback.onWriteCancelled();
                    } else if (failure != null) {
                        callback.onWriteFailed(failure);
                    } else {
                        callback.onWriteFinished(written);
                    }
                });
            }
        }

        private static void appendWrittenPage(List<PageRange> ranges, int page) {
            int last = ranges.size() - 1;
            if (last >= 0 && ranges.get(last).getEnd() + 1 == page) {
                ranges.set(last, new PageRange(ranges.get(last).getStart(), page));
            } else {
                ranges.add(new PageRange(page, page));
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
