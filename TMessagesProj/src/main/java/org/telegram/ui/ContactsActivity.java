/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.SecretChatHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.support.widget.LinearLayoutManager;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tpro1.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Adapters.ContactsAdapter;
import org.telegram.ui.Adapters.SearchAdapter;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.LetterSectionCell;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import sections.sqlite.catDBAdapter;
import sections.sqlite.categoryDBAdapter;
import sections.ui.DeletSelectedContacts;
import sections.ui.ShortcutActivity;
import sections.ui.categoryManagement;
import sections.ui.objects.Favourite;
import sections.ui.objects.category;
import sections.ui.objects.chatobject;

public class ContactsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ContactsAdapter listViewAdapter;
    private EmptyTextProgressView emptyView;
    private RecyclerListView listView;
    private SearchAdapter searchListViewAdapter;
    private ActionBarMenuItem addItem;

    private boolean searchWas;
    private boolean searching;
    private boolean onlyUsers;
    private boolean needPhonebook;
    private boolean destroyAfterSelect;
    private boolean returnAsResult;
    private boolean createSecretChat;
    private boolean creatingChat;
    private boolean allowBots = true;
    private boolean needForwardCount = true;
    private boolean needFinishFragment = true;
    private boolean addingToChannel;
    private int chat_id;
    private String selectAlertString = null;
    private HashMap<Integer, TLRPC.User> ignoreUsers;
    private boolean allowUsernameSearch = true;
    private ContactsActivityDelegate delegate;

    private AlertDialog permissionDialog;

    private boolean checkPermission = true;

    private final static int search_button = 0;
    private final static int add_button = 1;

    /// *****
    private boolean onlineUsers = false;
    private final int MenuItem_REFRESH = 5;
    private final int MENU_DELETE = 6;
    private Context context;

    public interface ContactsActivityDelegate {
        void didSelectContact(TLRPC.User user, String param, ContactsActivity activity);
    }

    public ContactsActivity(Bundle args) {
        super(args);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.encryptedChatCreated);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        if (arguments != null) {
            onlineUsers = getArguments().getBoolean("onlineUsers", false);
            onlyUsers = getArguments().getBoolean("onlyUsers", false);
            destroyAfterSelect = arguments.getBoolean("destroyAfterSelect", false);
            returnAsResult = arguments.getBoolean("returnAsResult", false);
            createSecretChat = arguments.getBoolean("createSecretChat", false);
            selectAlertString = arguments.getString("selectAlertString");
            allowUsernameSearch = arguments.getBoolean("allowUsernameSearch", true);
            needForwardCount = arguments.getBoolean("needForwardCount", true);
            allowBots = arguments.getBoolean("allowBots", true);
            addingToChannel = arguments.getBoolean("addingToChannel", false);
            needFinishFragment = arguments.getBoolean("needFinishFragment", true);
            chat_id = arguments.getInt("chat_id", 0);
        } else {
            needPhonebook = true;
        }
        if (onlineUsers) {
            createSecretChat = false;
        }
        ContactsController.getInstance().checkInviteText();

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.encryptedChatCreated);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        delegate = null;
    }

    @Override
    public View createView(Context context) {

        searching = false;
        searchWas = false;

        this.context = context;
        ContactsController.getInstance().initOnlineUsersSectionsDict();


        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        if (destroyAfterSelect) {
            if (returnAsResult) {
                actionBar.setTitle(LocaleController.getString("SelectContact", R.string.SelectContact));
            } else {
                if (createSecretChat) {
                    actionBar.setTitle(LocaleController.getString("NewSecretChat", R.string.NewSecretChat));
                } else {
                    actionBar.setTitle(LocaleController.getString("NewMessageTitle", R.string.NewMessageTitle));
                }
            }
        } else {
            actionBar.setTitle(LocaleController.getString("Contacts", R.string.Contacts));
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == add_button) {
                    presentFragment(new NewContactActivity());
                } else if (id == MENU_DELETE) {
                    presentFragment(new DeletSelectedContacts());
                } else if (id == MenuItem_REFRESH) {
                    reloadOnlineList();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem item = menu.addItem(search_button, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                searching = true;
                if (addItem != null) {
                    addItem.setVisibility(View.GONE);
                }
            }

            @Override
            public void onSearchCollapse() {
                if (addItem != null) {
                    addItem.setVisibility(View.VISIBLE);
                }
                searchListViewAdapter.searchDialogs(null);
                searching = false;
                searchWas = false;
                listView.setAdapter(listViewAdapter);
                listViewAdapter.notifyDataSetChanged();
                listView.setFastScrollVisible(true);
                listView.setVerticalScrollBarEnabled(false);
                emptyView.setText(LocaleController.getString("NoContacts", R.string.NoContacts));
            }

            @Override
            public void onTextChanged(EditText editText) {
                if (searchListViewAdapter == null) {
                    return;
                }
                String text = editText.getText().toString();
                if (text.length() != 0) {
                    searchWas = true;
                    if (listView != null) {
                        listView.setAdapter(searchListViewAdapter);
                        searchListViewAdapter.notifyDataSetChanged();
                        listView.setFastScrollVisible(false);
                        listView.setVerticalScrollBarEnabled(true);
                    }
                    if (emptyView != null) {
                        emptyView.setText(LocaleController.getString("NoResult", R.string.NoResult));
                    }
                }
                searchListViewAdapter.searchDialogs(text);
            }
        });
        item.getSearchField().setHint(LocaleController.getString("Search", R.string.Search));
        if (!createSecretChat && !returnAsResult) {
            addItem = menu.addItem(add_button, R.drawable.add);
        }

        if (onlineUsers) {
            Drawable refresh = getParentActivity().getResources().getDrawable(R.drawable.ic_refresh);
            ActionBarMenuItem item2 = new ActionBarMenuItem(context, menu, 0, 0xffffffff);
            menu.addItem(MenuItem_REFRESH, refresh).setIsSearchField(false);


        } else {
            menu.addItemWithWidth(MENU_DELETE, R.drawable.ic_delete_contacts, AndroidUtilities.dp(56));
        }


        searchListViewAdapter = new SearchAdapter(context, ignoreUsers, allowUsernameSearch, false, false, allowBots, 0);
//        listViewAdapter = new ContactsAdapter(context, onlyUsers ? 1 : 0, needPhonebook, ignoreUsers, chat_id != 0);

        if (onlineUsers) {
            listViewAdapter = new ContactsAdapter(context, onlyUsers ? 1 : 0, needPhonebook, ignoreUsers, chat_id != 0, true);
        } else {
            listViewAdapter = new ContactsAdapter(context, onlyUsers ? 1 : 0, needPhonebook, ignoreUsers, chat_id != 0, false);
        }

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        emptyView = new EmptyTextProgressView(context);
        emptyView.setShowAtCenter(true);
        emptyView.showTextView();
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView = new RecyclerListView(context);
        listView.setEmptyView(emptyView);
        listView.setSectionsType(1);
        listView.setVerticalScrollBarEnabled(false);
        listView.setFastScrollEnabled();
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listViewAdapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (searching && searchWas) {
                    TLRPC.User user = (TLRPC.User) searchListViewAdapter.getItem(position);
                    if (user == null) {
                        return;
                    }
                    if (searchListViewAdapter.isGlobalSearch(position)) {
                        ArrayList<TLRPC.User> users = new ArrayList<>();
                        users.add(user);
                        MessagesController.getInstance().putUsers(users, false);
                        MessagesStorage.getInstance().putUsersAndChats(users, null, false, true);
                    }
                    if (returnAsResult) {
                        if (ignoreUsers != null && ignoreUsers.containsKey(user.id)) {
                            return;
                        }
                        didSelectResult(user, true, null);
                    } else {
                        if (createSecretChat) {
                            if (user.id == UserConfig.getClientUserId()) {
                                return;
                            }
                            creatingChat = true;
                            SecretChatHelper.getInstance().startSecretChat(getParentActivity(), user);
                        } else {
                            Bundle args = new Bundle();
                            args.putInt("user_id", user.id);
                            if (MessagesController.checkCanOpenChat(args, ContactsActivity.this)) {
                                presentFragment(new ChatActivity(args), true);
                            }
                        }
                    }
                } else {
                    int section = listViewAdapter.getSectionForPosition(position);
                    int row = listViewAdapter.getPositionInSectionForPosition(position);
                    if (row < 0 || section < 0) {
                        return;
                    }
                    if ((!onlyUsers || chat_id != 0) && section == 0) {
                        if (needPhonebook) {
                            if (row == 0) {
                                presentFragment(new InviteContactsActivity());
                            }
                        } else if (chat_id != 0) {
                            if (row == 0) {
                                presentFragment(new GroupInviteActivity(chat_id));
                            }
                        } else {
                            if (row == 0) {
                                if (!MessagesController.isFeatureEnabled("chat_create", ContactsActivity.this)) {
                                    return;
                                }
                                presentFragment(new GroupCreateActivity(), false);
                            } else if (row == 1) {
                                Bundle args = new Bundle();
                                args.putBoolean("onlyUsers", true);
                                args.putBoolean("destroyAfterSelect", true);
                                args.putBoolean("createSecretChat", true);
                                args.putBoolean("allowBots", false);
                                presentFragment(new ContactsActivity(args), false);
                            } else if (row == 2) {
                                if (!MessagesController.isFeatureEnabled("broadcast_create", ContactsActivity.this)) {
                                    return;
                                }
                                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                                if (!BuildVars.DEBUG_VERSION && preferences.getBoolean("channel_intro", false)) {
                                    Bundle args = new Bundle();
                                    args.putInt("step", 0);
                                    presentFragment(new ChannelCreateActivity(args));
                                } else {
                                    presentFragment(new ChannelIntroActivity());
                                    preferences.edit().putBoolean("channel_intro", true).commit();
                                }
                            }
                        }
                    } else {
                        Object item = listViewAdapter.getItem(section, row);

                        if (item instanceof TLRPC.User) {
                            TLRPC.User user = (TLRPC.User) item;
                            if (returnAsResult) {
                                if (ignoreUsers != null && ignoreUsers.containsKey(user.id)) {
                                    return;
                                }
                                didSelectResult(user, true, null);
                            } else {
                                if (createSecretChat) {
                                    creatingChat = true;
                                    SecretChatHelper.getInstance().startSecretChat(getParentActivity(), user);
                                } else {
                                    Bundle args = new Bundle();
                                    args.putInt("user_id", user.id);
                                    if (MessagesController.checkCanOpenChat(args, ContactsActivity.this)) {
                                        presentFragment(new ChatActivity(args), true);
                                    }
                                }
                            }
                        } else if (item instanceof ContactsController.Contact) {
                            ContactsController.Contact contact = (ContactsController.Contact) item;
                            String usePhone = null;
                            if (!contact.phones.isEmpty()) {
                                usePhone = contact.phones.get(0);
                            }
                            if (usePhone == null || getParentActivity() == null) {
                                return;
                            }
                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                            builder.setMessage(LocaleController.getString("InviteUser", R.string.InviteUser));
                            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                            final String arg1 = usePhone;
                            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", arg1, null));
                                        intent.putExtra("sms_body", ContactsController.getInstance().getInviteText(1));
                                        getParentActivity().startActivityForResult(intent, 500);
                                    } catch (Exception e) {
                                        FileLog.e(e);
                                    }
                                }
                            });
                            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                            showDialog(builder.create());
                        }
                    }
                }
            }
        });


        listView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListener() {
            @Override
            public boolean onItemClick(View view, int position) {

                if (searching && searchWas) {
                    TLRPC.User user = (TLRPC.User) searchListViewAdapter.getItem(position);
                    if (user == null) {
                        return true;
                    }
                    if (searchListViewAdapter.isGlobalSearch(position)) {
                        ArrayList<TLRPC.User> users = new ArrayList<>();
                        users.add(user);
                        MessagesController.getInstance().putUsers(users, false);
                        MessagesStorage.getInstance().putUsersAndChats(users, null, false, true);
                    }
                    if (returnAsResult) {
                        if (ignoreUsers != null && ignoreUsers.containsKey(user.id)) {
                            return true;
                        }
                        didSelectResult(user, true, null);
                    } else {
                        showAlert(user);
                    }
                } else {
                    int section = listViewAdapter.getSectionForPosition(position);
                    int row = listViewAdapter.getPositionInSectionForPosition(position);
                    if (row < 0 || section < 0) {
                        return true;
                    }
                    if ((!onlyUsers || chat_id != 0) && section == 0) {
                        if (needPhonebook) {
                            return true;
                        } else if (chat_id != 0) {
                            return true;
                        } else {
                            if (row == 0) {
                                if (!MessagesController.isFeatureEnabled("chat_create", ContactsActivity.this)) {
                                    return true;
                                }
                                return true;
                            } else if (row == 1) {
                                return true;
                            } else if (row == 2) {
                                if (!MessagesController.isFeatureEnabled("broadcast_create", ContactsActivity.this)) {
                                    return true;
                                }
                                return true;
                            }
                        }
                    } else {
                        Object item = listViewAdapter.getItem(section, row);

                        if (item instanceof TLRPC.User) {
                            TLRPC.User user = (TLRPC.User) item;
                            if (returnAsResult) {
                                if (ignoreUsers != null && ignoreUsers.containsKey(user.id)) {
                                    return true;
                                }
                                didSelectResult(user, true, null);
                            } else {

                                showAlert(user);

                            }
                        } else if (item instanceof ContactsController.Contact) {
                            return true;
                        }
                    }
                }
                return false;
            }
        });


        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && searching && searchWas) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        return fragmentView;
    }

    private void didSelectResult(final TLRPC.User user, boolean useAlert, String param) {
        if (useAlert && selectAlertString != null) {
            if (getParentActivity() == null) {
                return;
            }
            if (user.bot && user.bot_nochats && !addingToChannel) {
                try {
                    Toast.makeText(getParentActivity(), LocaleController.getString("BotCantJoinGroups", R.string.BotCantJoinGroups), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            String message = LocaleController.formatStringSimple(selectAlertString, UserObject.getUserName(user));
            EditText editText = null;
            if (!user.bot && needForwardCount) {
                message = String.format("%s\n\n%s", message, LocaleController.getString("AddToTheGroupForwardCount", R.string.AddToTheGroupForwardCount));
                editText = new EditText(getParentActivity());
                editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                editText.setText("50");
                editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                editText.setGravity(Gravity.CENTER);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                editText.setBackgroundDrawable(Theme.createEditTextDrawable(getParentActivity(), true));
                final EditText editTextFinal = editText;
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        try {
                            String str = s.toString();
                            if (str.length() != 0) {
                                int value = Utilities.parseInt(str);
                                if (value < 0) {
                                    editTextFinal.setText("0");
                                    editTextFinal.setSelection(editTextFinal.length());
                                } else if (value > 300) {
                                    editTextFinal.setText("300");
                                    editTextFinal.setSelection(editTextFinal.length());
                                } else if (!str.equals("" + value)) {
                                    editTextFinal.setText("" + value);
                                    editTextFinal.setSelection(editTextFinal.length());
                                }
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }

                });
                builder.setView(editText);
            }
            builder.setMessage(message);
            final EditText finalEditText = editText;
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    didSelectResult(user, false, finalEditText != null ? finalEditText.getText().toString() : "0");
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
            if (editText != null) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) editText.getLayoutParams();
                if (layoutParams != null) {
                    if (layoutParams instanceof FrameLayout.LayoutParams) {
                        ((FrameLayout.LayoutParams) layoutParams).gravity = Gravity.CENTER_HORIZONTAL;
                    }
                    layoutParams.rightMargin = layoutParams.leftMargin = AndroidUtilities.dp(24);
                    layoutParams.height = AndroidUtilities.dp(36);
                    editText.setLayoutParams(layoutParams);
                }
                editText.setSelection(editText.getText().length());
            }
        } else {
            if (delegate != null) {
                delegate.didSelectContact(user, param, this);
                delegate = null;
            }
            if (needFinishFragment) {
                finishFragment();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listViewAdapter != null) {
            listViewAdapter.notifyDataSetChanged();
        }
        if (checkPermission && Build.VERSION.SDK_INT >= 23) {
            Activity activity = getParentActivity();
            if (activity != null) {
                checkPermission = false;
                if (activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    if (activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("PermissionContacts", R.string.PermissionContacts));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                        showDialog(permissionDialog = builder.create());
                    } else {
                        askForPermissons();
                    }
                }
            }
        }
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        super.onDialogDismiss(dialog);
        if (permissionDialog != null && dialog == permissionDialog && getParentActivity() != null) {
            askForPermissons();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askForPermissons() {
        Activity activity = getParentActivity();
        if (activity == null || activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ArrayList<String> permissons = new ArrayList<>();
        permissons.add(Manifest.permission.READ_CONTACTS);
        permissons.add(Manifest.permission.WRITE_CONTACTS);
        permissons.add(Manifest.permission.GET_ACCOUNTS);
        String[] items = permissons.toArray(new String[permissons.size()]);
        activity.requestPermissions(items, 1);
    }

    @Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            for (int a = 0; a < permissions.length; a++) {
                if (grantResults.length <= a || grantResults[a] != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
                switch (permissions[a]) {
                    case Manifest.permission.READ_CONTACTS:
                        ContactsController.getInstance().forceImportContacts();
                        break;
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (actionBar != null) {
            actionBar.closeSearchField();
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.contactsDidLoaded) {
            if (listViewAdapter != null) {
                listViewAdapter.notifyDataSetChanged();
            }
        } else if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer)args[0];
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                updateVisibleRows(mask);
            }
        } else if (id == NotificationCenter.encryptedChatCreated) {
            if (createSecretChat && creatingChat) {
                TLRPC.EncryptedChat encryptedChat = (TLRPC.EncryptedChat)args[0];
                Bundle args2 = new Bundle();
                args2.putInt("enc_id", encryptedChat.id);
                NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                presentFragment(new ChatActivity(args2), true);
            }
        } else if (id == NotificationCenter.closeChats) {
            if (!creatingChat) {
                removeSelfFromStack();
            }
        }
    }

    private void updateVisibleRows(int mask) {
        if (listView != null) {
            int count = listView.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = listView.getChildAt(a);
                if (child instanceof UserCell) {
                    ((UserCell) child).update(mask);
                }
            }
        }
    }

    public void setDelegate(ContactsActivityDelegate delegate) {
        this.delegate = delegate;
    }

    public void setIgnoreUsers(HashMap<Integer, TLRPC.User> users) {
        ignoreUsers = users;
    }

    @Override
    public ThemeDescription[] getThemeDescriptions() {
        ThemeDescription.ThemeDescriptionDelegate сellDelegate = new ThemeDescription.ThemeDescriptionDelegate() {
            @Override
            public void didSetColor(int color) {
                int count = listView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View child = listView.getChildAt(a);
                    if (child instanceof UserCell) {
                        ((UserCell) child).update(0);
                    } else if (child instanceof ProfileSearchCell) {
                        ((ProfileSearchCell) child).update(0);
                    }
                }
            }
        };

        return new ThemeDescription[]{
                new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite),

                new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault),
                new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultSearch),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultSearchPlaceholder),

                new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector),

                new ThemeDescription(listView, ThemeDescription.FLAG_SECTIONS, new Class[]{LetterSectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4),

                new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider),

                new ThemeDescription(emptyView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder),

                new ThemeDescription(listView, ThemeDescription.FLAG_FASTSCROLL, null, null, null, null, Theme.key_fastScrollActive),
                new ThemeDescription(listView, ThemeDescription.FLAG_FASTSCROLL, null, null, null, null, Theme.key_fastScrollInactive),
                new ThemeDescription(listView, ThemeDescription.FLAG_FASTSCROLL, null, null, null, null, Theme.key_fastScrollText),

                new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"nameTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"statusColor"}, null, null, сellDelegate, Theme.key_windowBackgroundWhiteGrayText),
                new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"statusOnlineColor"}, null, null, сellDelegate, Theme.key_windowBackgroundWhiteBlueText),
                new ThemeDescription(listView, 0, new Class[]{UserCell.class}, null, new Drawable[]{Theme.avatar_photoDrawable, Theme.avatar_broadcastDrawable, Theme.avatar_savedDrawable}, null, Theme.key_avatar_text),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundRed),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundOrange),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundViolet),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundGreen),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundCyan),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundBlue),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundPink),

                new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayIcon),

                new ThemeDescription(listView, 0, new Class[]{GraySectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2),
                new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{GraySectionCell.class}, null, null, null, Theme.key_graySection),

                new ThemeDescription(listView, 0, new Class[]{ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_groupDrawable, Theme.dialogs_broadcastDrawable, Theme.dialogs_botDrawable}, null, Theme.key_chats_nameIcon),
                new ThemeDescription(listView, 0, new Class[]{ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_verifiedCheckDrawable}, null, Theme.key_chats_verifiedCheck),
                new ThemeDescription(listView, 0, new Class[]{ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_verifiedDrawable}, null, Theme.key_chats_verifiedBackground),
                new ThemeDescription(listView, 0, new Class[]{ProfileSearchCell.class}, Theme.dialogs_offlinePaint, null, null, Theme.key_windowBackgroundWhiteGrayText3),
                new ThemeDescription(listView, 0, new Class[]{ProfileSearchCell.class}, Theme.dialogs_onlinePaint, null, null, Theme.key_windowBackgroundWhiteBlueText3),
                new ThemeDescription(listView, 0, new Class[]{ProfileSearchCell.class}, Theme.dialogs_namePaint, null, null, Theme.key_chats_name),
        };
    }

    private void reloadOnlineList() {
        ContactsController.getInstance().initOnlineUsersSectionsDict();
        listViewAdapter = new ContactsAdapter(context, onlyUsers ? 1 : 0, needPhonebook, ignoreUsers, chat_id != 0, true);

        this.listView.setAdapter(this.listViewAdapter);
    }



    private void showAlert(final TLRPC.User user) {

        int muted = MessagesController.getInstance().isDialogMuted(user.id) ? R.drawable.mute_fixed : 0;
        CharSequence muteText = muted != 0 ? LocaleController.getString("UnmuteNotifications", R.string.UnmuteNotifications) : LocaleController.getString("MuteNotifications", R.string.MuteNotifications);

        final boolean isFav = Favourite.isFavorite((long) user.id);
        CharSequence favoriteText = isFav ? LocaleController.getString("DeleteFromFavorites", R.string.DeleteFromFavorites) : LocaleController.getString("AddToFavorites", R.string.AddToFavorites);


        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        final TLRPC.User finalUser = user;
        builder.setItems(new CharSequence[]{
                muteText,
                favoriteText,
                LocaleController.getString("AddShortcut", R.string.AddShortcut), // 4
                LocaleController.getString("addToCategory", R.string.addToCategory), // 6
                LocaleController.getString("ClearHistory", R.string.ClearHistory),
                LocaleController.getString("Delete", R.string.Delete)
        }, new int[]{
//                dialog.pinned ? R.drawable.chats_unpin : R.drawable.chats_pin,
                muted != 0 ? R.drawable.dialog_unmute : R.drawable.dialog_mute,
                isFav ? R.drawable.dialog_remove_fav : R.drawable.dialog_add_fav,
                R.drawable.dialog_shortcut,
                R.drawable.ic_menu_category,
                R.drawable.chats_clear,
                R.drawable.chats_delete
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {
                if (which == 0) { // mute / unmute
                    boolean muted = MessagesController.getInstance().isDialogMuted(user.id);
                    if (!muted) {
                        long flags;
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putInt("notify2_" + user.id, 2);
                        flags = 1;
                        MessagesStorage.getInstance().setDialogFlags(user.id, flags);
                        editor.commit();
                        TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(user.id);
                        if (dialg != null) {
                            dialg.notify_settings = new TLRPC.TL_peerNotifySettings();
                        }
                        NotificationsController.updateServerNotificationsSettings(user.id);
                    } else {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putInt("notify2_" + user.id, 0);
                        MessagesStorage.getInstance().setDialogFlags(user.id, 0);
                        editor.commit();
                        TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(user.id);
                        if (dialg != null) {
                            dialg.notify_settings = new TLRPC.TL_peerNotifySettings();
                        }
                        NotificationsController.updateServerNotificationsSettings(user.id);
                    }
                } else if (which == 1) {
                    TLRPC.TL_dialog dialg = MessagesController.getInstance().dialogs_dict.get(user.id);
                    if (isFav) {
                        Favourite.deleteFavorite((long) user.id);
                        MessagesController.getInstance().dialogsFavs.remove(dialg);
                    } else {
                        Favourite.addFavorite((long) user.id);
                        MessagesController.getInstance().dialogsFavs.add(dialg);
                    }

                } else if (which == 2) {
                    addShortcut(user.id);
                } else if (which == 3) {
                    addtoCategory(user.id, context);
                }  else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    if (which == 5)
                        builder.setMessage(LocaleController.getString("AreYouSureDeleteContact", R.string.AreYouSureDeleteContact));
                    else
                        builder.setMessage(LocaleController.getString("AreYouSureClearHistory", R.string.AreYouSureClearHistory));

                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            if (which == 5) {
                                if (user != null || getParentActivity() != null) {
                                    ArrayList<TLRPC.User> arrayList2 = new ArrayList<>();
                                    arrayList2.add(user);
                                    ContactsController.getInstance().deleteContact(arrayList2);
                                }

                                listView.invalidate();
                            } else {
                                MessagesController.getInstance().deleteDialog(user.id, 1);
                            }

                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                }
            }
        });
        showDialog(builder.create());

    }


    private void addShortcut(int selectedDialog) {

        Intent shortcutIntent = new Intent(ApplicationLoader.applicationContext, ShortcutActivity.class);
        shortcutIntent.setAction("com.tmessages.openchat" + Math.random() + Integer.MAX_VALUE);
        TLRPC.User user = MessagesController.getInstance().getUser((int) selectedDialog);
        TLRPC.EncryptedChat encryptedChat = null;

        AvatarDrawable avatarDrawable = new AvatarDrawable();


        user = MessagesController.getInstance().getUser(selectedDialog);
        shortcutIntent.putExtra("userId", selectedDialog);
        avatarDrawable.setInfo(user);


        final String name = user != null && encryptedChat == null ? UserObject.getUserName(user) : encryptedChat != null ? new String(Character.toChars(0x1F512)) + UserObject.getUserName(user) : null;
        //Log.e("DialogsActivity", "addShortcut " + user.id);
        if (name == null) {
            return;
        }
        //Log.e("Plus", "addShortcut " + name + " dialog_id " + dialog_id);

        TLRPC.FileLocation photoPath = null;

        if (user != null) {
            if (user.photo != null && user.photo.photo_small != null && user.photo.photo_small.volume_id != 0 && user.photo.photo_small.local_id != 0) {
                photoPath = user.photo.photo_small;
            }
        }
        BitmapDrawable img = null;

        if (photoPath != null) {
            img = ImageLoader.getInstance().getImageFromMemory(photoPath, null, "50_50");
        }

        String action = "com.android.launcher.action.INSTALL_SHORTCUT";
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);

        if (img != null) {
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, getRoundBitmap(img.getBitmap()));
        } else {
            int w = AndroidUtilities.dp(40);
            int h = AndroidUtilities.dp(40);
            Bitmap mutableBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mutableBitmap);
            avatarDrawable.setBounds(0, 0, w, h);
            avatarDrawable.draw(canvas);
            //if(mutableBitmap != null){
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, getRoundBitmap(mutableBitmap));
            //} else{
            //addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(ApplicationLoader.applicationContext, R.drawable.intro1));
            //}
        }

        addIntent.putExtra("duplicate", false);
        addIntent.setAction(action);
        //addIntent.setPackage(ApplicationLoader.applicationContext.getPackageName());
        boolean error = false;
        if (ApplicationLoader.applicationContext.getPackageManager().queryBroadcastReceivers(new Intent(action), 0).size() > 0) {
            ApplicationLoader.applicationContext.sendBroadcast(addIntent);
        } else {
            error = true;
        }
        final String msg = error ? LocaleController.formatString("ShortcutError", R.string.ShortcutError, name) : LocaleController.formatString("ShortcutAdded", R.string.ShortcutAdded, name);
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (getParentActivity() != null) {
                    Toast toast = Toast.makeText(getParentActivity(), msg, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    private Bitmap getRoundBitmap(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int radius = Math.min(h / 2, w / 2);
        Bitmap output = Bitmap.createBitmap(w + 8, h + 8, Bitmap.Config.ARGB_8888);
        Paint p = new Paint();
        p.setAntiAlias(true);
        Canvas c = new Canvas(output);
        c.drawARGB(0, 0, 0, 0);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle((w / 2) + 4, (h / 2) + 4, radius, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        c.drawBitmap(bitmap, 4, 4, p);
        return output;
    }


    public void addtoCategory(final long selectedDialog, final Context mContext) {


        final CharSequence[] items;

        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(R.string.addToCategory));
        builder.setCancelable(false);
        builder.setIcon(R.drawable.ic_launcher);

        categoryDBAdapter db = new categoryDBAdapter(mContext);
        db.open();
        List<category> categories = new ArrayList<>();
        categories = db.getAllItms();
        db.close();


        if (categories.size() > 0) {
            items = new CharSequence[categories.size()];
            for (int i = 0; i < categories.size(); i++) {
                items[i] = categories.get(i).getName();
            }


            final List<category> finalCategories = categories;
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    catDBAdapter cdb = new catDBAdapter(mContext);
                    cdb.open();
                    chatobject chat = new chatobject();
                    chat.setDialog_id((int) selectedDialog);
                    chat.setCatCode(finalCategories.get(item).getId());
                    cdb.insert(chat);
                    cdb.close();


                }
            });

        } else {
            builder.setMessage(mContext.getResources().getString(R.string.noCategory));
            builder.setPositiveButton(mContext.getResources().getString(R.string.newCategory), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    presentFragment(new categoryManagement());
                }
            });

        }


        builder.setNegativeButton(mContext.getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dismis
            }
        });

        android.app.AlertDialog alert = builder.create();
        alert.show();

    }



}
