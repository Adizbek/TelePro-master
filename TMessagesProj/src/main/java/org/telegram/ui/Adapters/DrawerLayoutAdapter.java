/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.ui.Adapters;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.tpro1.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.DrawerActionCell;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.TextInfoCell;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Locale;

public class DrawerLayoutAdapter extends RecyclerListView.SelectionAdapter {

    private Context mContext;
    private ArrayList<Item> items = new ArrayList<>(16);

    private int s1 = 5;
    private int s3 = 11;
    private int last = 17;
    private int end = 18;


    public DrawerLayoutAdapter(Context context) {
        mContext = context;
        Theme.createDialogsResources(context);
        resetItems();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public void notifyDataSetChanged() {
        resetItems();
        super.notifyDataSetChanged();
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        return holder.getItemViewType() == 3;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                view = new DrawerProfileCell(mContext);
                break;
            case 1:
            default:
                view = new EmptyCell(mContext, AndroidUtilities.dp(8));
                break;
            case 2:
                view = new DividerCell(mContext);
                break;
            case 3:
                view = new DrawerActionCell(mContext);
                break;
            case 4:
                view = new TextInfoCell(mContext);
                try {
                    PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
                    int code = pInfo.versionCode / 10;
                    String abi = "";
                    try {
                        if (Build.CPU_ABI != null) {
                            abi = Build.CPU_ABI;
                        } else {
                            switch (pInfo.versionCode % 10) {
                                case 0:
                                    abi = "arm";
                                    break;
                                case 1:
                                    abi = "arm-v7a";
                                    break;
                                case 2:
                                    abi = "x86";
                                    break;
                                case 3:
                                    abi = "universal";
                                    break;
                            }
                        }

                    } catch (Exception e) {

                        abi = "arm-v7a";

                    }

                    ((TextInfoCell) view).setText(String.format(Locale.US, LocaleController.getString("TelegramForAndroid", R.string.TelegramForAndroid) + "\nv%s (%d) ", pInfo.versionName, code));
                    ((TextInfoCell) view).setTextColor(0xffa3a3a3);
                    ((TextInfoCell) view).setTextSize(13);
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
                break;
        }
        view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder == null)
            return;
        if (holder.itemView == null)
            return;
        switch (holder.getItemViewType()) {
            case 0:
                ((DrawerProfileCell) holder.itemView).setUser(MessagesController.getInstance().getUser(UserConfig.getClientUserId()));
                holder.itemView.setBackgroundColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue));
                break;
            case 3:
                items.get(position).bind((DrawerActionCell) holder.itemView);
                break;

        }
    }


    @Override
    public int getItemViewType(int i) {
        if (i == 0) {
            return 0;
        } else if (i == 1) {
            return 1;
        } else if ( i == s1  ||i == s3 || i == last) {
            return 2;
        } else if (i == end) {
            return 4;
        }


        return 3;
    }

    private void resetItems() {
        items.clear();
        if (!UserConfig.isClientActivated()) {
            return;
        }
        items.add(null); // profile
        items.add(null); // padding


        items.add(new Item(2, LocaleController.getString("NewGroup", R.string.NewGroup), R.drawable.menu_newgroup));
        items.add(new Item(3, LocaleController.getString("NewSecretChat", R.string.NewSecretChat), R.drawable.menu_secret));
        items.add(new Item(4, LocaleController.getString("NewChannel", R.string.NewChannel), R.drawable.menu_broadcast));
        items.add(null); // divider


        items.add(new Item(6, LocaleController.getString("Calls", R.string.Calls), R.drawable.menu_calls));
        items.add(new Item(7, LocaleController.getString("Contacts", R.string.Contacts), R.drawable.menu_contacts));
        items.add(new Item(24, LocaleController.getString("Store", R.string.Store), R.drawable.ic_menu_store));
        items.add(new Item(19, LocaleController.getString("Theming", R.string.Theming), R.drawable.menu_theming));
        items.add(new Item(23, LocaleController.getString("OfficialChannel", R.string.OfficialChannel), R.drawable.menu_broadcast));

        items.add(null); // divider
        items.add(new Item(20, LocaleController.getString("Settings", R.string.Settings), R.drawable.menu_settings));
        items.add(new Item(21, LocaleController.getString("PlusSettings", R.string.PlusSettings), R.drawable.menu_plus));
        items.add(new Item(15, LocaleController.getString("FileManager", R.string.FileManager), R.drawable.menu_file_manager));
        items.add(new Item(13, LocaleController.getString("downloadManager", R.string.downloadManager), R.drawable.ic_menu_download_manager));
        items.add(new Item(14, LocaleController.getString("CategoryManagement", R.string.CategoryManagement), R.drawable.ic_menu_category));


        items.add(null); // divider

        items.add(null); // version view
    }

    public int getId(int position) {
        if (position < 0 || position >= items.size()) {
            return -1;
        }
        Item item = items.get(position);
        return item != null ? item.id : -1;
    }

    private class Item {
        public int icon;
        public String text;
        public int id;

        public Item(int id, String text, int icon) {
            this.icon = icon;
            this.id = id;
            this.text = text;
        }

        public void bind(DrawerActionCell actionCell) {
            actionCell.setTextAndIcon(text, icon);
        }
    }


}
