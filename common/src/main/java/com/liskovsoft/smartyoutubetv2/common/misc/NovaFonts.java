package com.liskovsoft.smartyoutubetv2.common.misc;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.LayoutInflaterCompat;

import com.liskovsoft.sharedutils.mylogger.Log;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * NovaTV: applies the bundled Inter font to every inflated TextView (and subclasses).<br/>
 * Works on all API levels (android:fontFamily from xml needs API 26, the app supports 17+).<br/>
 * Views created in code keep the system font. If anything goes wrong the stock inflation is used.
 */
public final class NovaFonts {
    private static final String TAG = NovaFonts.class.getSimpleName();
    private static final String FONT_REGULAR = "fonts/inter_regular.ttf";
    private static final String FONT_MEDIUM = "fonts/inter_medium.ttf";
    private static final String FONT_SEMIBOLD = "fonts/inter_semibold.ttf";
    private static final String[] PREFIXES = {"android.widget.", "android.view.", "android.webkit."};
    private static final Class<?>[] CONSTRUCTOR_SIGNATURE = {Context.class, AttributeSet.class};
    private static final Map<String, Constructor<? extends View>> sConstructors = new HashMap<>();
    private static Typeface sRegular;
    private static Typeface sMedium;
    private static Typeface sSemiBold;
    private static boolean sLoadFailed;

    private NovaFonts() {
    }

    /**
     * Call before super.onCreate() of the activity.
     */
    public static void install(Activity activity) {
        if (!loadTypefaces(activity)) {
            return;
        }

        LayoutInflater inflater = activity.getLayoutInflater();

        if (inflater.getFactory() != null) {
            return; // someone else owns the factory
        }

        try {
            LayoutInflaterCompat.setFactory2(inflater, new Factory(inflater));
        } catch (IllegalStateException e) {
            Log.e(TAG, "Can't install font factory: %s", e.getMessage());
        }
    }

    /**
     * Apply the font to a view created in code.
     */
    public static void apply(TextView view) {
        if (view == null || !loadTypefaces(view.getContext())) {
            return;
        }

        view.setTypeface(pick(view.getTypeface()));
    }

    private static synchronized boolean loadTypefaces(Context context) {
        if (sRegular != null) {
            return true;
        }

        if (sLoadFailed) {
            return false;
        }

        try {
            sRegular = Typeface.createFromAsset(context.getAssets(), FONT_REGULAR);
            sMedium = Typeface.createFromAsset(context.getAssets(), FONT_MEDIUM);
            sSemiBold = Typeface.createFromAsset(context.getAssets(), FONT_SEMIBOLD);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Inter font not found, using the system font: %s", e.getMessage());
            sRegular = sMedium = sSemiBold = null;
            sLoadFailed = true;
            return false;
        }
    }

    private static Typeface pick(@Nullable Typeface current) {
        if (current == null) {
            return sRegular;
        }

        if (current.isBold()) {
            return sSemiBold;
        }

        if (Build.VERSION.SDK_INT >= 28 && current.getWeight() >= 500) {
            return sMedium;
        }

        return sRegular;
    }

    private static final class Factory implements LayoutInflater.Factory2 {
        private final LayoutInflater mInflater;

        Factory(LayoutInflater inflater) {
            mInflater = inflater;
        }

        @Override
        public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
            // Let the activity handle <fragment> and anything that needs inflater internals
            if (name.equals("fragment") || name.equals("ViewStub") || name.equals("include")
                    || name.equals("merge") || name.equals("requestFocus") || name.equals("tag") || name.equals("blink")) {
                return null;
            }

            View view = create(name, context, attrs);

            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                Typeface typeface = textView.getTypeface();
                // Keep monospace/serif text (e.g. debug info) untouched
                if (typeface == null || (!Typeface.MONOSPACE.equals(typeface) && !Typeface.SERIF.equals(typeface))) {
                    textView.setTypeface(pick(typeface));
                }
            }

            return view;
        }

        @Override
        public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
            return onCreateView(null, name, context, attrs);
        }

        @Nullable
        private View create(String name, Context context, AttributeSet attrs) {
            if (name.indexOf('.') != -1) {
                return newInstance(name, context, attrs);
            }

            for (String prefix : PREFIXES) {
                View view = newInstance(prefix + name, context, attrs);
                if (view != null) {
                    return view;
                }
            }

            return null;
        }

        @Nullable
        private View newInstance(String className, Context context, AttributeSet attrs) {
            try {
                Constructor<? extends View> constructor = sConstructors.get(className);

                if (constructor == null) {
                    if (sConstructors.containsKey(className)) {
                        return null; // known miss
                    }
                    ClassLoader loader = context.getClassLoader();
                    Class<? extends View> clazz = Class.forName(className, false, loader).asSubclass(View.class);
                    constructor = clazz.getConstructor(CONSTRUCTOR_SIGNATURE);
                    constructor.setAccessible(true);
                    sConstructors.put(className, constructor);
                }

                return constructor.newInstance(context, attrs);
            } catch (ClassNotFoundException | NoSuchMethodException | ClassCastException e) {
                sConstructors.put(className, null);
                return null;
            } catch (Exception e) {
                // Constructor threw: fall back to the default inflater path
                return null;
            }
        }
    }
}
