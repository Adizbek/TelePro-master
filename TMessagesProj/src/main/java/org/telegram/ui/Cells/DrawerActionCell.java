/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.tpro1.R;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class DrawerActionCell extends FrameLayout {

    private TextView textView;

    public DrawerActionCell(Context context) {
        super(context);

        textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        textView.setCompoundDrawablePadding(AndroidUtilities.dp(34));
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 14, 0, 16, 0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(48), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        textView.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
    }

    public void setTextAndIcon(String text, int resId) {
        try {
            textView.setText(text);
            Drawable drawable;
            if (resId == -1 || resId == -2) {

                drawable = getResources().getDrawable(R.drawable.ic_menu_off);
                drawable.setColorFilter(resId == -1 ? 0xfffa8072 : 0xff008080, PorterDuff.Mode.SRC_ATOP);
                textView.setTag("POWER");
            } else{
                drawable = getResources().getDrawable(resId);
                if (drawable != null) {
                    drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_menuItemIcon), PorterDuff.Mode.SRC_ATOP));
                }
            }
            if (resId == -10 ){
                textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, drawable, null);
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            }else {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }


}
