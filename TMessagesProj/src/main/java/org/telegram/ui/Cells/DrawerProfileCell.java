/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.ui.Cells;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.telegram.tpro1.R;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;

public class DrawerProfileCell extends FrameLayout {

    private BackupImageView avatarImageView;
    private TextView nameTextView;
    private TextView phoneTextView;
    private ImageView shadowView;
    private CloudView cloudView;
    private Rect srcRect = new Rect();
    private Rect destRect = new Rect();
    private Paint paint = new Paint();
    private Integer currentColor;
    private Drawable cloudDrawable;
    private int lastCloudColor;

    private NightView nightView;
    private Drawable night;

    private PowerView powerView;
    private Drawable power;

    private SharedPreferences preferences;



    private class CloudView extends View {

        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public CloudView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (Theme.isCustomTheme() && Theme.getCachedWallpaper() != null) {
                paint.setColor(Theme.getServiceMessageColor());
            } else {
                paint.setColor(Theme.getColor(Theme.key_chats_menuCloudBackgroundCats));
            }
            int newColor = Theme.getColor(Theme.key_chats_menuCloud);
            if (lastCloudColor != newColor) {
                cloudDrawable.setColorFilter(new PorterDuffColorFilter(lastCloudColor = Theme.getColor(Theme.key_chats_menuCloud), PorterDuff.Mode.MULTIPLY));
            }
            canvas.drawCircle(getMeasuredWidth() / 2.0f, getMeasuredHeight() / 2.0f, AndroidUtilities.dp(34) / 2.0f, paint);
            int l = (getMeasuredWidth() - AndroidUtilities.dp(24)) / 2;
            int t = (getMeasuredHeight() - AndroidUtilities.dp(24)) / 2 + AndroidUtilities.dp(0.5f);
            cloudDrawable.setBounds(l, t, l + AndroidUtilities.dp(24), t + AndroidUtilities.dp(24));
            cloudDrawable.draw(canvas);
        }
    }

    private class PowerView extends View {

        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public PowerView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (Theme.isCustomTheme() && Theme.getCachedWallpaper() != null) {
                paint.setColor(Theme.getServiceMessageColor());
            } else {
                paint.setColor(Theme.getColor(Theme.key_chats_menuCloudBackgroundCats));
            }
            int newColor = Theme.getColor(Theme.key_chats_menuCloud);
            if (lastCloudColor != newColor) {
                power.setColorFilter(new PorterDuffColorFilter(lastCloudColor = Theme.getColor(Theme.key_chats_menuCloud), PorterDuff.Mode.MULTIPLY));
            }
            canvas.drawCircle(getMeasuredWidth() / 2.0f, getMeasuredHeight() / 2.0f, AndroidUtilities.dp(34) / 2.0f, paint);
            int l = (getMeasuredWidth() - AndroidUtilities.dp(33)) / 2;
            int t = (getMeasuredHeight() - AndroidUtilities.dp(33)) / 2;
            power.setBounds(l, t, l + AndroidUtilities.dp(33), t + AndroidUtilities.dp(33));
            power.draw(canvas);

        }
    }


    private class NightView extends View {

        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public NightView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (Theme.isCustomTheme() && Theme.getCachedWallpaper() != null) {
                paint.setColor(Theme.getServiceMessageColor());
            } else {
                paint.setColor(Theme.getColor(Theme.key_chats_menuCloudBackgroundCats));
            }
            int newColor = Theme.getColor(Theme.key_chats_menuCloud);
            if (lastCloudColor != newColor) {
                night.setColorFilter(new PorterDuffColorFilter(lastCloudColor = Theme.getColor(Theme.key_chats_menuCloud), PorterDuff.Mode.MULTIPLY));
            }
            canvas.drawCircle(getMeasuredWidth() / 2.0f, getMeasuredHeight() / 2.0f, AndroidUtilities.dp(34) / 2.0f, paint);
            int l = (getMeasuredWidth() - AndroidUtilities.dp(33)) / 2;
            int t = (getMeasuredHeight() - AndroidUtilities.dp(33)) / 2;
            night.setBounds(l, t, l + AndroidUtilities.dp(33), t + AndroidUtilities.dp(33));
            night.draw(canvas);

        }
    }

    public DrawerProfileCell(final Context context) {
        super(context);

        cloudDrawable = context.getResources().getDrawable(R.drawable.cloud);
        cloudDrawable.setColorFilter(new PorterDuffColorFilter(lastCloudColor = Theme.getColor(Theme.key_chats_menuCloud), PorterDuff.Mode.MULTIPLY));
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        night = context.getResources().getDrawable(preferences.getBoolean("isNightMode", false) ? R.drawable.light_mode : R.drawable.might_mode);
        night.setColorFilter(preferences.getBoolean("isNightMode", false) ? 0xFFFFCC00 : 0xffFFFFFF, PorterDuff.Mode.SRC_ATOP);


        shadowView = new ImageView(context);
        shadowView.setVisibility(INVISIBLE);
        shadowView.setScaleType(ImageView.ScaleType.FIT_XY);
        shadowView.setImageResource(R.drawable.bottom_shadow);
        addView(shadowView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 70, Gravity.LEFT | Gravity.BOTTOM));

        avatarImageView = new BackupImageView(context);
        avatarImageView.getImageReceiver().setRoundRadius(AndroidUtilities.dp(32));
        addView(avatarImageView, LayoutHelper.createFrame(64, 64, Gravity.LEFT | Gravity.BOTTOM, 16, 0, 0, 67));



        nameTextView = new TextView(context);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        nameTextView.setLines(1);
        nameTextView.setMaxLines(1);
        nameTextView.setSingleLine(true);
        nameTextView.setGravity(Gravity.LEFT);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 16, 0, 76, 28));

        phoneTextView = new TextView(context);
        phoneTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        phoneTextView.setLines(1);
        phoneTextView.setMaxLines(1);
        phoneTextView.setSingleLine(true);
        phoneTextView.setGravity(Gravity.LEFT);
        addView(phoneTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 16, 0, 76, 9));

        cloudView = new CloudView(context);
        addView(cloudView, LayoutHelper.createFrame(61, 61, Gravity.RIGHT | Gravity.BOTTOM));

        nightView = new NightView(context);
        addView(nightView, LayoutHelper.createFrame(61, 61, Gravity.RIGHT | Gravity.TOP,0,16,0,0));
        nightView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangeTheme();
            }
        });


        power = context.getResources().getDrawable(R.drawable.off_on);
        power.setColorFilter(preferences.getBoolean("isPoertMode", false) ? 0xfffa8072 : 0xffFFFFFF, PorterDuff.Mode.SRC_ATOP);
        powerView = new PowerView(context);

        addView(powerView, LayoutHelper.createFrame(61, 61, Gravity.RIGHT | Gravity.BOTTOM, 0, 0, 50, 0));

        powerView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangePower(context);

            }
        });
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (Build.VERSION.SDK_INT >= 21) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(148) + AndroidUtilities.statusBarHeight, MeasureSpec.EXACTLY));
        } else {
            try {
                super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(148), MeasureSpec.EXACTLY));
            } catch (Exception e) {
                setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(148));
                FileLog.e(e);
            }
        }

        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("BaseConfig", Activity.MODE_PRIVATE);
        if (plusPreferences.getBoolean("hideMobile", false) && !plusPreferences.getBoolean("showUsername", false)) {
            phoneTextView.setVisibility(GONE);
        } else {
            phoneTextView.setVisibility(VISIBLE);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable backgroundDrawable = Theme.getCachedWallpaper();
        int color;
        if (Theme.hasThemeKey(Theme.key_chats_menuTopShadow)) {
            color = Theme.getColor(Theme.key_chats_menuTopShadow);
        } else {
            color = Theme.getServiceMessageColor() | 0xff000000;
        }
        if (currentColor == null || currentColor != color) {
            currentColor = color;
            shadowView.getDrawable().setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        }
        nameTextView.setTextColor(Theme.getColor(Theme.key_chats_menuName));
        if (Theme.isCustomTheme() && backgroundDrawable != null) {
            phoneTextView.setTextColor(Theme.getColor(Theme.key_chats_menuPhone));
            shadowView.setVisibility(VISIBLE);
            if (backgroundDrawable instanceof ColorDrawable) {
                backgroundDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                backgroundDrawable.draw(canvas);
            } else if (backgroundDrawable instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
                float scaleX = (float) getMeasuredWidth() / (float) bitmap.getWidth();
                float scaleY = (float) getMeasuredHeight() / (float) bitmap.getHeight();
                float scale = scaleX < scaleY ? scaleY : scaleX;
                int width = (int) (getMeasuredWidth() / scale);
                int height = (int) (getMeasuredHeight() / scale);
                int x = (bitmap.getWidth() - width) / 2;
                int y = (bitmap.getHeight() - height) / 2;
                srcRect.set(x, y, x + width, y + height);
                destRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                try {
                    canvas.drawBitmap(bitmap, srcRect, destRect, paint);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        } else {
            shadowView.setVisibility(INVISIBLE);
            phoneTextView.setTextColor(Theme.getColor(Theme.key_chats_menuPhoneCats));
            super.onDraw(canvas);
        }
    }

    public void setUser(TLRPC.User user) {
        if (user == null) {
            return;
        }
        TLRPC.FileLocation photo = null;
        if (user.photo != null) {
            photo = user.photo.photo_small;
        }
        nameTextView.setText(UserObject.getUserName(user));

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("BaseConfig", Activity.MODE_PRIVATE);
        String value;
        if (preferences.getBoolean("showUsername", false)) {
            if (user.username != null && user.username.length() != 0) {
                value = "@" + user.username;
            } else {
                value = LocaleController.getString("UsernameEmpty", R.string.UsernameEmpty);
            }
        } else {
            value = PhoneFormat.getInstance().format("+" + user.phone);
        }

        phoneTextView.setText(PhoneFormat.getInstance().format(value));
        AvatarDrawable avatarDrawable = new AvatarDrawable(user);
        avatarDrawable.setColor(Theme.getColor(Theme.key_avatar_backgroundInProfileBlue));
        avatarImageView.setImage(photo, "50_50", avatarDrawable);

        updateTheme();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        cloudView.invalidate();
        nightView.invalidate();
        powerView.invalidate();
    }

    private void updateTheme() {

        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("BaseConfig", Activity.MODE_PRIVATE);

        if (plusPreferences.getBoolean("hideMobile", false) && !plusPreferences.getBoolean("showUsername", false)) {
            phoneTextView.setVisibility(GONE);
        } else {
            phoneTextView.setVisibility(VISIBLE);
        }

    }

    private void ChangeTheme() {
        boolean isNightMode = preferences.getBoolean("isNightMode", false);
        String latTheme = preferences.getString("LastTheme", null);
        preferences.edit().putBoolean("isNightMode", !isNightMode).commit();
        if (isNightMode) {

            for (int i = 0; i < Theme.themes.size(); i++) {
                if (Theme.themes.get(i).getName2().equalsIgnoreCase(latTheme)) {
                    Theme.applyTheme(Theme.themes.get(i));
                }
            }
        } else {

            Theme.ThemeInfo current = Theme.getCurrentTheme();
            preferences.edit().putString("LastTheme", current.getName2()).commit();
            for (int i = 0; i < Theme.themes.size(); i++) {
                if (Theme.themes.get(i).getName2().equalsIgnoreCase("Dark")) {
                    Theme.applyTheme(Theme.themes.get(i));
                }
            }
        }
        night = getResources().getDrawable(!isNightMode ? R.drawable.light_mode : R.drawable.might_mode);
        night.setColorFilter(!isNightMode ? 0xFFFFCC00 : 0xffFFFFFF, PorterDuff.Mode.SRC_ATOP);
        nightView.invalidate();
        DialogsActivity.formProfileCell = true;
        NotificationCenter.getInstance().postNotificationName(NotificationCenter.didSetNewTheme);


//        Utilities.restartApp();
    }


    private void ChangePower(Context context) {

        final boolean powermode = preferences.getBoolean("isPoertMode", false);

        if (!powermode) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(context.getResources().getString(R.string.TurnOffAlert));
            builder.setPositiveButton(context.getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preferences.edit().putBoolean("isPoertMode", !powermode).commit();
                    power.setColorFilter(!powermode ? 0xfffa8072 : 0xffFFFFFF, PorterDuff.Mode.SRC_ATOP);
//                    power.setImageDrawable(mp);
                    powerView.invalidate();
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.powerChange, !powermode);
                    Utilities.restartApp();
                }
            });

            builder.setNegativeButton(context.getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.show();
        } else {

            preferences.edit().putBoolean("isPoertMode", !powermode).commit();
            power.setColorFilter(!powermode ? 0xfffa8072 : 0xffFFFFFF, PorterDuff.Mode.SRC_ATOP);
//            power.setImageDrawable(mp);
            ///0xff10b49c
            powerView.invalidate();
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.powerChange, !powermode);
            Utilities.restartApp();
        }
    }

    @Override
    public void onFinishTemporaryDetach() {
        super.onFinishTemporaryDetach();
    }




}
