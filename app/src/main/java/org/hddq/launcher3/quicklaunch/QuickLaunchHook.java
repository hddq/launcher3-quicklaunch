package org.hddq.launcher3.quicklaunch;

import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed module that hooks Launcher3's AllAppsSearchBarController to launch
 * the first visible app from search results when the user presses Enter.
 *
 * This replicates the behaviour from the LineageOS Gerrit patch:
 * https://review.lineageos.org/c/LineageOS/android_packages_apps_Launcher3/+/489879
 */
public class QuickLaunchHook implements IXposedHookLoadPackage {

    private static final String TAG = "QuickLaunch";
    private static final String TARGET_PACKAGE = "com.android.launcher3";
    private static final String CONTROLLER_CLASS =
            "com.android.launcher3.allapps.search.AllAppsSearchBarController";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) {
            return;
        }

        XposedBridge.log(TAG + ": Loaded in " + lpparam.packageName);

        try {
            hookSearchBarController(lpparam);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook AllAppsSearchBarController: " + t);
        }
    }

    private void hookSearchBarController(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> controllerClass = XposedHelpers.findClass(CONTROLLER_CLASS, lpparam.classLoader);

        XposedHelpers.findAndHookMethod(
                controllerClass,
                "onEditorAction",
                TextView.class, int.class, KeyEvent.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Only act when the original method returned false (didn't handle it)
                        Object result = param.getResult();
                        if (result instanceof Boolean && (Boolean) result) {
                            return;
                        }

                        int actionId = (int) param.args[1];
                        KeyEvent event = (KeyEvent) param.args[2];

                        // Only handle search action or Enter key press
                        boolean isSearchAction = (actionId == EditorInfo.IME_ACTION_SEARCH);
                        boolean isEnterKey = (event != null
                                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                                && event.getAction() == KeyEvent.ACTION_DOWN);

                        if (!isSearchAction && !isEnterKey) {
                            return;
                        }

                        try {
                            if (launchFirstSearchResult(param.thisObject)) {
                                param.setResult(true);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": Error launching first result: " + t);
                        }
                    }

                    private boolean launchFirstSearchResult(Object controller) throws Throwable {
                        // controller.mLauncher -> ActivityContext
                        Object mLauncher = XposedHelpers.getObjectField(controller, "mLauncher");

                        // mLauncher.getAppsView() -> ActivityAllAppsContainerView
                        Object appsView = XposedHelpers.callMethod(mLauncher, "getAppsView");

                        // appsView.getSearchRecyclerView() -> SearchRecyclerView
                        Object searchRv = XposedHelpers.callMethod(appsView, "getSearchRecyclerView");
                        if (searchRv == null) {
                            return false;
                        }

                        ViewGroup rv = (ViewGroup) searchRv;
                        if (rv.getChildCount() == 0) {
                            return false;
                        }

                        View targetView = findFirstAppIcon(rv);
                        if (targetView != null) {
                            Object tag = targetView.getTag();
                            Intent intent = getIntentFromItemInfo(tag);
                            if (intent != null) {
                                Object launched = XposedHelpers.callMethod(
                                        mLauncher, "startActivitySafely",
                                        targetView, intent, tag
                                );
                                if (launched != null) {
                                    XposedBridge.log(TAG + ": Launched first search result");
                                    return true;
                                }
                            }
                        }
                        return false;
                    }

                    private View findFirstAppIcon(ViewGroup parent) {
                        for (int i = 0; i < parent.getChildCount(); i++) {
                            View child = parent.getChildAt(i);
                            if (child == null) continue;

                            Object tag = child.getTag();
                            if (tag != null && isItemInfo(tag)) {
                                Intent intent = getIntentFromItemInfo(tag);
                                // Make sure we only click things that can actually be launched
                                if (intent != null && intent.getComponent() != null) {
                                    return child;
                                }
                            }

                            if (child instanceof ViewGroup) {
                                View found = findFirstAppIcon((ViewGroup) child);
                                if (found != null) {
                                    return found;
                                }
                            }
                        }
                        return null;
                    }

                    private Intent getIntentFromItemInfo(Object tag) {
                        try {
                            return (Intent) XposedHelpers.callMethod(tag, "getIntent");
                        } catch (Throwable t) {
                            // ignore
                        }
                        try {
                            return (Intent) XposedHelpers.getObjectField(tag, "intent");
                        } catch (Throwable t) {
                            // ignore
                        }
                        return null;
                    }

                    /**
                     * Check by class name to avoid compile-time dependency on Launcher3 classes.
                     */
                    private boolean isBubbleTextView(View view) {
                        String className = view.getClass().getName();
                        return className.endsWith(".BubbleTextView");
                    }

                    /**
                     * Walk the class hierarchy to check if the object is an ItemInfo subclass.
                     */
                    private boolean isItemInfo(Object obj) {
                        Class<?> cls = obj.getClass();
                        while (cls != null) {
                            if (cls.getName().endsWith(".ItemInfo")) {
                                return true;
                            }
                            cls = cls.getSuperclass();
                        }
                        return false;
                    }
                }
        );

        XposedBridge.log(TAG + ": Hooked " + CONTROLLER_CLASS + ".onEditorAction()");
    }
}
