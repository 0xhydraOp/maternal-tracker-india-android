package com.maternaltracker.india;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Asset-only, size-bounded previews. Never use these bitmaps for printing. */
public final class DocumentPreviewLoader implements AutoCloseable {
    private static final String TAG = "DocumentPreviewLoader";
    private static final int MAX_CACHE_BYTES = 8 * 1024 * 1024;

    private final AssetManager assets;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final LruCache<String, Bitmap> cache;
    private final int cacheBytes;
    private final Object lifecycleLock = new Object();
    // Main-thread-only tokens avoid taking over the caller's ImageView tags.
    private final Map<ImageView, Request> requests = new WeakHashMap<>();
    private volatile boolean closed;

    public DocumentPreviewLoader(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        assets = context.getApplicationContext().getAssets();
        cacheBytes = (int) Math.max(1L,
                Math.min(MAX_CACHE_BYTES, Runtime.getRuntime().maxMemory() / 16L));
        cache = new LruCache<String, Bitmap>(cacheBytes) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getAllocationByteCount();
            }
        };
    }

    /**
     * Call on the main thread with positive pixel bounds, preferably after layout.
     * Clears the old image immediately; failed loads leave the view empty.
     * A null/empty path cancels and clears without decoding. Calls after close are ignored.
     * Layout, color filters, placeholders and accessibility remain caller-owned.
     */
    public void load(ImageView view, String assetPath, int maxWidthPx, int maxHeightPx) {
        requireMainThread();
        if (closed) {
            return;
        }
        if (view == null || maxWidthPx <= 0 || maxHeightPx <= 0) {
            throw new IllegalArgumentException("view and positive preview bounds are required");
        }
        Request previous = requests.remove(view);
        if (previous != null) {
            previous.cancelled = true;
        }
        view.setImageDrawable(null);
        if (assetPath == null || assetPath.isEmpty()) {
            return;
        }

        Request request = new Request();
        requests.put(view, request);
        String key = maxWidthPx + "x" + maxHeightPx + ":" + assetPath;
        Bitmap cached = cache.get(key);
        if (cached != null) {
            view.setImageBitmap(cached);
            requests.remove(view);
            return;
        }

        WeakReference<ImageView> target = new WeakReference<>(view);
        executor.execute(() -> {
            if (closed || request.cancelled || target.get() == null) {
                return;
            }
            Bitmap bitmap = cache.get(key);
            if (bitmap == null) {
                try {
                    bitmap = decode(assetPath, maxWidthPx, maxHeightPx);
                } catch (IOException | RuntimeException | OutOfMemoryError error) {
                    Log.w(TAG, "Unable to decode preview: " + assetPath, error);
                }
            }
            final Bitmap result = bitmap;
            synchronized (lifecycleLock) {
                if (closed || request.cancelled || target.get() == null) {
                    return;
                }
                if (result != null && result.getAllocationByteCount() <= cacheBytes) {
                    cache.put(key, result);
                }
                mainHandler.post(() -> {
                    ImageView currentView = target.get();
                    if (closed || request.cancelled || currentView == null
                            || requests.get(currentView) != request) {
                        return;
                    }
                    requests.remove(currentView);
                    currentView.setImageBitmap(result);
                });
            }
        });
    }

    private Bitmap decode(String assetPath, int maxWidthPx, int maxHeightPx)
            throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream input = assets.open(assetPath)) {
            BitmapFactory.decodeStream(input, null, options);
        }
        if (options.outWidth <= 0 || options.outHeight <= 0
                || closed || Thread.currentThread().isInterrupted()) {
            return null;
        }

        int sample = 1;
        // Round up dimensions so odd-sized images also fit the requested bounds.
        // Limit each decode to the cache budget even for oversized caller bounds.
        while (sample < (1 << 30)) {
            long width = (options.outWidth + (long) sample - 1) / sample;
            long height = (options.outHeight + (long) sample - 1) / sample;
            if (width <= maxWidthPx && height <= maxHeightPx
                    && width * height <= cacheBytes / 4L) {
                break;
            }
            sample *= 2;
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inScaled = false;
        if (closed || Thread.currentThread().isInterrupted()) {
            return null;
        }
        try (InputStream input = assets.open(assetPath)) {
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            if (bitmap != null && (bitmap.getWidth() > maxWidthPx
                    || bitmap.getHeight() > maxHeightPx
                    || bitmap.getAllocationByteCount() > cacheBytes)) {
                bitmap.recycle();
                return null;
            }
            return bitmap;
        }
    }

    /** Call from onDestroy on the main thread. Safe to call more than once. */
    @Override
    public void close() {
        requireMainThread();
        synchronized (lifecycleLock) {
            if (closed) {
                return;
            }
            closed = true;
            executor.shutdownNow();
            mainHandler.removeCallbacksAndMessages(null);
            requests.clear();
            // Eviction must not recycle bitmaps still displayed by an ImageView.
            cache.evictAll();
        }
    }

    private static void requireMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Preview load/close must run on the main thread");
        }
    }

    private static final class Request {
        volatile boolean cancelled;
    }
}
