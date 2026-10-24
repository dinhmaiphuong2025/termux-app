package com.termux.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.termux.R;
import com.termux.app.api.file.FileReceiverActivity;
import com.termux.app.terminal.TerminalBorderDrawable;
import com.termux.app.terminal.LiquidGlassButtonDrawable;
import com.termux.app.terminal.TermuxActivityRootView;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.io.TermuxTerminalExtraKeys;
import com.termux.shared.activities.ReportActivity;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.android.PermissionUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY;
import com.termux.app.activities.HelpActivity;
import com.termux.app.activities.SettingsActivity;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.io.TerminalToolbarViewPager;
import com.termux.app.terminal.TermuxTerminalViewClient;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.theme.TermuxThemeUtils;
import com.termux.shared.theme.NightMode;
import com.termux.shared.view.ViewUtils;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.terminal.TextStyle;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.termux.app.terminal.TermuxTabBarController;
import com.termux.app.terminal.TermuxGesturePillView;
import com.google.android.material.card.MaterialCardView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A terminal emulator activity.
 * <p/>
 * See
 * <ul>
 * <li>http://www.mongrel-phones.com.au/default/how_to_make_a_local_service_and_bind_to_it_in_android</li>
 * <li>https://code.google.com/p/android/issues/detail?id=6426</li>
 * </ul>
 * about memory leaks.
 */
public final class TermuxActivity extends AppCompatActivity implements ServiceConnection {

    /**
     * The connection to the {@link TermuxService}. Requested in {@link #onCreate(Bundle)} with a call to
     * {@link #bindService(Intent, ServiceConnection, int)}, and obtained and stored in
     * {@link #onServiceConnected(ComponentName, IBinder)}.
     */
    TermuxService mTermuxService;

    /**
     * The {@link TerminalView} shown in  {@link TermuxActivity} that displays the terminal.
     */
    TerminalView mTerminalView;

    /**
     *  The {@link TerminalViewClient} interface implementation to allow for communication between
     *  {@link TerminalView} and {@link TermuxActivity}.
     */
    TermuxTerminalViewClient mTermuxTerminalViewClient;

    /**
     *  The {@link TerminalSessionClient} interface implementation to allow for communication between
     *  {@link TerminalSession} and {@link TermuxActivity}.
     */
    TermuxTerminalSessionActivityClient mTermuxTerminalSessionActivityClient;

    /**
     * Termux app shared preferences manager.
     */
    private TermuxAppSharedPreferences mPreferences;

    /**
     * Termux app SharedProperties loaded from termux.properties
     */
    private TermuxAppSharedProperties mProperties;

    /**
     * The root view of the {@link TermuxActivity}.
     */
    TermuxActivityRootView mTermuxActivityRootView;

    /**
     * The space at the bottom of {@link @mTermuxActivityRootView} of the {@link TermuxActivity}.
     */
    View mTermuxActivityBottomSpaceView;

    /**
     * The terminal extra keys view.
     */
    ExtraKeysView mExtraKeysView;

    /**
     * The client for the {@link #mExtraKeysView}.
     */
    TermuxTerminalExtraKeys mTermuxTerminalExtraKeys;

    /**
     * The termux sessions list controller.
     */
    TermuxSessionsListViewController mTermuxSessionListViewController;

    MaterialCardView mTerminalCardContainer;
    MaterialCardView mTerminalToolbarCard;
    ImageView mTerminalBackgroundImage;
    ImageView mTerminalGlassBackdrop;
    View mTerminalFrameLayer;
    ImageView mTerminalCardSnapshot;
    ImageView mTerminalCardIncomingSnapshot;
    Bitmap mTransitionSnapshotBitmapA;
    Bitmap mTransitionSnapshotBitmapB;
    View mTerminalBackgroundOverlay;
    View mTerminalBorderOverlay;
    TerminalBorderDrawable mTerminalBorderDrawable;
    View mTerminalGlassTint;
    ImageView mTerminalToolbarBackgroundImage;
    View mTerminalToolbarBackgroundOverlay;
    View mTerminalToolbarGlassTint;
    ImageView mActivityBackgroundImage;
    View mActivityBackgroundOverlay;
    FrameLayout mTerminalTabBarLayout;
    ImageView mTerminalTabBarBackgroundImage;

    /** Last decoded background bitmap and blur radius, kept for re-applying layout matrices. */
    Bitmap mLastBackgroundBitmap;
    int mLastBackgroundBlurRadius;
    float mLastVeilAlpha = 0.35f;
    /** Temporary diagnostic signature for the right-edge column investigation. */
    String mLastGlassDebugSig = "";
    /**
     * Tracks layout changes of the activity scene and the extra keys bar so the
     * wallpaper sync slices and the glass backdrop follow their real positions in
     * real time (e.g. while the soft keyboard opens/closes).
     */
    private final View.OnLayoutChangeListener mLiveWallpaperSyncListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
            int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (isFinishing() || mLastBackgroundBitmap == null || getPreferences() == null) return;
            float density = getResources().getDisplayMetrics().density;
            if (mTerminalCardContainer != null) mTerminalCardContainer.invalidateOutline();
            if (mTerminalToolbarCard != null) mTerminalToolbarCard.invalidateOutline();
            if (mTerminalTabBarLayout != null) mTerminalTabBarLayout.invalidateOutline();
            if (isBarBackgroundEnabled()) {
                applyToolbarBackgroundSlice(mLastBackgroundBitmap, mLastBackgroundBlurRadius,
                    mLastBackgroundBlurRadius * density);
            } else if (!getPreferences().isGlassEffectEnabled()) {
                applyFixedSceneMatrix(mTerminalBackgroundImage, mLastBackgroundBitmap);
            }
            if (isTabBarBackgroundEnabled()) {
                applyTabBarBackgroundSlice(mLastBackgroundBitmap, mLastBackgroundBlurRadius,
                    mLastBackgroundBlurRadius * density);
            }
            if (getPreferences().isGlassEffectEnabled()) {
                applyGlassBackdrop(mLastBackgroundBitmap, getGlassBlurPx(density));
            }
            if (mActivityBackgroundImage != null && mActivityBackgroundImage.getVisibility() == View.VISIBLE) {
                applyFixedSceneMatrix(mActivityBackgroundImage, mLastBackgroundBitmap);
            }
        }
    };
    /** Last toolbar card height applied to the toolbar background views. */
    private int mLastToolbarBackgroundHeight;

    /** Small LRU cache of decoded (and on old APIs, pre-blurred) background bitmaps. */
    final Map<String, Bitmap> mBackgroundBitmapCache = new LinkedHashMap<String, Bitmap>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) {
            if (size() > 4) {
                if (eldest.getValue() != null && !eldest.getValue().isRecycled()) {
                    eldest.getValue().recycle();
                }
                return true;
            }
            return false;
        }
    };
    TermuxTabBarController mTermuxTabBarController;
    TermuxGesturePillView mTermuxGesturePillView;

    private android.animation.ValueAnimator mTabTransitionAnimator;
    private Bitmap mTransitionSnapshotBitmap;

    /**
     * The {@link TermuxActivity} broadcast receiver for various things like terminal style configuration changes.
     */
    private final BroadcastReceiver mTermuxActivityBroadcastReceiver = new TermuxActivityBroadcastReceiver();

    /**
     * The last toast shown, used cancel current toast before showing new in {@link #showToast(String, boolean)}.
     */
    Toast mLastToast;

    /**
     * If between onResume() and onStop(). Note that only one session is in the foreground of the terminal view at the
     * time, so if the session causing a change is not in the foreground it should probably be treated as background.
     */
    private boolean mIsVisible;

    private boolean mPendingReloadStyling = false;

    /**
     * If onResume() was called after onCreate().
     */
    private boolean mIsOnResumeAfterOnCreate = false;

    /**
     * If activity was restarted like due to call to {@link #recreate()} after receiving
     * {@link TERMUX_ACTIVITY#ACTION_RELOAD_STYLE}, system dark night mode was changed or activity
     * was killed by android.
     */
    private boolean mIsActivityRecreated = false;

    /**
     * The {@link TermuxActivity} is in an invalid state and must not be run.
     */
    private boolean mIsInvalidState;

    private int mNavBarHeight;

    private float mTerminalToolbarDefaultHeight;


    private static final int CONTEXT_MENU_SELECT_URL_ID = 0;
    private static final int CONTEXT_MENU_SHARE_TRANSCRIPT_ID = 1;
    private static final int CONTEXT_MENU_SHARE_SELECTED_TEXT = 10;
    private static final int CONTEXT_MENU_AUTOFILL_USERNAME = 11;
    private static final int CONTEXT_MENU_AUTOFILL_PASSWORD = 2;
    private static final int CONTEXT_MENU_RESET_TERMINAL_ID = 3;
    private static final int CONTEXT_MENU_KILL_PROCESS_ID = 4;
    private static final int CONTEXT_MENU_STYLING_ID = 5;
    private static final int CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON = 6;
    private static final int CONTEXT_MENU_HELP_ID = 7;
    private static final int CONTEXT_MENU_SETTINGS_ID = 8;
    private static final int CONTEXT_MENU_REPORT_ID = 9;

    private static final String ARG_TERMINAL_TOOLBAR_TEXT_INPUT = "terminal_toolbar_text_input";
    private static final String ARG_ACTIVITY_RECREATED = "activity_recreated";

    private static final String LOG_TAG = "TermuxActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.logDebug(LOG_TAG, "onCreate");
        mIsOnResumeAfterOnCreate = true;

        if (savedInstanceState != null)
            mIsActivityRecreated = savedInstanceState.getBoolean(ARG_ACTIVITY_RECREATED, false);

        // Delete ReportInfo serialized object files from cache older than 14 days
        ReportActivity.deleteReportInfoFilesOlderThanXDays(this, 14, false);

        // Load Termux app SharedProperties from disk
        mProperties = TermuxAppSharedProperties.getProperties();
        reloadProperties();

        setActivityTheme();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_termux);

        // Load termux shared preferences
        // This will also fail if TermuxConstants.TERMUX_PACKAGE_NAME does not equal applicationId
        mPreferences = TermuxAppSharedPreferences.build(this, true);
        if (mPreferences == null) {
            // An AlertDialog should have shown to kill the app, so we don't continue running activity code
            mIsInvalidState = true;
            return;
        }

        setMargins();

        mTermuxActivityRootView = findViewById(R.id.activity_termux_root_view);
        mTermuxActivityRootView.setActivity(this);
        mTermuxActivityBottomSpaceView = findViewById(R.id.activity_termux_bottom_space_view);
        mTermuxActivityRootView.setOnApplyWindowInsetsListener(new TermuxActivityRootView.WindowInsetsListener());

        View content = findViewById(android.R.id.content);
        content.setOnApplyWindowInsetsListener((v, insets) -> {
            mNavBarHeight = insets.getSystemWindowInsetBottom();
            return insets;
        });

        if (mProperties.isUsingFullScreen()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setTermuxTerminalViewAndClients();

        setTerminalToolbarView(savedInstanceState);

        setSettingsButtonView();

        setNewSessionButtonView();

        setToggleKeyboardView();

        registerForContextMenu(mTerminalView);

        FileReceiverActivity.updateFileReceiverActivityComponentsState(this);

        try {
            // Start the {@link TermuxService} and make it run regardless of who is bound to it
            Intent serviceIntent = new Intent(this, TermuxService.class);
            startService(serviceIntent);

            // Attempt to bind to the service, this will call the {@link #onServiceConnected(ComponentName, IBinder)}
            // callback if it succeeds.
            if (!bindService(serviceIntent, this, 0))
                throw new RuntimeException("bindService() failed");
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG,"TermuxActivity failed to start TermuxService", e);
            Logger.showToast(this,
                getString(e.getMessage() != null && e.getMessage().contains("app is in background") ?
                    R.string.error_termux_service_start_failed_bg : R.string.error_termux_service_start_failed_general),
                true);
            mIsInvalidState = true;
            return;
        }

        // Send the {@link TermuxConstants#BROADCAST_TERMUX_OPENED} broadcast to notify apps that Termux
        // app has been opened.
        TermuxUtils.sendTermuxOpenedBroadcast(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        Logger.logDebug(LOG_TAG, "onStart");

        if (mIsInvalidState) return;

        mIsVisible = true;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStart();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStart();

        if (mPreferences.isTerminalMarginAdjustmentEnabled())
            addTermuxActivityRootViewGlobalLayoutListener();

        registerTermuxActivityBroadcastReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();

        Logger.logVerbose(LOG_TAG, "onResume");

        if (mIsInvalidState) return;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onResume();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onResume();

        if (mPendingReloadStyling) {
            mPendingReloadStyling = false;
            reloadActivityStyling(false);
        } else {
            updateTerminalAppearance();
        }

        // Check if a crash happened on last run of the app or if a plugin crashed and show a
        // notification with the crash details if it did
        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(this, LOG_TAG);

        mIsOnResumeAfterOnCreate = false;
    }

    @Override
    protected void onStop() {
        super.onStop();

        Logger.logDebug(LOG_TAG, "onStop");

        if (mIsInvalidState) return;

        mIsVisible = false;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStop();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStop();

        removeTermuxActivityRootViewGlobalLayoutListener();

        unregisterTermuxActivityBroadcastReceiver();
        DrawerLayout drawer = getDrawer();
        if (drawer != null) drawer.closeDrawers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Logger.logDebug(LOG_TAG, "onDestroy");

        cleanupTransitionSnapshot();

        if (mIsInvalidState) return;

        if (mTermuxService != null) {
            // Do not leave service and session clients with references to activity.
            mTermuxService.unsetTermuxTerminalSessionClient();
            mTermuxService = null;
        }

        try {
            unbindService(this);
        } catch (Exception e) {
            // ignore.
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        Logger.logVerbose(LOG_TAG, "onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);
        saveTerminalToolbarTextInput(savedInstanceState);
        savedInstanceState.putBoolean(ARG_ACTIVITY_RECREATED, true);
    }





    /**
     * Part of the {@link ServiceConnection} interface. The service is bound with
     * {@link #bindService(Intent, ServiceConnection, int)} in {@link #onCreate(Bundle)} which will cause a call to this
     * callback method.
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Logger.logDebug(LOG_TAG, "onServiceConnected");

        mTermuxService = ((TermuxService.LocalBinder) service).service;

        setTermuxSessionsListView();

        final Intent intent = getIntent();
        setIntent(null);

        if (mTermuxService.isTermuxSessionsEmpty()) {
            if (mIsVisible) {
                TermuxInstaller.setupBootstrapIfNeeded(TermuxActivity.this, () -> {
                    if (mTermuxService == null) return; // Activity might have been destroyed.
                    try {
                        boolean launchFailsafe = false;
                        if (intent != null && intent.getExtras() != null) {
                            launchFailsafe = intent.getExtras().getBoolean(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
                        }
                        mTermuxTerminalSessionActivityClient.addNewSession(launchFailsafe, null);
                    } catch (WindowManager.BadTokenException e) {
                        // Activity finished - ignore.
                    }
                });
            } else {
                // The service connected while not in foreground - just bail out.
                finishActivityIfNotFinishing();
            }
        } else {
            // If termux was started from launcher "New session" shortcut and activity is recreated,
            // then the original intent will be re-delivered, resulting in a new session being re-added
            // each time.
            if (!mIsActivityRecreated && intent != null && Intent.ACTION_RUN.equals(intent.getAction())) {
                // Android 7.1 app shortcut from res/xml/shortcuts.xml.
                boolean isFailSafe = intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
                mTermuxTerminalSessionActivityClient.addNewSession(isFailSafe, null);
            } else {
                mTermuxTerminalSessionActivityClient.setCurrentSession(mTermuxTerminalSessionActivityClient.getCurrentStoredSessionOrLast());
            }
        }

        // Update the {@link TerminalSession} and {@link TerminalEmulator} clients.
        mTermuxService.setTermuxTerminalSessionClient(mTermuxTerminalSessionActivityClient);

        updateTerminalAppearance();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Logger.logDebug(LOG_TAG, "onServiceDisconnected");

        // Respect being stopped from the {@link TermuxService} notification action.
        finishActivityIfNotFinishing();
    }






    private void reloadProperties() {
        mProperties.loadTermuxPropertiesFromDisk();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadProperties();
    }



    private void setActivityTheme() {
        // Update NightMode.APP_NIGHT_MODE
        TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());

        // Set activity night mode. If NightMode.SYSTEM is set, then android will automatically
        // trigger recreation of activity when uiMode/dark mode configuration is changed so that
        // day or night theme takes affect.
        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);
    }

    private void setMargins() {
        RelativeLayout relativeLayout = findViewById(R.id.activity_termux_root_relative_layout);
        int marginHorizontal = mPreferences != null ? mPreferences.getTerminalHorizontalMargin() : mProperties.getTerminalMarginHorizontal();
        int marginVertical = mProperties.getTerminalMarginVertical();
        ViewUtils.setLayoutMarginsInDp(relativeLayout, marginHorizontal, marginVertical, marginHorizontal, marginVertical);
    }



    public void addTermuxActivityRootViewGlobalLayoutListener() {
        getTermuxActivityRootView().getViewTreeObserver().addOnGlobalLayoutListener(getTermuxActivityRootView());
    }

    public void removeTermuxActivityRootViewGlobalLayoutListener() {
        if (getTermuxActivityRootView() != null)
            getTermuxActivityRootView().getViewTreeObserver().removeOnGlobalLayoutListener(getTermuxActivityRootView());
    }



    private void setTermuxTerminalViewAndClients() {
        // Set termux terminal view and session clients
        mTermuxTerminalSessionActivityClient = new TermuxTerminalSessionActivityClient(this);
        mTermuxTerminalViewClient = new TermuxTerminalViewClient(this, mTermuxTerminalSessionActivityClient);

        // Set termux terminal view
        mTerminalView = findViewById(R.id.terminal_view);
        mTerminalView.setTerminalViewClient(mTermuxTerminalViewClient);

        mTerminalCardContainer = findViewById(R.id.terminal_card_container);
        mTerminalCardSnapshot = findViewById(R.id.terminal_card_snapshot);
        mTerminalCardIncomingSnapshot = findViewById(R.id.terminal_card_incoming_snapshot);
        mTerminalBackgroundImage = findViewById(R.id.terminal_background_image);
        mTerminalGlassBackdrop = findViewById(R.id.terminal_glass_backdrop);
        mTerminalFrameLayer = findViewById(R.id.terminal_frame_layer);
        mTerminalBackgroundOverlay = findViewById(R.id.terminal_background_overlay);
        mTerminalToolbarCard = findViewById(R.id.terminal_toolbar_card);
        mTerminalToolbarCard.addOnLayoutChangeListener(mLiveWallpaperSyncListener);
        if (mTerminalCardContainer != null) {
            mTerminalCardContainer.addOnLayoutChangeListener(mLiveWallpaperSyncListener);
        }
        View activityScene = findViewById(R.id.activity_termux_root_relative_layout);
        if (activityScene != null) {
            activityScene.addOnLayoutChangeListener(mLiveWallpaperSyncListener);
        }
        mTerminalBorderOverlay = findViewById(R.id.terminal_border_overlay);
        mTerminalBorderDrawable = new TerminalBorderDrawable();
        if (mTerminalBorderOverlay != null) {
            mTerminalBorderOverlay.setBackground(mTerminalBorderDrawable);
        }
        mTerminalGlassTint = findViewById(R.id.terminal_glass_tint);
        mTerminalToolbarBackgroundImage = findViewById(R.id.terminal_toolbar_background_image);
        mTerminalToolbarBackgroundOverlay = findViewById(R.id.terminal_toolbar_background_overlay);
        mTerminalToolbarGlassTint = findViewById(R.id.terminal_toolbar_glass_tint);
        mActivityBackgroundImage = findViewById(R.id.activity_background_image);
        mActivityBackgroundOverlay = findViewById(R.id.activity_background_overlay);

        // Keep the toolbar background views sized to the extra keys card. They start at 0dp
        // in the layout so they can never inflate the wrap-content card measure.
        if (mTerminalToolbarCard != null) {
            mTerminalToolbarCard.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                int height = mTerminalToolbarCard.getHeight();
                if (height == mLastToolbarBackgroundHeight) return;
                mLastToolbarBackgroundHeight = height;
                if (isBarBackgroundEnabled() && mLastBackgroundBitmap != null && height > 0) {
                    applyToolbarBackgroundSlice(mLastBackgroundBitmap, mLastBackgroundBlurRadius,
                        mLastBackgroundBlurRadius * getResources().getDisplayMetrics().density);
                } else {
                    syncToolbarBackgroundViews();
                }
            });
        }

        mTermuxGesturePillView = findViewById(R.id.terminal_gesture_pill);
        if (mTermuxGesturePillView != null) {
            mTermuxGesturePillView.setActivity(this);
        }

        View tabBarLayout = findViewById(R.id.terminal_tab_bar_layout);
        if (tabBarLayout != null) {
            mTerminalTabBarLayout = (FrameLayout) tabBarLayout;
            mTerminalTabBarBackgroundImage = findViewById(R.id.terminal_tab_bar_background_image);
            mTermuxTabBarController = new TermuxTabBarController(this, tabBarLayout);
        }

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onCreate();

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onCreate();
    }

    private void setTermuxSessionsListView() {
        ListView termuxSessionsListView = findViewById(R.id.terminal_sessions_list);
        if (termuxSessionsListView != null && mTermuxService != null) {
            mTermuxSessionListViewController = new TermuxSessionsListViewController(this, mTermuxService.getTermuxSessions());
            termuxSessionsListView.setAdapter(mTermuxSessionListViewController);
            termuxSessionsListView.setOnItemClickListener(mTermuxSessionListViewController);
            termuxSessionsListView.setOnItemLongClickListener(mTermuxSessionListViewController);
        }
    }

    private void setSettingsButtonView() {
        ImageButton settingsButton = findViewById(R.id.settings_button);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                ActivityUtils.startActivity(this, new Intent(this, SettingsActivity.class));
            });
        }
    }

    private void setNewSessionButtonView() {
        View newSessionButton = findViewById(R.id.new_session_button);
        if (newSessionButton != null) {
            newSessionButton.setOnClickListener(v -> mTermuxTerminalSessionActivityClient.addNewSession(false, null));
            newSessionButton.setOnLongClickListener(v -> {
                TextInputDialogUtils.textInput(TermuxActivity.this, R.string.title_create_named_session, null,
                    R.string.action_create_named_session_confirm, text -> mTermuxTerminalSessionActivityClient.addNewSession(false, text),
                    R.string.action_new_session_failsafe, text -> mTermuxTerminalSessionActivityClient.addNewSession(true, text),
                    -1, null, null);
                return true;
            });
        }
    }

    private void setToggleKeyboardView() {
        View toggleBtn = findViewById(R.id.toggle_keyboard_button);
        if (toggleBtn != null) {
            toggleBtn.setOnClickListener(v -> {
                mTermuxTerminalViewClient.onToggleSoftKeyboardRequest();
                DrawerLayout drawer = getDrawer();
                if (drawer != null) drawer.closeDrawers();
            });

            toggleBtn.setOnLongClickListener(v -> {
                toggleTerminalToolbar();
                return true;
            });
        }
    }



    private void setTerminalToolbarView(Bundle savedInstanceState) {
        mTermuxTerminalExtraKeys = new TermuxTerminalExtraKeys(this, mTerminalView,
            mTermuxTerminalViewClient, mTermuxTerminalSessionActivityClient);

        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (mPreferences.shouldShowTerminalToolbar()) terminalToolbarViewPager.setVisibility(View.VISIBLE);

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        mTerminalToolbarDefaultHeight = layoutParams.height;

        setTerminalToolbarHeight();

        String savedTextInput = null;
        if (savedInstanceState != null)
            savedTextInput = savedInstanceState.getString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT);

        terminalToolbarViewPager.setAdapter(new TerminalToolbarViewPager.PageAdapter(this, savedTextInput));
        terminalToolbarViewPager.addOnPageChangeListener(new TerminalToolbarViewPager.OnPageChangeListener(this, terminalToolbarViewPager));
    }

    private void setTerminalToolbarHeight() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null) return;

        float extraKeysScale = 1.0f;
        if (getPreferences() != null) {
            extraKeysScale = getPreferences().getExtraKeysHeightScale() / 100.0f;
        }

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        layoutParams.height = Math.round(mTerminalToolbarDefaultHeight *
            (mTermuxTerminalExtraKeys.getExtraKeysInfo() == null ? 0 : mTermuxTerminalExtraKeys.getExtraKeysInfo().getMatrix().length) *
            mProperties.getTerminalToolbarHeightScaleFactor() * extraKeysScale);
        terminalToolbarViewPager.setLayoutParams(layoutParams);
    }

    public void toggleTerminalToolbar() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null) return;

        final boolean showNow = mPreferences.toogleShowTerminalToolbar();
        Logger.showToast(this, (showNow ? getString(R.string.msg_enabling_terminal_toolbar) : getString(R.string.msg_disabling_terminal_toolbar)), true);
        terminalToolbarViewPager.setVisibility(showNow ? View.VISIBLE : View.GONE);
        if (showNow && isTerminalToolbarTextInputViewSelected()) {
            // Focus the text input view if just revealed.
            findViewById(R.id.terminal_toolbar_text_input).requestFocus();
        }
        if (mTermuxGesturePillView != null) {
            mTermuxGesturePillView.post(() -> mTermuxGesturePillView.applyPosition());
        }
    }

    private void saveTerminalToolbarTextInput(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        final EditText textInputView = findViewById(R.id.terminal_toolbar_text_input);
        if (textInputView != null) {
            String textInput = textInputView.getText().toString();
            if (!textInput.isEmpty()) savedInstanceState.putString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT, textInput);
        }
    }



    @SuppressLint("RtlHardcoded")
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = getDrawer();
        if (drawer != null && drawer.isDrawerOpen(Gravity.LEFT)) {
            drawer.closeDrawers();
        } else {
            finishActivityIfNotFinishing();
        }
    }

    public void finishActivityIfNotFinishing() {
        // prevent duplicate calls to finish() if called from multiple places
        if (!TermuxActivity.this.isFinishing()) {
            finish();
        }
    }

    /** Show a toast and dismiss the last one if still visible. */
    public void showToast(String text, boolean longDuration) {
        if (text == null || text.isEmpty()) return;
        if (mLastToast != null) mLastToast.cancel();
        mLastToast = Toast.makeText(TermuxActivity.this, text, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        int yOffset = (int) (getResources().getDisplayMetrics().heightPixels * 0.20f);
        mLastToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, yOffset);
        mLastToast.show();
    }



    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        TerminalSession currentSession = getCurrentSession();
        if (currentSession == null) return;

        boolean autoFillEnabled = mTerminalView.isAutoFillEnabled();

        menu.add(Menu.NONE, CONTEXT_MENU_SELECT_URL_ID, Menu.NONE, R.string.action_select_url);
        menu.add(Menu.NONE, CONTEXT_MENU_SHARE_TRANSCRIPT_ID, Menu.NONE, R.string.action_share_transcript);
        if (!DataUtils.isNullOrEmpty(mTerminalView.getStoredSelectedText()))
            menu.add(Menu.NONE, CONTEXT_MENU_SHARE_SELECTED_TEXT, Menu.NONE, R.string.action_share_selected_text);
        if (autoFillEnabled)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_USERNAME, Menu.NONE, R.string.action_autofill_username);
        if (autoFillEnabled)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_PASSWORD, Menu.NONE, R.string.action_autofill_password);
        menu.add(Menu.NONE, CONTEXT_MENU_RESET_TERMINAL_ID, Menu.NONE, R.string.action_reset_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_KILL_PROCESS_ID, Menu.NONE, getResources().getString(R.string.action_kill_process, getCurrentSession().getPid())).setEnabled(currentSession.isRunning());
        menu.add(Menu.NONE, CONTEXT_MENU_STYLING_ID, Menu.NONE, R.string.action_style_terminal);
        menu.add(Menu.NONE, CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON, Menu.NONE, R.string.action_toggle_keep_screen_on).setCheckable(true).setChecked(mPreferences.shouldKeepScreenOn());
        menu.add(Menu.NONE, CONTEXT_MENU_HELP_ID, Menu.NONE, R.string.action_open_help);
        menu.add(Menu.NONE, CONTEXT_MENU_SETTINGS_ID, Menu.NONE, R.string.action_open_settings);
        menu.add(Menu.NONE, CONTEXT_MENU_REPORT_ID, Menu.NONE, R.string.action_report_issue);
    }

    /** Hook system menu to show context menu instead. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mTerminalView.showContextMenu();
        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TerminalSession session = getCurrentSession();

        switch (item.getItemId()) {
            case CONTEXT_MENU_SELECT_URL_ID:
                mTermuxTerminalViewClient.showUrlSelection();
                return true;
            case CONTEXT_MENU_SHARE_TRANSCRIPT_ID:
                mTermuxTerminalViewClient.shareSessionTranscript();
                return true;
            case CONTEXT_MENU_SHARE_SELECTED_TEXT:
                mTermuxTerminalViewClient.shareSelectedText();
                return true;
            case CONTEXT_MENU_AUTOFILL_USERNAME:
                mTerminalView.requestAutoFillUsername();
                return true;
            case CONTEXT_MENU_AUTOFILL_PASSWORD:
                mTerminalView.requestAutoFillPassword();
                return true;
            case CONTEXT_MENU_RESET_TERMINAL_ID:
                onResetTerminalSession(session);
                return true;
            case CONTEXT_MENU_KILL_PROCESS_ID:
                showKillSessionDialog(session);
                return true;
            case CONTEXT_MENU_STYLING_ID:
                showStylingDialog();
                return true;
            case CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON:
                toggleKeepScreenOn();
                return true;
            case CONTEXT_MENU_HELP_ID:
                ActivityUtils.startActivity(this, new Intent(this, HelpActivity.class));
                return true;
            case CONTEXT_MENU_SETTINGS_ID:
                ActivityUtils.startActivity(this, new Intent(this, SettingsActivity.class));
                return true;
            case CONTEXT_MENU_REPORT_ID:
                mTermuxTerminalViewClient.reportIssueFromTranscript();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        // onContextMenuClosed() is triggered twice if back button is pressed to dismiss instead of tap for some reason
        mTerminalView.onContextMenuClosed(menu);
    }

    private void showKillSessionDialog(TerminalSession session) {
        if (session == null) return;

        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setMessage(R.string.title_confirm_kill_process);
        b.setPositiveButton(android.R.string.yes, (dialog, id) -> {
            dialog.dismiss();
            session.finishIfRunning();
        });
        b.setNegativeButton(android.R.string.no, null);
        b.show();
    }

    private void onResetTerminalSession(TerminalSession session) {
        if (session != null) {
            session.reset();
            showToast(getResources().getString(R.string.msg_terminal_reset), true);

            if (mTermuxTerminalSessionActivityClient != null)
                mTermuxTerminalSessionActivityClient.onResetTerminalSession();
        }
    }

    private void showStylingDialog() {
        Intent stylingIntent = new Intent();
        stylingIntent.setClassName(TermuxConstants.TERMUX_STYLING_PACKAGE_NAME, TermuxConstants.TERMUX_STYLING_APP.TERMUX_STYLING_ACTIVITY_NAME);
        try {
            startActivity(stylingIntent);
        } catch (ActivityNotFoundException | IllegalArgumentException e) {
            // The startActivity() call is not documented to throw IllegalArgumentException.
            // However, crash reporting shows that it sometimes does, so catch it here.
            new AlertDialog.Builder(this).setMessage(getString(R.string.error_styling_not_installed))
                .setPositiveButton(R.string.action_styling_install,
                    (dialog, which) -> ActivityUtils.startActivity(this, new Intent(Intent.ACTION_VIEW, Uri.parse(TermuxConstants.TERMUX_STYLING_FDROID_PACKAGE_URL))))
                .setNegativeButton(android.R.string.cancel, null).show();
        }
    }
    private void toggleKeepScreenOn() {
        if (mTerminalView.getKeepScreenOn()) {
            mTerminalView.setKeepScreenOn(false);
            mPreferences.setKeepScreenOn(false);
        } else {
            mTerminalView.setKeepScreenOn(true);
            mPreferences.setKeepScreenOn(true);
        }
    }



    /**
     * For processes to access primary external storage (/sdcard, /storage/emulated/0, ~/storage/shared),
     * termux needs to be granted legacy WRITE_EXTERNAL_STORAGE or MANAGE_EXTERNAL_STORAGE permissions
     * if targeting targetSdkVersion 30 (android 11) and running on sdk 30 (android 11) and higher.
     */
    public void requestStoragePermission(boolean isPermissionCallback) {
        new Thread() {
            @Override
            public void run() {
                // Do not ask for permission again
                int requestCode = isPermissionCallback ? -1 : PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION;

                // If permission is granted, then also setup storage symlinks.
                if(PermissionUtils.checkAndRequestLegacyOrManageExternalStoragePermission(
                    TermuxActivity.this, requestCode, !isPermissionCallback)) {
                    if (isPermissionCallback)
                        Logger.logInfoAndShowToast(TermuxActivity.this, LOG_TAG,
                            getString(com.termux.shared.R.string.msg_storage_permission_granted_on_request));

                    TermuxInstaller.setupStorageSymlinks(TermuxActivity.this);
                } else {
                    if (isPermissionCallback)
                        Logger.logInfoAndShowToast(TermuxActivity.this, LOG_TAG,
                            getString(com.termux.shared.R.string.msg_storage_permission_not_granted_on_request));
                }
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.logVerbose(LOG_TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: "  + resultCode + ", data: "  + IntentUtils.getIntentString(data));
        if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {
            requestStoragePermission(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Logger.logVerbose(LOG_TAG, "onRequestPermissionsResult: requestCode: " + requestCode + ", permissions: "  + Arrays.toString(permissions) + ", grantResults: "  + Arrays.toString(grantResults));
        if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {
            requestStoragePermission(true);
        }
    }



    public int getNavBarHeight() {
        return mNavBarHeight;
    }

    public TermuxActivityRootView getTermuxActivityRootView() {
        return mTermuxActivityRootView;
    }

    public View getTermuxActivityBottomSpaceView() {
        return mTermuxActivityBottomSpaceView;
    }

    public ExtraKeysView getExtraKeysView() {
        return mExtraKeysView;
    }

    public TermuxTerminalExtraKeys getTermuxTerminalExtraKeys() {
        return mTermuxTerminalExtraKeys;
    }

    public void setExtraKeysView(ExtraKeysView extraKeysView) {
        mExtraKeysView = extraKeysView;
    }

    public DrawerLayout getDrawer() {
        return (DrawerLayout) findViewById(R.id.drawer_layout);
    }


    public ViewPager getTerminalToolbarViewPager() {
        return (ViewPager) findViewById(R.id.terminal_toolbar_view_pager);
    }

    public float getTerminalToolbarDefaultHeight() {
        return mTerminalToolbarDefaultHeight;
    }

    public boolean isTerminalViewSelected() {
        return getTerminalToolbarViewPager().getCurrentItem() == 0;
    }

    public boolean isTerminalToolbarTextInputViewSelected() {
        return getTerminalToolbarViewPager().getCurrentItem() == 1;
    }


    public void termuxSessionListNotifyUpdated() {
        if (mTermuxSessionListViewController != null) {
            mTermuxSessionListViewController.notifyDataSetChanged();
        }
        if (mTermuxTabBarController != null) {
            mTermuxTabBarController.updateTabs();
        }
    }

    public TermuxTabBarController getTermuxTabBarController() {
        return mTermuxTabBarController;
    }

    public void updateTerminalAppearance() {
        if (getPreferences() == null) return;
        float density = getResources().getDisplayMetrics().density;

        // 1. Niri WM Window Border Styling
        setMargins();
        if (mTerminalCardContainer != null) {
            boolean borderEnabled = getPreferences().isTerminalBorderEnabled();
            if (borderEnabled) {
                int cornerRadius = getPreferences().isTerminalBorderCornerEnabled()
                    ? (int) (getPreferences().getTerminalBorderCornerRadius() * density) : 0;
                int strokeWidth = (int) (getPreferences().getTerminalBorderWidth() * density);
                int gaps = (int) (getPreferences().getTerminalBorderGaps() * density);
                String colorStr = getPreferences().getTerminalBorderColor();
                String borderStyle = getPreferences().getTerminalBorderStyle();

                int strokeColor;
                try {
                    strokeColor = Color.parseColor(colorStr);
                } catch (Exception e) {
                    strokeColor = 0xFFFFFFFF; // Default white
                }

                // If TerminalBorderDrawable is available on overlay, use it to support Brackets and Title text
                if (mTerminalBorderDrawable != null && mTerminalBorderOverlay != null) {
                    mTerminalCardContainer.setRadius(cornerRadius);
                    mTerminalCardContainer.setStrokeWidth(0); // Overlay handles drawing the border
                    mTerminalCardContainer.setPreventCornerOverlap(true);
                    mTerminalCardContainer.setContentPadding(0, 0, 0, 0);

                    mTerminalBorderDrawable.setConfig(density, strokeWidth, cornerRadius, strokeColor, borderStyle);
                    mTerminalBorderOverlay.setVisibility(View.VISIBLE);
                    mTerminalBorderOverlay.invalidate();
                } else {
                    mTerminalCardContainer.setRadius(cornerRadius);
                    mTerminalCardContainer.setStrokeWidth(strokeWidth);
                    mTerminalCardContainer.setStrokeColor(strokeColor);
                    mTerminalCardContainer.setPreventCornerOverlap(true);
                    mTerminalCardContainer.setContentPadding(0, 0, 0, 0);
                }

                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mTerminalCardContainer.getLayoutParams();
                if (lp != null) {
                    lp.setMargins(gaps, gaps, gaps, gaps);
                    mTerminalCardContainer.setLayoutParams(lp);
                }

                if (mTerminalView != null) {
                    ViewGroup.MarginLayoutParams viewLp = (ViewGroup.MarginLayoutParams) mTerminalView.getLayoutParams();
                    if (viewLp != null) {
                        int outerGap = (int) ((getPreferences().getTerminalHorizontalMargin() + getPreferences().getTerminalBorderGaps()) * density);
                        int borderToText = outerGap + strokeWidth;
                        viewLp.setMargins(borderToText, borderToText, borderToText, borderToText);
                        mTerminalView.setLayoutParams(viewLp);
                        syncSnapshotMargins(viewLp);
                    }
                }
            } else {
                // Border disabled: corners still apply if their toggle is on.
                int cornerRadius = getPreferences().isTerminalBorderCornerEnabled()
                    ? (int) (getPreferences().getTerminalBorderCornerRadius() * density) : 0;
                mTerminalCardContainer.setRadius(cornerRadius);
                mTerminalCardContainer.setStrokeWidth(0);
                mTerminalCardContainer.setContentPadding(0, 0, 0, 0);
                if (mTerminalBorderOverlay != null) {
                    mTerminalBorderOverlay.setVisibility(View.GONE);
                }
                // With "Keep Layout When Border Off" the window keeps the exact size and
                // position it would have with the border drawn, minus the stroke itself.
                int gaps = getPreferences().isKeepBorderLayoutEnabled()
                    ? (int) (getPreferences().getTerminalBorderGaps() * density) : 0;
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mTerminalCardContainer.getLayoutParams();
                if (lp != null) {
                    lp.setMargins(gaps, gaps, gaps, gaps);
                    mTerminalCardContainer.setLayoutParams(lp);
                }

                if (mTerminalView != null) {
                    ViewGroup.MarginLayoutParams viewLp = (ViewGroup.MarginLayoutParams) mTerminalView.getLayoutParams();
                    if (viewLp != null) {
                        if (getPreferences().isKeepBorderLayoutEnabled()) {
                            int strokeWidth = (int) (getPreferences().getTerminalBorderWidth() * density);
                            int outerGap = (int) ((getPreferences().getTerminalHorizontalMargin() + getPreferences().getTerminalBorderGaps()) * density);
                            int borderToText = outerGap + strokeWidth;
                            viewLp.setMargins(borderToText, borderToText, borderToText, borderToText);
                        } else {
                            viewLp.setMargins(0, 0, 0, 0);
                        }
                        mTerminalView.setLayoutParams(viewLp);
                        syncSnapshotMargins(viewLp);
                    }
                }
            }

            final int cardRadius = getPreferences().isTerminalBorderCornerEnabled()
                ? (int) (getPreferences().getTerminalBorderCornerRadius() * density) : 0;
            mTerminalCardContainer.setRadius(cardRadius);
            mTerminalCardContainer.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cardRadius);
                }
            });
            mTerminalCardContainer.setClipToOutline(cardRadius > 0);
        }

        // 2. Terminal Background Image & Opacity Overlay (with blur + glass)
        applyTerminalBackground();

        // 2.5 Terminal Cell Background Transparency (for Neovim / TUI apps)
        if (mTerminalView != null) {
            if (getPreferences().isTerminalCellBackgroundTransparencyEnabled()) {
                int opacityPercent = getPreferences().getTerminalCellBackgroundOpacity();
                int alpha = Math.round(opacityPercent * 255f / 100f);
                mTerminalView.setCellBackgroundAlpha(alpha);
            } else {
                mTerminalView.setCellBackgroundAlpha(-1);
            }

            // Smooth (fractional pixel) scrolling of terminal content.
            mTerminalView.setSmoothScrollEnabled(getPreferences().isTerminalSmoothScrollEnabled());
        }

        // 3. Tab Bar
        if (mTerminalTabBarLayout != null) {
            ViewGroup.MarginLayoutParams tabLp = (ViewGroup.MarginLayoutParams) mTerminalTabBarLayout.getLayoutParams();
            if (tabLp != null) {
                tabLp.setMargins(0, 0, 0, 0);
                mTerminalTabBarLayout.setLayoutParams(tabLp);
            }
        }
        if (mTermuxTabBarController != null) {
            mTermuxTabBarController.updateTabs();
        }

        // 4. Extra Keys Bar Border (Option 2)
        if (mTerminalToolbarCard != null) {
            boolean barBorderEnabled = getPreferences().isExtraKeysBarBorderEnabled();
            if (barBorderEnabled) {
                int barStrokeWidth = (int) (getPreferences().getExtraKeysBarBorderWidth() * density);
                int cornerRadius = getPreferences().isExtraKeysBarCornerEnabled()
                    ? (int) (getPreferences().getExtraKeysBarBorderCornerRadius() * density) : 0;
                int gaps = (int) (getPreferences().getExtraKeysBarBorderGaps() * density);

                int barStrokeColor;
                String barColor = getPreferences().getExtraKeysBarBorderColor();
                try {
                    barStrokeColor = Color.parseColor(barColor);
                } catch (Exception e) {
                    barStrokeColor = 0xFFFFFFFF;
                }
                mTerminalToolbarCard.setRadius(cornerRadius);
                mTerminalToolbarCard.setStrokeWidth(barStrokeWidth);
                mTerminalToolbarCard.setStrokeColor(barStrokeColor);
                mTerminalToolbarCard.setPreventCornerOverlap(true);
                mTerminalToolbarCard.setCardBackgroundColor(Color.BLACK);

                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mTerminalToolbarCard.getLayoutParams();
                if (lp != null) {
                    lp.setMargins(gaps, gaps, gaps, gaps);
                    mTerminalToolbarCard.setLayoutParams(lp);
                }
            } else {
                // Bar border disabled: corners still apply if their toggle is on.
                int cornerRadius = getPreferences().isExtraKeysBarCornerEnabled()
                    ? (int) (getPreferences().getExtraKeysBarBorderCornerRadius() * density) : 0;
                mTerminalToolbarCard.setRadius(cornerRadius);
                mTerminalToolbarCard.setStrokeWidth(0);
                mTerminalToolbarCard.setCardBackgroundColor(Color.TRANSPARENT);
                // With "Keep Layout When Border Off" the bar keeps the exact size and
                // position it would have with the border drawn, minus the stroke itself.
                int gaps = getPreferences().isKeepBorderLayoutEnabled()
                    ? (int) (getPreferences().getExtraKeysBarBorderGaps() * density) : 0;
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mTerminalToolbarCard.getLayoutParams();
                if (lp != null) {
                    lp.setMargins(gaps, gaps, gaps, gaps);
                    mTerminalToolbarCard.setLayoutParams(lp);
                }
            }

            final int barRadius = getPreferences().isExtraKeysBarCornerEnabled()
                ? (int) (getPreferences().getExtraKeysBarBorderCornerRadius() * density) : 0;
            mTerminalToolbarCard.setRadius(barRadius);
            mTerminalToolbarCard.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), barRadius);
                }
            });
            mTerminalToolbarCard.setClipToOutline(barRadius > 0);
        }

        // 5. Extra Keys Styling & Config
        reloadExtraKeysStylingAndConfig();

        // 5.5 Glass & Blur Effects
        applyGlassAndBlurEffects(density);

        // 6. Floating Gesture Pill
        if (mTermuxGesturePillView != null) {
            mTermuxGesturePillView.applyConfig();
        }
    }

    /**
     * Load (or reuse from the LRU cache) the terminal background wallpaper and apply
     * blur, saturation and glass layout to the terminal and toolbar background views.
     * Also manages the optional wallpaper behind the whole activity (outer background).
     */
    private void applyTerminalBackground() {
        if (mTerminalBackgroundImage == null || mTerminalBackgroundOverlay == null) return;

        String imagePath = getPreferences().getTerminalBackgroundImagePath();
        if (!TextUtils.isEmpty(imagePath)) {
            final int opacity = getPreferences().getTerminalBackgroundOpacity();
            final float alpha = Math.max(0.0f, Math.min(1.0f, opacity / 100.0f));
            if (!getPreferences().isGlassEffectEnabled()) {
                mTerminalBackgroundOverlay.setAlpha(alpha);
                mTerminalBackgroundOverlay.setVisibility(View.VISIBLE);
            } else {
                mTerminalBackgroundOverlay.setVisibility(View.GONE);
            }
            if (mTerminalToolbarBackgroundOverlay != null) {
                mTerminalToolbarBackgroundOverlay.setAlpha(alpha);
            }
            if (mActivityBackgroundOverlay != null) {
                mActivityBackgroundOverlay.setAlpha(Math.max(0.45f, alpha));
            }

            final String targetPath = imagePath;
            final float density = getResources().getDisplayMetrics().density;
            final boolean blurEnabled = getPreferences().isBackgroundBlurEnabled();
            final int blurRadius = blurEnabled ? Math.max(1, getPreferences().getBackgroundBlurRadius()) : 0;

            // Terminal wallpaper. On API 31+ the bitmap stays sharp and blur is applied
            // as a view RenderEffect, so the cache key collapses to a single entry.
            final int terminalBlurPx = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? 0 : Math.round(blurRadius * density);
            loadBackgroundBitmap(targetPath, terminalBlurPx, targetPath + "|" + terminalBlurPx,
                bitmap -> {
                    if (!isFinishing()) {
                        if (bitmap != null) {
                            applyBackgroundBitmap(bitmap, blurRadius);
                        } else {
                            mTerminalBackgroundImage.setVisibility(View.GONE);
                            mTerminalBackgroundOverlay.setVisibility(View.GONE);
                            hideToolbarBackground();
                            hideTabBarBackground();
                            showToast("Could not load background image", false);
                        }
                    }
                });

            // Outer (whole-activity) wallpaper behind the terminal card.
            String outerStyle = getPreferences().getOuterBackgroundStyle();
            if (!"off".equals(outerStyle)) {
                final boolean outerBlur = "blur".equals(outerStyle);
                final int outerBlurPx = (outerBlur && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) ? Math.round(14 * density) : 0;
                loadBackgroundBitmap(targetPath, outerBlurPx, targetPath + "|outer|" + outerBlurPx,
                    bitmap -> {
                        if (!isFinishing() && bitmap != null) {
                            applyActivityBackground(bitmap, outerBlur);
                        }
                    });
            } else {
                hideActivityBackground();
            }
        } else {
            mTerminalBackgroundImage.setImageDrawable(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mTerminalBackgroundImage.setRenderEffect(null);
            }
            mTerminalBackgroundImage.setColorFilter(null);
            mTerminalBackgroundImage.setVisibility(View.GONE);
            mTerminalBackgroundOverlay.setVisibility(View.GONE);
            mLastBackgroundBitmap = null;
            mLastBackgroundBlurRadius = 0;
            mLastVeilAlpha = 0f;
            hideGlassBackdrop();
            hideToolbarBackground();
            hideTabBarBackground();
            hideActivityBackground();
        }
    }

    private interface BitmapLoadCallback {
        void onLoaded(Bitmap bitmap);
    }

    /**
     * Return a wallpaper bitmap from the LRU cache, or decode it on a background thread
     * (with RenderScript blur on APIs 21-30 when {@code blurRadiusPx > 0}). The callback
     * always runs on the UI thread; all cache access happens there too.
     */
    private void loadBackgroundBitmap(String imagePath, int blurRadiusPx, String cacheKey, BitmapLoadCallback callback) {
        Bitmap cached = mBackgroundBitmapCache.get(cacheKey);
        if (cached != null) {
            // Refresh LRU order.
            mBackgroundBitmapCache.remove(cacheKey);
            mBackgroundBitmapCache.put(cacheKey, cached);
            callback.onLoaded(cached);
            return;
        }

        new Thread(() -> {
            Bitmap loadedBitmap = null;
            try {
                loadedBitmap = decodeBackgroundBitmap(imagePath);
                if (loadedBitmap != null && blurRadiusPx > 0 && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    loadedBitmap = blurBitmapRenderScript(loadedBitmap, blurRadiusPx);
                }
            } catch (Throwable t) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to decode background image in background thread", t);
            }

            final Bitmap finalBitmap = loadedBitmap;
            runOnUiThread(() -> {
                if (!isFinishing()) {
                    if (finalBitmap != null) {
                        mBackgroundBitmapCache.put(cacheKey, finalBitmap);
                    }
                    callback.onLoaded(finalBitmap);
                }
            });
        }).start();
    }

    /** Decode and downsample a background image from a path, file or content URI. */
    private Bitmap decodeBackgroundBitmap(String targetPath) throws Throwable {
        Uri uri = Uri.parse(targetPath);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;

        if (targetPath.startsWith("content://") || targetPath.startsWith("file://")) {
            try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, opts);
            }
        } else {
            BitmapFactory.decodeFile(targetPath, opts);
        }

        // Downsample if image is excessively large (larger than 4096 in either dimension)
        int sampleSize = 1;
        int maxDim = Math.max(opts.outWidth, opts.outHeight);
        while (maxDim / sampleSize > 4096) {
            sampleSize *= 2;
        }

        BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
        decodeOpts.inSampleSize = sampleSize;

        if (targetPath.startsWith("content://") || targetPath.startsWith("file://")) {
            try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is, null, decodeOpts);
            }
        } else {
            return BitmapFactory.decodeFile(targetPath, decodeOpts);
        }
    }

    /**
     * Show the wallpaper behind the whole activity (the area outside the terminal card:
     * margins, tab bar gaps, extra keys gaps), blurred or sharp. The image is anchored
     * to the FIXED full-screen scene (never re-crops when the keyboard resizes the
     * layout), so the window, the bar and the outside stay one continuous wallpaper.
     */
    @SuppressLint("NewApi")
    private void applyActivityBackground(Bitmap bitmap, boolean blurred) {
        if (mActivityBackgroundImage == null) return;
        mActivityBackgroundImage.setImageBitmap(bitmap);
        Drawable actD = mActivityBackgroundImage.getDrawable();
        if (actD instanceof BitmapDrawable) {
            ((BitmapDrawable) actD).setAntiAlias(true);
            ((BitmapDrawable) actD).setFilterBitmap(true);
            ((BitmapDrawable) actD).setDither(true);
        }
        applyFixedSceneMatrix(mActivityBackgroundImage, bitmap);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (blurred) {
                float blurPx = 14 * getResources().getDisplayMetrics().density;
                mActivityBackgroundImage.setRenderEffect(
                    RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP));
            } else {
                mActivityBackgroundImage.setRenderEffect(null);
            }
        }
        mActivityBackgroundImage.setVisibility(View.VISIBLE);
        if (mActivityBackgroundOverlay != null) {
            mActivityBackgroundOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void logGlassDebug(String msg) {
        android.util.Log.d("TermuxGlassDebug", msg);
        try {
            java.io.FileWriter fw = new java.io.FileWriter("/storage/emulated/0/Download/termux_glass_debug.txt", true);
            fw.append(msg).append("\n");
            fw.close();
        } catch (Throwable ignored) {
            // File write optional: the same line is in logcat (TermuxGlassDebug).
        }
    }

    /**
     * Get the real full-screen dimensions in physical pixels matching getLocationOnScreen.
     */
    private int[] getRealScreenSize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Rect bounds = getWindowManager().getCurrentWindowMetrics().getBounds();
            return new int[]{bounds.width(), bounds.height()};
        } else {
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(dm);
            return new int[]{dm.widthPixels, dm.heightPixels};
        }
    }

    /**
     * Anchor an ImageView to the fixed full-screen wallpaper scene: center-crop the
     * bitmap over the physical screen once, then offset by the view's screen position.
     * The transform never changes when the keyboard opens/closes — the wallpaper
     * keeps its position and the view simply reveals whichever part it covers.
     */
    private void applyFixedSceneMatrix(ImageView view, Bitmap bitmap) {
        if (view == null || bitmap == null || bitmap.getWidth() <= 0 || bitmap.isRecycled()) return;
        if (!view.isLaidOut() || view.getWidth() <= 0 || view.getHeight() <= 0) {
            view.post(() -> {
                if (!isFinishing() && mLastBackgroundBitmap == bitmap && view.isLaidOut()) {
                    applyFixedSceneMatrix(view, bitmap);
                }
            });
        }
        int[] screen = getRealScreenSize();
        float sceneW = screen[0];
        float sceneH = screen[1];
        float s = Math.max(sceneW / bitmap.getWidth(), sceneH / bitmap.getHeight());
        float dx = (sceneW - bitmap.getWidth() * s) / 2f;
        float dy = (sceneH - bitmap.getHeight() * s) / 2f;
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        Matrix matrix = new Matrix();
        matrix.setScale(s, s);
        matrix.postTranslate(dx - loc[0], dy - loc[1]);
        view.setScaleType(ImageView.ScaleType.MATRIX);
        view.setImageMatrix(matrix);
    }

    private void hideActivityBackground() {
        if (mActivityBackgroundImage != null) {
            mActivityBackgroundImage.setVisibility(View.GONE);
            mActivityBackgroundImage.setImageDrawable(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mActivityBackgroundImage.setRenderEffect(null);
            }
        }
        if (mActivityBackgroundOverlay != null) {
            mActivityBackgroundOverlay.setVisibility(View.GONE);
        }
    }

    /**
     * Whether the toolbar should show the wallpaper slice behind it: when the bar
     * color theme is set to "sync_wallpaper" OR when liquid glass is enabled.
     */
    private boolean isBarBackgroundEnabled() {
        return getPreferences() != null
            && (getPreferences().isGlassEffectEnabled()
                || "sync_wallpaper".equals(getPreferences().getExtraKeysColorTheme()));
    }

    private boolean isTabBarBackgroundEnabled() {
        return getPreferences() != null
            && getPreferences().isTabBarEnabled()
            && getPreferences().isTabBarGlassEnabled()
            && getPreferences().isGlassEffectEnabled();
    }

    /**
     * Push a decoded (and possibly pre-blurred) wallpaper bitmap into the terminal
     * background view, applying blur, saturation and the glass union-crop layout.
     */
    @SuppressLint("NewApi")
    private void applyBackgroundBitmap(Bitmap bitmap, int blurRadius) {
        boolean glassEnabled = getPreferences() != null && getPreferences().isGlassEffectEnabled();
        float blurPx = blurRadius * getResources().getDisplayMetrics().density;

        mLastBackgroundBitmap = bitmap;
        mLastBackgroundBlurRadius = blurRadius;

        if (mTerminalBackgroundImage != null) {
            mTerminalBackgroundImage.setImageBitmap(bitmap);
            Drawable bgD = mTerminalBackgroundImage.getDrawable();
            if (bgD instanceof BitmapDrawable) {
                ((BitmapDrawable) bgD).setAntiAlias(true);
                ((BitmapDrawable) bgD).setFilterBitmap(true);
                ((BitmapDrawable) bgD).setDither(true);
            }
            // No saturation boost: Liquid Glass keeps the wallpaper colors natural.
            mTerminalBackgroundImage.setColorFilter(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (blurRadius > 0) {
                    mTerminalBackgroundImage.setRenderEffect(
                        RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP));
                } else {
                    mTerminalBackgroundImage.setRenderEffect(null);
                }
            }
            // In glass mode the inner wallpaper is replaced by the blurred backdrop.
            mTerminalBackgroundImage.setVisibility(glassEnabled ? View.GONE : View.VISIBLE);
        }

        if (glassEnabled) {
            applyGlassBackdrop(bitmap, getGlassBlurPx(getResources().getDisplayMetrics().density));
            if (mTerminalBackgroundOverlay != null) {
                mTerminalBackgroundOverlay.setVisibility(View.GONE);
            }
        } else {
            hideGlassBackdrop();
            if (mTerminalBackgroundOverlay != null) {
                int opacity = getPreferences() != null ? getPreferences().getTerminalBackgroundOpacity() : 60;
                float alpha = Math.max(0.0f, Math.min(1.0f, opacity / 100.0f));
                mTerminalBackgroundOverlay.setAlpha(alpha);
                mTerminalBackgroundOverlay.setVisibility(View.VISIBLE);
            }
        }

        if (isBarBackgroundEnabled()) {
            applyToolbarBackgroundSlice(bitmap, blurRadius, blurPx);
        } else {
            hideToolbarBackground();
            if (mTerminalBackgroundImage != null) {
                applyFixedSceneMatrix(mTerminalBackgroundImage, bitmap);
            }
        }

        if (isTabBarBackgroundEnabled()) {
            applyTabBarBackgroundSlice(bitmap, blurRadius, blurPx);
        } else {
            hideTabBarBackground();
        }
    }

    /**
     * Wallpaper sync: the terminal card and the extra keys bar are two windows into
     * one full-screen wallpaper scene (center-crop over the whole activity area, the
     * same transform as the outer background layer). Each view shows the scene region
     * at its CURRENT position, so while the keyboard is open the bar shows the
     * wallpaper continuation right below the terminal, and when the keyboard is
     * closed the bar (now at the bottom of the screen) shows the bottom of the
     * wallpaper. Re-applied on every layout change, the wallpaper slides with the
     * bar in real time while the keyboard animates.
     */
    @SuppressLint("NewApi")
    private void applyToolbarBackgroundSlice(Bitmap bitmap, int blurRadius, float blurPx) {
        if (mTerminalToolbarBackgroundImage == null || mTerminalToolbarCard == null || mTerminalCardContainer == null) return;

        int[] cardLoc = new int[2];
        int[] barLoc = new int[2];
        mTerminalCardContainer.getLocationOnScreen(cardLoc);
        mTerminalToolbarCard.getLocationOnScreen(barLoc);

        int cardW = mTerminalCardContainer.getWidth();
        int cardH = mTerminalCardContainer.getHeight();
        int barW = mTerminalToolbarCard.getWidth();
        int barH = mTerminalToolbarCard.getHeight();

        if (cardW <= 0 || cardH <= 0 || barW <= 0 || barH <= 0) {
            // Layout not ready yet (or the bar is hidden): retry once after a layout pass,
            // but never spin-retry while the toolbar card stays zero-sized.
            mTerminalCardContainer.post(() -> {
                if (!isFinishing() && mLastBackgroundBitmap == bitmap
                    && mTerminalToolbarCard.getVisibility() == View.VISIBLE
                    && mTerminalToolbarCard.getHeight() > 0) {
                    applyToolbarBackgroundSlice(bitmap, blurRadius, blurPx);
                }
            });
            return;
        }

        // Fixed full-screen scene: the wallpaper is anchored to the physical screen
        // and never re-crops when the keyboard resizes the layout. Card and bar are
        // windows into it at their current screen positions.
        int[] screen = getRealScreenSize();
        float sceneW = screen[0];
        float sceneH = screen[1];
        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();
        float scale = Math.max(sceneW / bw, sceneH / bh);
        float dx = (sceneW - bw * scale) / 2f;
        float dy = (sceneH - bh * scale) / 2f;

        // Terminal image: the scene region behind the terminal card.
        if (mTerminalBackgroundImage != null) {
            Matrix matrix = new Matrix();
            matrix.setScale(scale, scale);
            matrix.postTranslate(dx - cardLoc[0], dy - cardLoc[1]);
            mTerminalBackgroundImage.setScaleType(ImageView.ScaleType.MATRIX);
            mTerminalBackgroundImage.setImageMatrix(matrix);
        }

        // Toolbar image: the scene region behind the extra keys bar at its current position.
        Matrix barMatrix = new Matrix();
        barMatrix.setScale(scale, scale);
        barMatrix.postTranslate(dx - barLoc[0], dy - barLoc[1]);
        mTerminalToolbarBackgroundImage.setScaleType(ImageView.ScaleType.MATRIX);
        mTerminalToolbarBackgroundImage.setImageMatrix(barMatrix);
        mTerminalToolbarBackgroundImage.setImageBitmap(bitmap);
        Drawable barD = mTerminalToolbarBackgroundImage.getDrawable();
        if (barD instanceof BitmapDrawable) {
            ((BitmapDrawable) barD).setAntiAlias(true);
            ((BitmapDrawable) barD).setFilterBitmap(true);
            ((BitmapDrawable) barD).setDither(true);
        }
        mTerminalToolbarBackgroundImage.setColorFilter(null);

        boolean glassEnabled = getPreferences() != null && getPreferences().isGlassEffectEnabled();
        float density = getResources().getDisplayMetrics().density;
        float barBlurPx = glassEnabled ? getGlassBlurPx(density) : blurPx;

        float barSrcX = (barLoc[0] - dx) / scale;
        float barSrcY = (barLoc[1] - dy) / scale;
        float barSrcW = barW / scale;
        float barSrcH = barH / scale;
        float barLuminance = computeRegionLuminance(bitmap, barSrcX, barSrcY, barSrcW, barSrcH);
        float tintFactor = getPreferences() != null
            ? Math.max(0f, Math.min(1f, getPreferences().getGlassTintOpacity() / 100f)) : 1f;
        float barVeilAlpha = adaptiveTintAlpha(barLuminance) * tintFactor;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && glassEnabled) {
                try {
                    int barRadius = getPreferences().isExtraKeysBarCornerEnabled()
                        ? (int) (getPreferences().getExtraKeysBarBorderCornerRadius() * density) : 0;
                    float barRim = Math.min(18f * density, barH * 0.42f);
                    float barThick = barRim * 0.65f;
                    RuntimeShader lensShader = new RuntimeShader(GLASS_LENS_SHADER);
                    lensShader.setFloatUniform("uSize", (float) barW, (float) barH);
                    lensShader.setFloatUniform("uRadius", (float) barRadius);
                    lensShader.setFloatUniform("uRim", barRim);
                    lensShader.setFloatUniform("uThickness", barThick);
                    lensShader.setFloatUniform("uIOR", 2.6f);
                    lensShader.setFloatUniform("uVeilAlpha", barVeilAlpha);
                    RenderEffect lensEffect = RenderEffect.createRuntimeShaderEffect(lensShader, "content");
                    if (barBlurPx > 0.5f) {
                        RenderEffect blurEffect = RenderEffect.createBlurEffect(
                            barBlurPx, barBlurPx, Shader.TileMode.CLAMP);
                        mTerminalToolbarBackgroundImage.setRenderEffect(
                            RenderEffect.createChainEffect(lensEffect, blurEffect));
                    } else {
                        mTerminalToolbarBackgroundImage.setRenderEffect(lensEffect);
                    }
                } catch (Throwable t) {
                    mTerminalToolbarBackgroundImage.setRenderEffect(barBlurPx > 0.5f
                        ? RenderEffect.createBlurEffect(barBlurPx, barBlurPx, Shader.TileMode.CLAMP) : null);
                }
            } else if (barBlurPx > 0.5f) {
                mTerminalToolbarBackgroundImage.setRenderEffect(
                    RenderEffect.createBlurEffect(barBlurPx, barBlurPx, Shader.TileMode.CLAMP));
            } else {
                mTerminalToolbarBackgroundImage.setRenderEffect(null);
            }
        }
        mTerminalToolbarBackgroundImage.setVisibility(View.VISIBLE);
        if (mTerminalToolbarBackgroundOverlay != null) {
            // When liquid glass is active, the shader directly blends the adaptive veil in GPU.
            mTerminalToolbarBackgroundOverlay.setVisibility(glassEnabled ? View.GONE : View.VISIBLE);
        }
        syncToolbarBackgroundViews();
    }

    /**
     * Size the toolbar background views (0dp in XML) to the extra keys card height.
     * They must never influence the wrap-content measure of the card or the whole bar
     * inflates and jumps to the top of the screen.
     */
    private void syncToolbarBackgroundViews() {
        if (mTerminalToolbarCard == null) return;
        int height = mTerminalToolbarCard.getHeight();
        if (height <= 0) return;
        View[] views = new View[]{mTerminalToolbarBackgroundImage, mTerminalToolbarBackgroundOverlay, mTerminalToolbarGlassTint};
        for (View view : views) {
            if (view == null) continue;
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null && lp.height != height) {
                lp.height = height;
                view.setLayoutParams(lp);
            }
        }
    }

    private void hideToolbarBackground() {
        if (mTerminalToolbarBackgroundImage != null) {
            mTerminalToolbarBackgroundImage.setVisibility(View.GONE);
            mTerminalToolbarBackgroundImage.setImageDrawable(null);
            mTerminalToolbarBackgroundImage.setColorFilter(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mTerminalToolbarBackgroundImage.setRenderEffect(null);
            }
        }
        if (mTerminalToolbarBackgroundOverlay != null) {
            mTerminalToolbarBackgroundOverlay.setVisibility(View.GONE);
        }
    }

    @SuppressLint("NewApi")
    private void applyTabBarBackgroundSlice(Bitmap bitmap, int blurRadius, float blurPx) {
        if (mTerminalTabBarBackgroundImage == null || mTerminalTabBarLayout == null || mTerminalCardContainer == null) return;

        int[] tabLoc = new int[2];
        mTerminalTabBarLayout.getLocationOnScreen(tabLoc);

        int tabW = mTerminalTabBarLayout.getWidth();
        int tabH = mTerminalTabBarLayout.getHeight();

        if (tabW <= 0 || tabH <= 0) {
            mTerminalTabBarLayout.post(() -> {
                if (!isFinishing() && mLastBackgroundBitmap == bitmap
                    && mTerminalTabBarLayout.getVisibility() == View.VISIBLE
                    && mTerminalTabBarLayout.getHeight() > 0) {
                    applyTabBarBackgroundSlice(bitmap, blurRadius, blurPx);
                }
            });
            return;
        }

        int[] screen = getRealScreenSize();
        float sceneW = screen[0];
        float sceneH = screen[1];
        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();
        float scale = Math.max(sceneW / bw, sceneH / bh);
        float dx = (sceneW - bw * scale) / 2f;
        float dy = (sceneH - bh * scale) / 2f;

        float density = getResources().getDisplayMetrics().density;
        boolean borderEnabled = getPreferences() != null && getPreferences().isTerminalBorderEnabled();
        boolean keepLayout = getPreferences() != null && getPreferences().isKeepBorderLayoutEnabled();
        int gaps = (borderEnabled || keepLayout) && getPreferences() != null
            ? (int) (getPreferences().getTerminalBorderGaps() * density) : 0;

        ViewGroup.MarginLayoutParams imgLp = (ViewGroup.MarginLayoutParams) mTerminalTabBarBackgroundImage.getLayoutParams();
        if (imgLp != null && (imgLp.leftMargin != gaps || imgLp.rightMargin != gaps)) {
            imgLp.setMargins(gaps, 0, gaps, 0);
            mTerminalTabBarBackgroundImage.setLayoutParams(imgLp);
        }

        int effectiveW = Math.max(1, tabW - 2 * gaps);

        Matrix tabMatrix = new Matrix();
        tabMatrix.setScale(scale, scale);
        tabMatrix.postTranslate(dx - tabLoc[0] - gaps, dy - tabLoc[1]);
        mTerminalTabBarBackgroundImage.setScaleType(ImageView.ScaleType.MATRIX);
        mTerminalTabBarBackgroundImage.setImageMatrix(tabMatrix);
        mTerminalTabBarBackgroundImage.setImageBitmap(bitmap);
        Drawable tabD = mTerminalTabBarBackgroundImage.getDrawable();
        if (tabD instanceof BitmapDrawable) {
            ((BitmapDrawable) tabD).setAntiAlias(true);
            ((BitmapDrawable) tabD).setFilterBitmap(true);
            ((BitmapDrawable) tabD).setDither(true);
        }
        mTerminalTabBarBackgroundImage.setColorFilter(null);

        boolean glassEnabled = getPreferences() != null && getPreferences().isGlassEffectEnabled();
        float tabBlurPx = glassEnabled ? getGlassBlurPx(density) : blurPx;

        float tabSrcX = (tabLoc[0] + gaps - dx) / scale;
        float tabSrcY = (tabLoc[1] - dy) / scale;
        float tabSrcW = effectiveW / scale;
        float tabSrcH = tabH / scale;
        float tabLuminance = computeRegionLuminance(bitmap, tabSrcX, tabSrcY, tabSrcW, tabSrcH);
        float tintFactor = getPreferences() != null
            ? Math.max(0f, Math.min(1f, getPreferences().getGlassTintOpacity() / 100f)) : 1f;
        float tabVeilAlpha = adaptiveTintAlpha(tabLuminance) * tintFactor;

        int barCornerRadius = getPreferences() != null && getPreferences().isTabCornerEnabled()
            ? (int) (getPreferences().getTabBarCornerRadius() * density) : 0;
        final int finalRadius = Math.min(barCornerRadius, tabH / 2);

        mTerminalTabBarBackgroundImage.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), finalRadius);
            }
        });
        mTerminalTabBarBackgroundImage.setClipToOutline(finalRadius > 0);
        mTerminalTabBarLayout.setClipToOutline(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && glassEnabled) {
                try {
                    float tabRim = Math.min(18f * density, tabH * 0.42f);
                    float tabThick = tabRim * 0.65f;
                    RuntimeShader lensShader = new RuntimeShader(GLASS_LENS_SHADER);
                    lensShader.setFloatUniform("uSize", (float) effectiveW, (float) tabH);
                    lensShader.setFloatUniform("uRadius", (float) finalRadius);
                    lensShader.setFloatUniform("uRim", tabRim);
                    lensShader.setFloatUniform("uThickness", tabThick);
                    lensShader.setFloatUniform("uIOR", 2.6f);
                    lensShader.setFloatUniform("uVeilAlpha", tabVeilAlpha);
                    RenderEffect lensEffect = RenderEffect.createRuntimeShaderEffect(lensShader, "content");
                    if (tabBlurPx > 0.5f) {
                        RenderEffect blurEffect = RenderEffect.createBlurEffect(
                            tabBlurPx, tabBlurPx, Shader.TileMode.CLAMP);
                        mTerminalTabBarBackgroundImage.setRenderEffect(
                            RenderEffect.createChainEffect(lensEffect, blurEffect));
                    } else {
                        mTerminalTabBarBackgroundImage.setRenderEffect(lensEffect);
                    }
                } catch (Throwable t) {
                    mTerminalTabBarBackgroundImage.setRenderEffect(tabBlurPx > 0.5f
                        ? RenderEffect.createBlurEffect(tabBlurPx, tabBlurPx, Shader.TileMode.CLAMP) : null);
                }
            } else if (tabBlurPx > 0.5f) {
                mTerminalTabBarBackgroundImage.setRenderEffect(
                    RenderEffect.createBlurEffect(tabBlurPx, tabBlurPx, Shader.TileMode.CLAMP));
            } else {
                mTerminalTabBarBackgroundImage.setRenderEffect(null);
            }
        }
        mTerminalTabBarBackgroundImage.setVisibility(View.VISIBLE);
    }

    private void hideTabBarBackground() {
        if (mTerminalTabBarBackgroundImage != null) {
            mTerminalTabBarBackgroundImage.setVisibility(View.GONE);
            mTerminalTabBarBackgroundImage.setImageDrawable(null);
            mTerminalTabBarBackgroundImage.setColorFilter(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mTerminalTabBarBackgroundImage.setRenderEffect(null);
            }
        }
        if (mTerminalTabBarLayout != null) {
            mTerminalTabBarLayout.setClipToOutline(false);
        }
    }

    /**
     * AGSL lens shader for Apple Liquid Glass optics (API 33+):
     * Inspired by iOS 26 Liquid Glass physical optics:
     * - Flat interior is crystal clear unblurred (displacement = 0).
     * - Squircle curved bezel [0..uRim] produces natural droplet surface curvature:
     *   h(t) = pow(1 - (1-t)^4, 0.25).
     * - Refraction displacement peaks on the bezel slope and smoothly returns to 0 at
     *   both boundaries (zero hard seams).
     * - Directional specular glint (lightDir) + Fresnel rim + inner rim highlight.
     * - Smooth dark glass veil scaled by the user's Glass Tint Strength preference.
     * Evaluates content with full opacity (half4(outColor, 1.0)) across all coordinates,
     * allowing the MaterialCardView hardware outline to cleanly clip corners without
     * any 1-frame black dropouts during tiling translations.
     */
    private static final String GLASS_LENS_SHADER =
        "uniform shader content;\n" +
        "uniform vec2 uSize;\n" +
        "uniform float uRadius;\n" +
        "uniform float uRim;\n" +
        "uniform float uThickness;\n" +
        "uniform float uIOR;\n" +
        "uniform float uVeilAlpha;\n" +
        "\n" +
        "float sdRoundRect(vec2 p, vec2 halfSize, float r) {\n" +
        "    vec2 q = abs(p) - halfSize + vec2(r);\n" +
        "    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;\n" +
        "}\n" +
        "\n" +
        "float surfaceHeight(float t) {\n" +
        "    float s = 1.0 - t;\n" +
        "    return pow(max(0.0, 1.0 - s * s * s * s), 0.25);\n" +
        "}\n" +
        "\n" +
        "half4 main(float2 fragCoord) {\n" +
        "    vec2 halfSize = uSize * 0.5;\n" +
        "    vec2 p = fragCoord - halfSize;\n" +
        "    float d = sdRoundRect(p, halfSize, uRadius);\n" +
        "    float dist = -d;\n" +
        "    half4 col;\n" +
        "    float specular = 0.0;\n" +
        "    float bezel = max(2.0, uRim);\n" +
        "    if (dist > 0.0 && dist < bezel) {\n" +
        "        float t = clamp(dist / bezel, 0.0, 1.0);\n" +
        "        float h = surfaceHeight(t);\n" +
        "        float dt = 0.001;\n" +
        "        float h2 = surfaceHeight(min(t + dt, 1.0));\n" +
        "        float dh = (h2 - h) / dt;\n" +
        "\n" +
        "        float slopeAngle = atan(dh * (uThickness / bezel));\n" +
        "        float sinR = clamp(sin(slopeAngle) / uIOR, -1.0, 1.0);\n" +
        "        float thetaR = asin(sinR);\n" +
        "        float displacement = h * uThickness * (tan(slopeAngle) - tan(thetaR));\n" +
        "\n" +
        "        float eps = 0.5;\n" +
        "        vec2 grad;\n" +
        "        grad.x = sdRoundRect(p + vec2(eps, 0.0), halfSize, uRadius) - d;\n" +
        "        grad.y = sdRoundRect(p + vec2(0.0, eps), halfSize, uRadius) - d;\n" +
        "        float gLen = length(grad);\n" +
        "        vec2 normal = gLen > 0.0001 ? grad / gLen : vec2(0.0, -1.0);\n" +
        "\n" +
        "        vec2 dir = -normal;\n" +
        "        float ca = displacement * 0.035;\n" +
        "        half4 cR = content.eval(fragCoord + dir * (displacement - ca));\n" +
        "        half4 cG = content.eval(fragCoord + dir * displacement);\n" +
        "        half4 cB = content.eval(fragCoord + dir * (displacement + ca));\n" +
        "        col = half4(cR.r, cG.g, cB.b, 1.0);\n" +
        "\n" +
        "        vec2 toLight = normalize(vec2(-0.35, -0.93));\n" +
        "        float dotLight = dot(normal, toLight);\n" +
        "        float light = max(0.0, dotLight);\n" +
        "        float shadowSide = max(0.0, -dotLight);\n" +
        "\n" +
        "        // 1. Subtle inner shadow on bevel slope for 3D glass thickness:\n" +
        "        float innerShadow = 1.0 - smoothstep(0.0, bezel * 0.65, dist);\n" +
        "        col.rgb *= mix(1.0, 0.72, innerShadow * (0.15 + 0.35 * shadowSide));\n" +
        "\n" +
        "        // 2. Crisp inner rim highlight along bevel-to-flat transition:\n" +
        "        float innerRim = smoothstep(0.0, 1.5, dist) * (1.0 - smoothstep(1.5, 4.5, dist));\n" +
        "        col.rgb += half3(innerRim * 0.12 * light);\n" +
        "\n" +
        "        // 3. Crisp hairline specular highlight at the outer perimeter:\n" +
        "        float rimHairline = exp(-dist * dist / 14.0);\n" +
        "        specular = rimHairline * pow(light, 2.0) * 0.28;\n" +
        "    } else {\n" +
        "        col = content.eval(fragCoord);\n" +
        "    }\n" +
        "    half3 veil = half3(0.04, 0.06, 0.10);\n" +
        "    half3 outColor = mix(col.rgb, veil, uVeilAlpha);\n" +
        "    outColor += half3(1.0) * specular;\n" +
        "    return half4(outColor, 1.0);\n" +
        "}\n";

    /**
     * Backdrop blur for the glass, density-scaled.
     * Liquid Glass defaults to crystal clear unblurred (0px) so the interior stays
     * sharp like optical glass. When Background Blur is explicitly enabled in
     * Settings, applies the user-configured blur radius.
     */
    private float getGlassBlurPx(float density) {
        if (getPreferences() != null && getPreferences().isBackgroundBlurEnabled()) {
            return Math.max(1f, getPreferences().getBackgroundBlurRadius() * density);
        }
        return 0f;
    }

    /**
     * Average perceived luminance of the wallpaper region shown behind the window,
     * sampled on a ~40x40 grid (cheap enough to run per layout change).
     */
    private float computeRegionLuminance(Bitmap bitmap, float srcX, float srcY, float srcW, float srcH) {
        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();
        int ix = Math.max(0, Math.min(bw - 1, (int) srcX));
        int iy = Math.max(0, Math.min(bh - 1, (int) srcY));
        int iw = Math.min(bw - ix, Math.max(1, (int) srcW));
        int ih = Math.min(bh - iy, Math.max(1, (int) srcH));
        int stepX = Math.max(1, iw / 40);
        int stepY = Math.max(1, ih / 40);
        int[] row = new int[iw];
        float sum = 0f;
        int count = 0;
        for (int y = 0; y < ih; y += stepY) {
            bitmap.getPixels(row, 0, iw, ix, iy + y, iw, 1);
            for (int x = 0; x < iw; x += stepX) {
                int c = row[x];
                sum += 0.2126f * Color.red(c) + 0.7152f * Color.green(c) + 0.0722f * Color.blue(c);
                count++;
            }
        }
        return count > 0 ? sum / count : 128f;
    }

    /**
     * REGULAR-material adaptive veil: brighter backdrop -> stronger dark tint
     * (15-60%), darker backdrop -> nearly clear. Keeps white terminal text
     * readable without smothering dark wallpaper regions. Scaled by the user's
     * glass tint opacity setting so 0% gives crystal clear glass.
     */
    private float adaptiveTintAlpha(float luminance) {
        float t = Math.max(0f, Math.min(1f, luminance / 255f));
        return 0.40f + t * 0.45f;
    }

    /**
     * Apply Apple Liquid Glass to the terminal window, the tab bar buttons and the
     * extra keys bar: optical edge refraction over a crystal clear backdrop (or
     * blurred when Background Blur is enabled), dark readability tint scaled by
     * the glass tint opacity slider, specular rim, and subtle text drop shadow.
     */
    private void applyGlassAndBlurEffects(float density) {
        boolean glassEnabled = getPreferences().isGlassEffectEnabled();

        // In Liquid Glass mode, the AGSL shader renders the adaptive veil and optical
        // Fresnel glint directly in GPU memory, perfectly clipped to the SDF roundRect.
        // The separate 2D shape layer views (mTerminalGlassTint, mTerminalToolbarGlassTint)
        // are hidden so there is zero artificial 2D overlay or edge-bleeding.
        if (mTerminalGlassTint != null) {
            mTerminalGlassTint.setVisibility(View.GONE);
        }
        if (mTerminalToolbarGlassTint != null) {
            mTerminalToolbarGlassTint.setVisibility(View.GONE);
        }

        // Subtle drop shadow behind terminal text so it never washes out on the
        // bright glass backdrop.
        if (mTerminalView != null) {
            mTerminalView.setTextShadowEnabled(glassEnabled);
        }

        // The window (decor view) must stay transparent in glass mode so the
        // backdrop or the wallpaper behind shows through instead of the opaque
        // terminal background color.
        if (mTermuxTerminalSessionActivityClient != null) {
            mTermuxTerminalSessionActivityClient.updateBackgroundColor();
        }

        // Terminal border overlay: ONLY visible when the user explicitly enables
        // a window border in Settings. When border is off, the glass edge is defined
        // 100% by optical refraction + Fresnel glint inside the shader.
        if (mTerminalBorderOverlay != null) {
            boolean borderEnabled = getPreferences().isTerminalBorderEnabled();
            mTerminalBorderOverlay.setVisibility(borderEnabled ? View.VISIBLE : View.GONE);
            if (borderEnabled && mTerminalBorderDrawable != null) {
                int strokeWidth = (int) (getPreferences().getTerminalBorderWidth() * density);
                int cornerRadius = getPreferences().isTerminalBorderCornerEnabled()
                    ? (int) (getPreferences().getTerminalBorderCornerRadius() * density) : 0;
                String colorStr = getPreferences().getTerminalBorderColor();
                int strokeColor;
                try {
                    strokeColor = Color.parseColor(colorStr);
                } catch (Exception e) {
                    strokeColor = 0xFFFFFFFF;
                }
                mTerminalBorderDrawable.setConfig(density, strokeWidth, cornerRadius, strokeColor,
                    getPreferences().getTerminalBorderStyle());
                mTerminalBorderOverlay.invalidate();
            }
        }

        // Extra keys bar wallpaper mode (only the sync theme triggers it).
        applyExtraKeysBarGlass(isBarBackgroundEnabled());

        // Light drop shadow under the terminal window.
        if (mTerminalCardContainer != null) {
            mTerminalCardContainer.setCardElevation(getPreferences().isTerminalShadowEnabled() ? 6 * density : 0f);
        }

        // Size the toolbar background views to the extra keys card.
        syncToolbarBackgroundViews();

        // Glass backdrop vs inner wallpaper, on an already-loaded wallpaper.
        if (mLastBackgroundBitmap != null) {
            if (glassEnabled) {
                // The glass blurs the wallpaper behind the window with a genuinely
                // light blur (~4dp) and, on API 33+, refracts the rim via the lens.
                applyGlassBackdrop(mLastBackgroundBitmap, getGlassBlurPx(density));
                if (mTerminalBackgroundImage != null) {
                    mTerminalBackgroundImage.setVisibility(View.GONE);
                }
                if (mTerminalBackgroundOverlay != null) {
                    mTerminalBackgroundOverlay.setVisibility(View.GONE);
                }
            } else {
                hideGlassBackdrop();
                if (mTerminalBackgroundImage != null) {
                    mTerminalBackgroundImage.setColorFilter(null);
                    applyFixedSceneMatrix(mTerminalBackgroundImage, mLastBackgroundBitmap);
                    if (!TextUtils.isEmpty(getPreferences().getTerminalBackgroundImagePath())) {
                        mTerminalBackgroundImage.setVisibility(View.VISIBLE);
                    }
                }
                if (mTerminalBackgroundOverlay != null
                    && !TextUtils.isEmpty(getPreferences().getTerminalBackgroundImagePath())) {
                    mTerminalBackgroundOverlay.setVisibility(View.VISIBLE);
                }
            }
            if (isBarBackgroundEnabled()) {
                applyToolbarBackgroundSlice(mLastBackgroundBitmap, mLastBackgroundBlurRadius,
                    mLastBackgroundBlurRadius * density);
            } else {
                hideToolbarBackground();
            }
            if (isTabBarBackgroundEnabled()) {
                applyTabBarBackgroundSlice(mLastBackgroundBitmap, mLastBackgroundBlurRadius,
                    mLastBackgroundBlurRadius * density);
            } else {
                hideTabBarBackground();
            }
        }
    }

    /**
     * Glass backdrop: draw the wallpaper behind the terminal window, cropped with
     * the same center-crop transform as the activity-level background so the scene
     * continues seamlessly across the window edges, then blur it. On API 31+ the
     * blur is a view RenderEffect (no pixel work); below that the window region is
     * cropped and blurred with RenderScript once per layout.
     */
    @SuppressLint("NewApi")
    private void applyGlassBackdrop(Bitmap bitmap, float blurPx) {
        if (mTerminalGlassBackdrop == null || mTerminalCardContainer == null) return;

        if (mTerminalCardContainer.getWidth() <= 0) {
            // Layout not ready yet: retry after the next layout pass.
            mTerminalCardContainer.post(() -> {
                if (!isFinishing() && mLastBackgroundBitmap == bitmap) {
                    applyGlassBackdrop(bitmap, blurPx);
                }
            });
            return;
        }

        int[] cardLoc = new int[2];
        mTerminalCardContainer.getLocationOnScreen(cardLoc);

        int rootW, rootH;
        int cardW = mTerminalCardContainer.getWidth();
        int cardH = mTerminalCardContainer.getHeight();
        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();
        if (cardW <= 0 || cardH <= 0 || bw <= 0 || bh <= 0) return;

        float s, srcX, srcY, srcW, srcH;
        Matrix matrix = new Matrix();

        // Fixed full-screen scene (same anchor as the sync slices and the outer
        // background): the wallpaper keeps its position on screen; the window is
        // just a window into it.
        int[] screen = getRealScreenSize();
        rootW = screen[0];
        rootH = screen[1];
        s = Math.max((float) rootW / bw, (float) rootH / bh);
        float dx = (rootW - bw * s) / 2f;
        float dy = (rootH - bh * s) / 2f;
        srcX = (cardLoc[0] - dx) / s;
        srcY = (cardLoc[1] - dy) / s;
        srcW = cardW / s;
        srcH = cardH / s;
        matrix.setScale(s, s);
        matrix.postTranslate(dx - cardLoc[0], dy - cardLoc[1]);

        // Temporary diagnostic for the right-edge column investigation.
        String sig = "backdrop: api=" + Build.VERSION.SDK_INT + " bmp=" + bw + "x" + bh
            + " scene=" + rootW + "x" + rootH + " s=" + s + " dx=" + dx + " dy=" + dy
            + " cardLoc=" + cardLoc[0] + "," + cardLoc[1] + " card=" + cardW + "x" + cardH
            + " backdropView=" + mTerminalGlassBackdrop.getWidth() + "x" + mTerminalGlassBackdrop.getHeight()
            + " frameLayer=" + (mTerminalFrameLayer != null
                ? mTerminalFrameLayer.getWidth() + "x" + mTerminalFrameLayer.getHeight() : "null");
        if (!sig.equals(mLastGlassDebugSig)) {
            mLastGlassDebugSig = sig;
            logGlassDebug(sig);
            if (mTerminalView != null && mTerminalView.mEmulator != null) {
                logGlassDebug("terminal: view=" + mTerminalView.getWidth() + "x" + mTerminalView.getHeight()
                    + " grid=" + mTerminalView.getPointX(mTerminalView.mEmulator.mColumns)
                    + " cols=" + mTerminalView.mEmulator.mColumns);
            }
        }

        // REGULAR-material adaptive tint scaled by the glass tint opacity preference:
        float luminance = computeRegionLuminance(bitmap, srcX, srcY, srcW, srcH);
        float tintFactor = getPreferences() != null
            ? Math.max(0f, Math.min(1f, getPreferences().getGlassTintOpacity() / 100f)) : 1f;
        float veilAlpha = adaptiveTintAlpha(luminance) * tintFactor;
        mLastVeilAlpha = veilAlpha;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mTerminalGlassBackdrop.setImageBitmap(bitmap);
            Drawable d = mTerminalGlassBackdrop.getDrawable();
            if (d instanceof BitmapDrawable) {
                ((BitmapDrawable) d).setAntiAlias(true);
                ((BitmapDrawable) d).setFilterBitmap(true);
                ((BitmapDrawable) d).setDither(true);
            }
            mTerminalGlassBackdrop.setScaleType(ImageView.ScaleType.MATRIX);
            mTerminalGlassBackdrop.setImageMatrix(matrix);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Liquid glass optics: AGSL squircle edge refraction + Fresnel rim + veil in GPU.
                try {
                    float density = getResources().getDisplayMetrics().density;
                    float cornerPx = getPreferences() != null && getPreferences().isTerminalBorderCornerEnabled()
                        ? getPreferences().getTerminalBorderCornerRadius() * density : 0f;
                    float rimPx = Math.min(22f * density, Math.min(cardW, cardH) * 0.25f);
                    float thickPx = rimPx * 0.65f;
                    RuntimeShader lensShader = new RuntimeShader(GLASS_LENS_SHADER);
                    lensShader.setFloatUniform("uSize", (float) cardW, (float) cardH);
                    lensShader.setFloatUniform("uRadius", cornerPx);
                    lensShader.setFloatUniform("uRim", rimPx);
                    lensShader.setFloatUniform("uThickness", thickPx);
                    lensShader.setFloatUniform("uIOR", 2.6f);
                    lensShader.setFloatUniform("uVeilAlpha", veilAlpha);
                    RenderEffect lensEffect = RenderEffect.createRuntimeShaderEffect(lensShader, "content");
                    if (blurPx > 0.5f) {
                        RenderEffect blurEffect = RenderEffect.createBlurEffect(
                            blurPx, blurPx, Shader.TileMode.CLAMP);
                        mTerminalGlassBackdrop.setRenderEffect(
                            RenderEffect.createChainEffect(lensEffect, blurEffect));
                    } else {
                        mTerminalGlassBackdrop.setRenderEffect(lensEffect);
                    }
                    if (mTerminalGlassTint != null) mTerminalGlassTint.setVisibility(View.GONE);
                } catch (Throwable t) {
                    // Fall back to a light blur + shape layer if the lens shader cannot run.
                    mTerminalGlassBackdrop.setRenderEffect(blurPx > 0.5f
                        ? RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP) : null);
                    if (mTerminalGlassTint != null) {
                        mTerminalGlassTint.setAlpha(veilAlpha);
                        mTerminalGlassTint.setVisibility(veilAlpha > 0.001f ? View.VISIBLE : View.GONE);
                    }
                }
            } else {
                mTerminalGlassBackdrop.setRenderEffect(blurPx > 0.5f
                    ? RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP) : null);
                if (mTerminalGlassTint != null) {
                    mTerminalGlassTint.setAlpha(veilAlpha);
                    mTerminalGlassTint.setVisibility(veilAlpha > 0.001f ? View.VISIBLE : View.GONE);
                }
            }
        } else {
            int ix = Math.max(0, Math.min(bw - 1, (int) srcX));
            int iy = Math.max(0, Math.min(bh - 1, (int) srcY));
            int iw = Math.min(bw - ix, Math.max(1, (int) srcW));
            int ih = Math.min(bh - iy, Math.max(1, (int) srcH));
            Bitmap crop = Bitmap.createBitmap(bitmap, ix, iy, iw, ih);
            if (blurPx > 0.5f) {
                crop = blurBitmapRenderScript(crop, (int) blurPx);
            }
            mTerminalGlassBackdrop.setImageBitmap(crop);
            mTerminalGlassBackdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mTerminalGlassBackdrop.setImageMatrix(null);
            if (mTerminalGlassTint != null) {
                mTerminalGlassTint.setAlpha(veilAlpha);
                mTerminalGlassTint.setVisibility(veilAlpha > 0.001f ? View.VISIBLE : View.GONE);
            }
        }
        mTerminalGlassBackdrop.setVisibility(View.VISIBLE);
    }

    /** Remove the glass backdrop (glass effect off or wallpaper cleared). */
    private void hideGlassBackdrop() {
        if (mTerminalGlassBackdrop == null) return;
        mTerminalGlassBackdrop.setVisibility(View.GONE);
        mTerminalGlassBackdrop.setImageBitmap(null);
        mTerminalGlassBackdrop.setImageMatrix(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mTerminalGlassBackdrop.setRenderEffect(null);
        }
        if (mTerminalGlassTint != null) {
            mTerminalGlassTint.setVisibility(View.GONE);
        }
    }

    /**
     * Wallpaper-sync mode for the extra keys bar: translucent dark glass card,
     * transparent pager and key buttons so the wallpaper slice shows through with
     * cleanly defined rounded corners and an elegant 1dp glass rim.
     */
    private void applyExtraKeysBarGlass(boolean enabled) {
        if (mTerminalToolbarCard == null || getPreferences() == null) return;

        if (enabled) {
            float density = getResources().getDisplayMetrics().density;
            boolean glassEnabled = getPreferences().isGlassEffectEnabled();
            boolean cornerEnabled = getPreferences().isExtraKeysBarCornerEnabled();
            int barRadius = cornerEnabled
                ? (int) (getPreferences().getExtraKeysBarBorderCornerRadius() * density) : 0;

            // In liquid glass mode, the AGSL shader renders the veil and Fresnel glint in GPU.
            // In plain wallpaper-sync mode, use a translucent dark background so corners are visible.
            mTerminalToolbarCard.setCardBackgroundColor(glassEnabled ? Color.TRANSPARENT : 0x400A0F19);
            mTerminalToolbarCard.setRadius(barRadius);
            mTerminalToolbarCard.setClipToOutline(barRadius > 0);

            if (getPreferences().isExtraKeysBarBorderEnabled()) {
                int barStrokeWidth = (int) (getPreferences().getExtraKeysBarBorderWidth() * density);
                int barStrokeColor;
                try {
                    barStrokeColor = Color.parseColor(getPreferences().getExtraKeysBarBorderColor());
                } catch (Exception e) {
                    barStrokeColor = 0xFFFFFFFF;
                }
                mTerminalToolbarCard.setStrokeWidth(barStrokeWidth);
                mTerminalToolbarCard.setStrokeColor(barStrokeColor);
            } else if (!glassEnabled && cornerEnabled && barRadius > 0) {
                mTerminalToolbarCard.setStrokeWidth(Math.max(1, Math.round(1f * density)));
                mTerminalToolbarCard.setStrokeColor(0x33FFFFFF);
            } else {
                mTerminalToolbarCard.setStrokeWidth(0);
            }

            if (mExtraKeysView != null) {
                mExtraKeysView.setBackgroundColor(Color.TRANSPARENT);
                int pressedTextColor = 0xFFFFFFFF;
                String customPressed = getPreferences().getExtraKeysPressedTextColor();
                if (!TextUtils.isEmpty(customPressed)) {
                    try {
                        pressedTextColor = Color.parseColor(customPressed);
                    } catch (Exception ignored) {}
                }
                // Liquid glass pill button effect: soft translucent frosted capsule on press/toggle
                int activeBg = glassEnabled
                    ? Color.argb(0x40, Color.red(pressedTextColor), Color.green(pressedTextColor), Color.blue(pressedTextColor))
                    : 0x33FFFFFF;
                int keyCorner = (int) (6 * density);
                mExtraKeysView.setButtonCornerRadius(keyCorner);
                mExtraKeysView.setButtonColors(Color.WHITE, pressedTextColor, Color.TRANSPARENT, activeBg);
                mExtraKeysView.setButtonStrokeWidth(0);
                // setButtonColors only stores values; repaint every existing button now
                // so no stale rectangular backgrounds linger until a key is pressed.
                mExtraKeysView.restyleButtons();
            }
            ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
            if (terminalToolbarViewPager != null) {
                terminalToolbarViewPager.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    /** Real gaussian blur via RenderScript (framework API, available on API 21-30). */
    @SuppressWarnings("deprecation")
    private Bitmap blurBitmapRenderScript(Bitmap src, int radiusPx) {
        try {
            RenderScript rs = RenderScript.create(this);
            Allocation input = Allocation.createFromBitmap(rs, src, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            Allocation output = Allocation.createTyped(rs, input.getType());
            ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            blur.setRadius(Math.max(1f, Math.min(25f, radiusPx)));
            blur.setInput(input);
            blur.forEach(output);
            output.copyTo(src);
            input.destroy();
            output.destroy();
            blur.destroy();
            rs.destroy();
            return src;
        } catch (Throwable t) {
            Logger.logStackTraceWithMessage(LOG_TAG, "RenderScript blur failed, using unblurred wallpaper", t);
            return src;
        }
    }

    public void reloadExtraKeysStylingAndConfig() {
        if (mTermuxTerminalExtraKeys != null) {
            mTermuxTerminalExtraKeys.reloadExtraKeys();
        }
        setTerminalToolbarHeight();
        if (mExtraKeysView != null && getPreferences() != null) {
            float density = getResources().getDisplayMetrics().density;
            String themeName = getPreferences().getExtraKeysColorTheme();
            com.termux.shared.termux.extrakeys.ExtraKeysTheme theme =
                com.termux.shared.termux.extrakeys.ExtraKeysTheme.getTheme(this, themeName);

            int btnBg = theme.buttonBackgroundColor;
            if (getPreferences().isExtraKeysFlatKeys()) {
                btnBg = theme.barBackgroundColor;
            }

            int activeTextColor = theme.buttonActiveTextColor;
            String customPressed = getPreferences().getExtraKeysPressedTextColor();
            if (!TextUtils.isEmpty(customPressed)) {
                try {
                    activeTextColor = Color.parseColor(customPressed);
                } catch (Exception ignored) {}
            }

            int keyCorner = (int) (2 * density);
            int activeBg;
            if (getPreferences().isExtraKeysFlatKeys()) {
                // Subtle capsule highlight matching button corner radius, eliminating sharp rectangular boxes
                activeBg = Color.argb(0x35, Color.red(activeTextColor), Color.green(activeTextColor), Color.blue(activeTextColor));
            } else {
                activeBg = theme.buttonActiveBackgroundColor;
            }

            mExtraKeysView.setButtonColors(
                theme.buttonTextColor,
                activeTextColor,
                btnBg,
                activeBg
            );
            mExtraKeysView.setBackgroundColor(theme.barBackgroundColor);

            final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
            if (terminalToolbarViewPager != null) {
                terminalToolbarViewPager.setBackgroundColor(theme.barBackgroundColor);
            }

            if (mTerminalToolbarCard != null && getPreferences().isExtraKeysBarBorderEnabled()) {
                mTerminalToolbarCard.setCardBackgroundColor(theme.barBackgroundColor);
            }

            mExtraKeysView.setButtonCornerRadius(keyCorner);
            mExtraKeysView.setButtonMargin((int) (getPreferences().getExtraKeysMargin() * density));
            mExtraKeysView.setButtonTextSizeSp(getPreferences().getExtraKeysTextSize());
            mExtraKeysView.setButtonStrokeWidth(0);

            if (mProperties != null) {
                mExtraKeysView.setButtonTextAllCaps(mProperties.shouldExtraKeysTextBeAllCaps());
            }

            float toolbarHeight = mTerminalToolbarDefaultHeight;
            if (getPreferences() != null) {
                toolbarHeight = mTerminalToolbarDefaultHeight * (getPreferences().getExtraKeysHeightScale() / 100.0f);
            }
            if (mTermuxTerminalExtraKeys != null) {
                mExtraKeysView.reload(mTermuxTerminalExtraKeys.getExtraKeysInfo(), toolbarHeight);
            }
        }
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public boolean isOnResumeAfterOnCreate() {
        return mIsOnResumeAfterOnCreate;
    }

    public boolean isActivityRecreated() {
        return mIsActivityRecreated;
    }



    public TermuxService getTermuxService() {
        return mTermuxService;
    }

    public TerminalView getTerminalView() {
        return mTerminalView;
    }

    public TermuxTerminalViewClient getTermuxTerminalViewClient() {
        return mTermuxTerminalViewClient;
    }

    public TermuxTerminalSessionActivityClient getTermuxTerminalSessionClient() {
        return mTermuxTerminalSessionActivityClient;
    }

    @Nullable
    public TerminalSession getCurrentSession() {
        if (mTerminalView != null)
            return mTerminalView.getCurrentSession();
        else
            return null;
    }

    public TermuxAppSharedPreferences getPreferences() {
        return mPreferences;
    }

    public TermuxAppSharedProperties getProperties() {
        return mProperties;
    }




    public static void updateTermuxActivityStyling(Context context, boolean recreateActivity) {
        // Make sure that terminal styling is always applied.
        Intent stylingIntent = new Intent(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);
        stylingIntent.putExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, recreateActivity);
        context.sendBroadcast(stylingIntent);
    }

    private void registerTermuxActivityBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH);
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);

        registerReceiver(mTermuxActivityBroadcastReceiver, intentFilter);
    }

    private void unregisterTermuxActivityBroadcastReceiver() {
        unregisterReceiver(mTermuxActivityBroadcastReceiver);
    }

    private void fixTermuxActivityBroadcastReceiverIntent(Intent intent) {
        if (intent == null) return;

        String extraReloadStyle = intent.getStringExtra(TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE);
        if ("storage".equals(extraReloadStyle)) {
            intent.removeExtra(TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE);
            intent.setAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);
        }
    }

    class TermuxActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            if (mIsVisible) {
                fixTermuxActivityBroadcastReceiverIntent(intent);

                switch (intent.getAction()) {
                    case TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH:
                        Logger.logDebug(LOG_TAG, "Received intent to notify app crash");
                        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(context, LOG_TAG);
                        return;
                    case TERMUX_ACTIVITY.ACTION_RELOAD_STYLE:
                        Logger.logDebug(LOG_TAG, "Received intent to reload styling");
                        reloadActivityStyling(intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, true));
                        return;
                    case TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS:
                        Logger.logDebug(LOG_TAG, "Received intent to request storage permissions");
                        requestStoragePermission(false);
                        return;
                    default:
                }
            } else {
                if (intent != null && TERMUX_ACTIVITY.ACTION_RELOAD_STYLE.equals(intent.getAction())) {
                    mPendingReloadStyling = true;
                }
            }
        }
    }

    private void reloadActivityStyling(boolean recreateActivity) {
        if (mProperties != null) {
            reloadProperties();

            // Update NightMode.APP_NIGHT_MODE
            TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());
        }

        setMargins();

        FileReceiverActivity.updateFileReceiverActivityComponentsState(this);

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onReloadActivityStyling();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadActivityStyling();

        updateTerminalAppearance();

        // To change the activity and drawer theme, activity needs to be recreated.
        // It will destroy the activity, including all stored variables and views, and onCreate()
        // will be called again. Extra keys input text, terminal sessions and transcripts will be preserved.
        if (recreateActivity) {
            Logger.logDebug(LOG_TAG, "Recreating activity");
            TermuxActivity.this.recreate();
        }
    }



    public static void startTermuxActivity(@NonNull final Context context) {
        ActivityUtils.startActivity(context, newInstance(context));
    }

    public static Intent newInstance(@NonNull final Context context) {
        Intent intent = new Intent(context, TermuxActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public boolean isTabAnimationEnabled() {
        return getPreferences() != null && getPreferences().isTabAnimationEnabled();
    }

    public TermuxGesturePillView getGesturePillView() {
        return mTermuxGesturePillView;
    }

    private void syncSnapshotMargins(ViewGroup.MarginLayoutParams viewLp) {
        if (viewLp == null) return;
        for (ImageView snap : new ImageView[]{mTerminalCardSnapshot, mTerminalCardIncomingSnapshot}) {
            if (snap != null) {
                ViewGroup.MarginLayoutParams snapLp = (ViewGroup.MarginLayoutParams) snap.getLayoutParams();
                if (snapLp != null) {
                    snapLp.setMargins(viewLp.leftMargin, viewLp.topMargin, viewLp.rightMargin, viewLp.bottomMargin);
                    snap.setLayoutParams(snapLp);
                }
            }
        }
    }

    private void captureSessionSnapshot(Bitmap targetBitmap, View sourceView) {
        if (targetBitmap == null || sourceView == null) return;
        try {
            targetBitmap.eraseColor(Color.TRANSPARENT);
            android.graphics.Canvas canvas = new android.graphics.Canvas(targetBitmap);
            if (getPreferences() == null || !getPreferences().isGlassEffectEnabled()) {
                if (mLastBackgroundBitmap == null || mLastBackgroundBitmap.isRecycled()) {
                    int bgColor = 0xFF000000;
                    TerminalSession current = getCurrentSession();
                    if (current != null && current.getEmulator() != null) {
                        bgColor = current.getEmulator().mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND];
                    }
                    canvas.drawColor(bgColor);
                }
            }
            sourceView.draw(canvas);
        } catch (Throwable t) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to capture transition snapshot", t);
        }
    }

    public void animateTabTransition(boolean forward, Runnable onAttachSession) {
        if (!isTabAnimationEnabled() || mTerminalView == null
                || mTerminalCardSnapshot == null || mTerminalCardIncomingSnapshot == null || !mIsVisible) {
            onAttachSession.run();
            return;
        }

        int width = mTerminalView.getWidth();
        int height = mTerminalView.getHeight();
        if (width <= 0 || height <= 0) {
            onAttachSession.run();
            return;
        }

        if (mTabTransitionAnimator != null) {
            mTabTransitionAnimator.cancel();
            mTabTransitionAnimator = null;
        }

        // 1. Allocate / reuse both snapshot bitmaps
        if (mTransitionSnapshotBitmapA == null || mTransitionSnapshotBitmapA.isRecycled()
                || mTransitionSnapshotBitmapA.getWidth() != width || mTransitionSnapshotBitmapA.getHeight() != height) {
            if (mTransitionSnapshotBitmapA != null && !mTransitionSnapshotBitmapA.isRecycled()) mTransitionSnapshotBitmapA.recycle();
            if (mTransitionSnapshotBitmapB != null && !mTransitionSnapshotBitmapB.isRecycled()) mTransitionSnapshotBitmapB.recycle();
            try {
                mTransitionSnapshotBitmapA = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mTransitionSnapshotBitmapB = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            } catch (Throwable t) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to allocate transition snapshot bitmaps", t);
                mTransitionSnapshotBitmapA = null;
                mTransitionSnapshotBitmapB = null;
                onAttachSession.run();
                return;
            }
        }

        // 2. Capture Snapshot A (Old Session text):
        captureSessionSnapshot(mTransitionSnapshotBitmapA, mTerminalView);
        mTerminalCardSnapshot.setImageBitmap(mTransitionSnapshotBitmapA);

        // 3. Attach New Session:
        onAttachSession.run();

        // 4. Capture Snapshot B (New Session text):
        captureSessionSnapshot(mTransitionSnapshotBitmapB, mTerminalView);
        mTerminalCardIncomingSnapshot.setImageBitmap(mTransitionSnapshotBitmapB);

        // 5. Keep live terminal view attached and focused, but alpha = 0f during the slide:
        mTerminalView.setAlpha(0f);

        // Configure layout params for both snapshots matching mTerminalView:
        ViewGroup.MarginLayoutParams viewLp = (ViewGroup.MarginLayoutParams) mTerminalView.getLayoutParams();
        syncSnapshotMargins(viewLp);

        int marginHorizontal = viewLp != null ? (viewLp.leftMargin + viewLp.rightMargin) : 0;
        final float slideDistance = width + marginHorizontal;
        final float startNewX = forward ? slideDistance : -slideDistance;
        final float endOldX = forward ? -slideDistance : slideDistance;

        mTerminalCardSnapshot.setTranslationX(0f);
        mTerminalCardSnapshot.setAlpha(1.0f);
        mTerminalCardSnapshot.setVisibility(View.VISIBLE);

        mTerminalCardIncomingSnapshot.setTranslationX(startNewX);
        mTerminalCardIncomingSnapshot.setAlpha(1.0f);
        mTerminalCardIncomingSnapshot.setVisibility(View.VISIBLE);

        // 6. Animate both snapshots sliding side by side on the Niri ribbon:
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(280);
        animator.setInterpolator(new android.view.animation.PathInterpolator(0.18f, 0.9f, 0.2f, 1.0f));
        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            if (mTerminalCardSnapshot != null) {
                mTerminalCardSnapshot.setTranslationX(endOldX * fraction);
            }
            if (mTerminalCardIncomingSnapshot != null) {
                mTerminalCardIncomingSnapshot.setTranslationX(startNewX * (1f - fraction));
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                cleanupTransitionSnapshot();
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                cleanupTransitionSnapshot();
            }
        });

        mTabTransitionAnimator = animator;
        animator.start();
    }

    private void cleanupTransitionSnapshot() {
        if (mTerminalCardSnapshot != null) {
            mTerminalCardSnapshot.setVisibility(View.INVISIBLE);
            mTerminalCardSnapshot.setImageBitmap(null);
            mTerminalCardSnapshot.setTranslationX(0f);
            mTerminalCardSnapshot.setAlpha(1.0f);
        }
        if (mTerminalCardIncomingSnapshot != null) {
            mTerminalCardIncomingSnapshot.setVisibility(View.INVISIBLE);
            mTerminalCardIncomingSnapshot.setImageBitmap(null);
            mTerminalCardIncomingSnapshot.setTranslationX(0f);
            mTerminalCardIncomingSnapshot.setAlpha(1.0f);
        }
        if (mTerminalView != null) {
            mTerminalView.setAlpha(1.0f);
            mTerminalView.requestFocus();
        }
        mTabTransitionAnimator = null;
    }

}
