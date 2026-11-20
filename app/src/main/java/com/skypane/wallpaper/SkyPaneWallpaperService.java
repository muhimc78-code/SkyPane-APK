package com.skypane.wallpaper;

import android.app.Presentation;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SkyPaneWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new SkyPaneEngine();
    }

    private class SkyPaneEngine extends Engine {
        private VirtualDisplay virtualDisplay;
        private Presentation presentation;
        private WebView webView;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            cleanup();

            DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
            int density = getResources().getDisplayMetrics().densityDpi;
            int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
                      | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;

            virtualDisplay = dm.createVirtualDisplay(
                    "SkyPaneWallpaper", width, height, density, holder.getSurface(), flags
            );

            presentation = new Presentation(SkyPaneWallpaperService.this, virtualDisplay.getDisplay());
            webView = new WebView(presentation.getContext());

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setGeolocationEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setMediaPlaybackRequiresUserGesture(false);

            webView.setWebViewClient(new WebViewClient());
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                    callback.invoke(origin, true, false);
                }
            });

            webView.loadUrl("file:///android_asset/index.html?wallpaper=1");
            presentation.setContentView(webView);
            presentation.show();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (webView != null) {
                if (visible) {
                    webView.onResume();
                } else {
                    webView.onPause();
                }
            }
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (webView != null) {
                webView.dispatchTouchEvent(event);
            }
            super.onTouchEvent(event);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            cleanup();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            cleanup();
        }

        private void cleanup() {
            if (presentation != null) {
                presentation.dismiss();
                presentation = null;
            }
            if (webView != null) {
                webView.destroy();
                webView = null;
            }
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
        }
    }
}
