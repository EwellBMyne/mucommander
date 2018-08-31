/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2012 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.ui.main.menu;

import com.mucommander.bonjour.BonjourMenu;
import com.mucommander.bonjour.BonjourService;
import com.mucommander.bookmark.Bookmark;
import com.mucommander.bookmark.BookmarkManager;
import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.file.impl.local.LocalFile;
import com.mucommander.commons.runtime.OsFamily;
import com.mucommander.conf.MuConfigurations;
import com.mucommander.conf.MuPreference;
import com.mucommander.conf.MuPreferences;
import com.mucommander.desktop.DesktopManager;
import com.mucommander.ui.action.ActionManager;
import com.mucommander.ui.action.ActionParameters;
import com.mucommander.ui.action.MuAction;
import com.mucommander.ui.action.impl.*;
import com.mucommander.ui.dialog.InformationDialog;
import com.mucommander.ui.dialog.pref.theme.ThemeEditorDialog;
import com.mucommander.ui.helper.MenuToolkit;
import com.mucommander.ui.helper.MnemonicHelper;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.WindowManager;
import com.mucommander.ui.main.table.Column;
import com.mucommander.ui.main.table.FileTable;
import com.mucommander.ui.theme.Theme;
import com.mucommander.ui.theme.ThemeManager;
import com.mucommander.ui.viewer.FileFrame;
import com.mucommander.utils.text.Translator;
import ru.trolsoft.ui.TCheckBoxMenuItem;
import ru.trolsoft.ui.TMenuSeparator;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;


/**
 * This class is the main menu bar. It takes care of displaying menu and menu items and triggering
 * the proper actions.
 *
 * <p><b>Implementation note</b>: for performance reasons, some menu items are created/enabled/disabled when corresponding menus
 * are selected, instead of monitoring the MainFrame's state and unnecessarily creating/enabling/disabling menu items
 * when they are not visible. However, this prevents keyboard shortcuts from being managed by the menu bar for those
 * dynamic items.
 *
 * @author Maxence Bernard
 */
public class MainMenuBar extends JMenuBar implements ActionListener, MenuListener {

    private MainFrame mainFrame;

    // View menu
    private JMenu viewMenu;
    private JMenu themesMenu;
    private JCheckBoxMenuItem[] cbSortByItems = new TCheckBoxMenuItem[Column.values().length];
    private JMenu tableModeMenu;
    private JCheckBoxMenuItem[] cbTableModeItems = new TCheckBoxMenuItem[3];
    private JMenu columnsMenu;
    private JCheckBoxMenuItem[] cbToggleColumnItems = new TCheckBoxMenuItem[Column.values().length];
    private JCheckBoxMenuItem cbToggleToggleAutoSizeItem;
    private JCheckBoxMenuItem cbToggleShowFoldersFirstItem;
    private JCheckBoxMenuItem cbToggleFoldersAlwaysAlphabeticalItem;
    private JCheckBoxMenuItem cbToggleShowHiddenFilesItem;
    private JCheckBoxMenuItem cbToggleTreeItem;
    private JCheckBoxMenuItem cbToggleSinglePanel;
    private OpenWithMenu openWithMenu;
    private OpenAsMenu openAsMenu;
    /* TODO branch private JCheckBoxMenuItem toggleBranchView; */


    // Go menu
    private JMenu goMenu;
    private int volumeOffset;

    // Bookmark menu
    private JMenu bookmarksMenu;
    private int bookmarksOffset;  // Index of the first bookmark menu item

    private JMenu ejectDrivesMenu;

    // Window menu
    private JMenu windowMenu;
    private int windowOffset; // Index of the first window menu item
    private JCheckBoxMenuItem splitHorizontallyItem;
    private JCheckBoxMenuItem splitVerticallyItem;

    /** Maps window menu items onto weakly-referenced frames */
    private WeakHashMap<JMenuItem, Frame> windowMenuFrames;


    private final static String RECALL_WINDOW_ACTION_IDS[] = {
        RecallWindow1Action.Descriptor.ACTION_ID,
        RecallWindow2Action.Descriptor.ACTION_ID,
        RecallWindow3Action.Descriptor.ACTION_ID,
        RecallWindow4Action.Descriptor.ACTION_ID,
        RecallWindow5Action.Descriptor.ACTION_ID,
        RecallWindow6Action.Descriptor.ACTION_ID,
        RecallWindow7Action.Descriptor.ACTION_ID,
        RecallWindow8Action.Descriptor.ACTION_ID,
        RecallWindow9Action.Descriptor.ACTION_ID,
        RecallWindow10Action.Descriptor.ACTION_ID
    };


    /**
     * Creates a new MenuBar for the given MainFrame.
     */
    public MainMenuBar(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        // Disable menu bar (NOT menu item) mnemonics under Mac OS X because of a bug: when screen menu bar is enabled
        // and a menu is triggered by a mnemonic, the menu pops up where it would appear with a regular menu bar
        // (i.e. with screen menu bar disabled).
        MnemonicHelper menuMnemonicHelper = OsFamily.MAC_OS_X.isCurrent() ? null : new MnemonicHelper();

        MnemonicHelper menuItemMnemonicHelper = new MnemonicHelper();
        MnemonicHelper menuItemMnemonicHelper2 = new MnemonicHelper();

        // File menu
        JMenu fileMenu = MenuToolkit.addMenu(Translator.get("file_menu"), menuMnemonicHelper, this);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(NewWindowAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(AddTabAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        fileMenu.add(new TMenuSeparator());
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(OpenAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(OpenNativelyAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        openWithMenu = new OpenWithMenu(mainFrame, null);
        fileMenu.add(openWithMenu);
        openAsMenu = new OpenAsMenu(mainFrame);
        fileMenu.add(openAsMenu);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(OpenInNewTabAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(OpenInOtherPanelAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(OpenInBothPanelsAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(RevealInDesktopAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        fileMenu.add(new TMenuSeparator());
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(PackAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(UnpackAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(EmailAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(BatchRenameAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(SplitFileAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(CombineFilesAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(CreateSymlinkAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        fileMenu.add(new TMenuSeparator());
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(ShowFilePropertiesAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(CalculateChecksumAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(ChangePermissionsAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(ChangeDateAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(ChangeReplicationAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        // Under Mac OS X, 'Preferences' already appears in the application (muCommander) menu, do not display it again
        if (!OsFamily.MAC_OS_X.isCurrent()) {
            fileMenu.add(new TMenuSeparator());
            MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(ShowPreferencesAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        }

        fileMenu.add(new TMenuSeparator());
        MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(CloseWindowAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        // Under Mac OS X, 'Quit' already appears in the application (muCommander) menu, do not display it again
        if (!OsFamily.MAC_OS_X.isCurrent()) {
            MenuToolkit.addMenuItem(fileMenu, ActionManager.getActionInstance(QuitAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        }
        add(fileMenu);

        // Mark menu
        menuItemMnemonicHelper.clear();
        JMenu markMenu = MenuToolkit.addMenu(Translator.get("mark_menu"), menuMnemonicHelper, this);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(MarkSelectedFileAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(MarkGroupAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(UnmarkGroupAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(MarkAllAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(UnmarkAllAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(MarkExtensionAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(MarkEmptyFilesAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(InvertSelectionAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        markMenu.add(new TMenuSeparator());
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(CopyFilesToClipboardAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(CopyFileNamesAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(CopyFileBaseNamesAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(CopyFilePathsAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(PasteClipboardFilesAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        markMenu.add(new TMenuSeparator());
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(CompareFoldersAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(markMenu, ActionManager.getActionInstance(CompareFolderFilesAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        add(markMenu);

        // View menu
        menuItemMnemonicHelper.clear();
        viewMenu = MenuToolkit.addMenu(Translator.get("view_menu"), menuMnemonicHelper, this);
        MenuToolkit.addMenuItem(viewMenu, ActionManager.getActionInstance(SwapFoldersAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(viewMenu, ActionManager.getActionInstance(SetSameFolderAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        viewMenu.add(new TMenuSeparator());

        tableModeMenu = MenuToolkit.addMenu(Translator.get("view_menu.table_mode"), null, this);

        cbTableModeItems[0] = MenuToolkit.addCheckBoxMenuItem(tableModeMenu, ActionManager.getActionInstance(ToggleTableViewModeFullAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        cbTableModeItems[1] = MenuToolkit.addCheckBoxMenuItem(tableModeMenu, ActionManager.getActionInstance(ToggleTableViewModeCompactAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        cbTableModeItems[2] = MenuToolkit.addCheckBoxMenuItem(tableModeMenu, ActionManager.getActionInstance(ToggleTableViewModeShortAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        ButtonGroup groupViewMode = new ButtonGroup();
        for (JCheckBoxMenuItem checkBoxMenuItem : cbTableModeItems) {
            groupViewMode.add(checkBoxMenuItem);
        }
//        tableModeMenu.addMenuListener(new MenuListener() {
//            @Override
//            public void menuSelected(MenuEvent e) {
//                int mode = mainFrame.getActiveTable().getViewMode().ordinal();
//                cbTableModeItems[mode].setSelected(true);
//            }
//
//            @Override
//            public void menuDeselected(MenuEvent e) { }
//
//            @Override
//            public void menuCanceled(MenuEvent e) { }
//        });
        tableModeMenu.add(new TMenuSeparator());
        MenuToolkit.addMenuItem(tableModeMenu, ActionManager.getActionInstance(TogglePanelPreviewModeAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        viewMenu.add(tableModeMenu);
        viewMenu.add(new TMenuSeparator());

        cbToggleShowFoldersFirstItem = MenuToolkit.addCheckBoxMenuItem(viewMenu, ActionManager.getActionInstance(ToggleShowFoldersFirstAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        cbToggleFoldersAlwaysAlphabeticalItem = MenuToolkit.addCheckBoxMenuItem(viewMenu, ActionManager.getActionInstance(ToggleFoldersAlwaysAlphabeticalAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        cbToggleShowHiddenFilesItem = MenuToolkit.addCheckBoxMenuItem(viewMenu, ActionManager.getActionInstance(ToggleHiddenFilesAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        cbToggleTreeItem = MenuToolkit.addCheckBoxMenuItem(viewMenu, ActionManager.getActionInstance(ToggleTreeAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        cbToggleSinglePanel = MenuToolkit.addCheckBoxMenuItem(viewMenu, ActionManager.getActionInstance(ToggleSinglePanelAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        /* TODO branch toggleBranchView = MenuToolkit.addCheckBoxMenuItem(viewMenu, ActionManager.getActionInstance(ToggleBranchViewAction.class, mainFrame), menuItemMnemonicHelper); */

        MenuToolkit.addMenuItem(viewMenu, ActionManager.getActionInstance(ShowFoldersSizeAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        viewMenu.add(new TMenuSeparator());
        ButtonGroup buttonGroup = new ButtonGroup();
        for (Column c : Column.values()) {
            buttonGroup.add(cbSortByItems[c.ordinal()] = MenuToolkit.addCheckBoxMenuItem(viewMenu, ActionManager.getActionInstance(c.getSortByColumnActionId(), mainFrame), menuItemMnemonicHelper));
        }

        MenuToolkit.addMenuItem(viewMenu, ActionManager.getActionInstance(ReverseSortOrderAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        viewMenu.add(new TMenuSeparator());

        // Toggle columns submenu
        columnsMenu = MenuToolkit.addMenu(Translator.get("view_menu.show_hide_columns"), null, this);
        menuItemMnemonicHelper2.clear();
        for (Column c : Column.values()) {
            if (c == Column.NAME) {
                continue;
            }

            cbToggleColumnItems[c.ordinal()] = MenuToolkit.addCheckBoxMenuItem(columnsMenu, ActionManager.getActionInstance(c.getToggleColumnActionId(), mainFrame), menuItemMnemonicHelper2);
        }
        viewMenu.add(columnsMenu);

        cbToggleToggleAutoSizeItem = MenuToolkit.addCheckBoxMenuItem(viewMenu, ActionManager.getActionInstance(ToggleAutoSizeAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        viewMenu.add(new TMenuSeparator());
        MenuToolkit.addMenuItem(viewMenu, ActionManager.getActionInstance(ToggleToolBarAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(viewMenu, ActionManager.getActionInstance(ToggleStatusBarAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(viewMenu, ActionManager.getActionInstance(ToggleCommandBarAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(viewMenu, ActionManager.getActionInstance(CustomizeCommandBarAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        add(viewMenu);

        // Go menu
        menuItemMnemonicHelper.clear();
        goMenu = MenuToolkit.addMenu(Translator.get("go_menu"), menuMnemonicHelper, this);

        MenuToolkit.addMenuItem(goMenu, ActionManager.getActionInstance(GoBackAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(goMenu, ActionManager.getActionInstance(GoForwardAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        goMenu.add(new TMenuSeparator());

        MenuToolkit.addMenuItem(goMenu, ActionManager.getActionInstance(GoToParentAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(goMenu, ActionManager.getActionInstance(GoToParentInOtherPanelAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(goMenu, ActionManager.getActionInstance(GoToParentInBothPanelsAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(goMenu, ActionManager.getActionInstance(GoToRootAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        goMenu.add(new TMenuSeparator());
        MenuToolkit.addMenuItem(goMenu, ActionManager.getActionInstance(ChangeLocationAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(goMenu, ActionManager.getActionInstance(ConnectToServerAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(goMenu, ActionManager.getActionInstance(ShowServerConnectionsAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        // Quick lists
        goMenu.add(new TMenuSeparator());
        JMenu quickListMenu = MenuToolkit.addMenu(Translator.get("quick_lists_menu"), menuMnemonicHelper, this);
        menuItemMnemonicHelper2.clear();
        MenuToolkit.addMenuItem(quickListMenu, ActionManager.getActionInstance(ShowParentFoldersQLAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        MenuToolkit.addMenuItem(quickListMenu, ActionManager.getActionInstance(ShowRecentLocationsQLAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        MenuToolkit.addMenuItem(quickListMenu, ActionManager.getActionInstance(ShowRecentExecutedFilesQLAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        MenuToolkit.addMenuItem(quickListMenu, ActionManager.getActionInstance(ShowBookmarksQLAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        MenuToolkit.addMenuItem(quickListMenu, ActionManager.getActionInstance(ShowRootFoldersQLAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        MenuToolkit.addMenuItem(quickListMenu, ActionManager.getActionInstance(ShowTabsQLAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        MenuToolkit.addMenuItem(quickListMenu, ActionManager.getActionInstance(ShowRecentViewedFilesQLAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        MenuToolkit.addMenuItem(quickListMenu, ActionManager.getActionInstance(ShowRecentEditedFilesQLAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        MenuToolkit.addMenuItem(quickListMenu, ActionManager.getActionInstance(ShowEditorBookmarksQLAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper2);
        goMenu.add(quickListMenu);

        // Add Bonjour services menu
        goMenu.add(new TMenuSeparator());
        BonjourMenu bonjourMenu = new BonjourMenu() {
            @Override
            public MuAction getMenuItemAction(BonjourService bs) {
                return new OpenLocationAction(MainMenuBar.this.mainFrame, new HashMap<>(), bs);
            }
        };
        char mnemonic = menuItemMnemonicHelper.getMnemonic(bonjourMenu.getName());
        if (mnemonic != 0) {
            bonjourMenu.setMnemonic(mnemonic);
        }
        bonjourMenu.setIcon(null);
        goMenu.add(bonjourMenu);

        // Volumes will be added when the menu is selected
        goMenu.add(new TMenuSeparator());
        volumeOffset = goMenu.getItemCount();

        add(goMenu);

        // Tools menu
        menuItemMnemonicHelper.clear();
        JMenu toolsMenu = MenuToolkit.addMenu(Translator.get("tools_menu"), menuMnemonicHelper, this);

        MenuToolkit.addMenuItem(toolsMenu, ActionManager.getActionInstance(FindFileAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(toolsMenu, ActionManager.getActionInstance(CalculatorAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(toolsMenu, ActionManager.getActionInstance(RunCommandAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        if (OsFamily.MAC_OS_X.isCurrent()) {
            ejectDrivesMenu = MenuToolkit.addMenu(Translator.get("eject_menu"), menuMnemonicHelper, this);
            toolsMenu.add(ejectDrivesMenu);

            MenuToolkit.addMenuItem(toolsMenu, ActionManager.getActionInstance(CompareFilesAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        }
        toolsMenu.add(new TMenuSeparator());
        MenuToolkit.addMenuItem(toolsMenu, ActionManager.getActionInstance(EditCommandsAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        add(toolsMenu);
        //toolsOffset = toolsMenu.getItemCount();

        // Bookmark menu, menu items will be added when the menu gets selected
        menuItemMnemonicHelper.clear();
        bookmarksMenu = MenuToolkit.addMenu(Translator.get("bookmarks_menu"), menuMnemonicHelper, this);
        //bookmarksMenu = MenuToolkit.addScrollableMenu(Translator.get("bookmarks_menu"), menuMnemonicHelper, this);

        MenuToolkit.addMenuItem(bookmarksMenu, ActionManager.getActionInstance(AddBookmarkAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(bookmarksMenu, ActionManager.getActionInstance(EditBookmarksAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(bookmarksMenu, ActionManager.getActionInstance(ExploreBookmarksAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        bookmarksMenu.add(new TMenuSeparator());
        MenuToolkit.addMenuItem(bookmarksMenu, ActionManager.getActionInstance(EditCredentialsAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        bookmarksMenu.add(new TMenuSeparator());

        // Save the first bookmark menu item's offset for later (bookmarks will be added when menu becomes visible)
        this.bookmarksOffset = bookmarksMenu.getItemCount();

        add(bookmarksMenu);

        
        // Window menu
        menuItemMnemonicHelper.clear();

        windowMenu = MenuToolkit.addMenu(Translator.get("window_menu"), menuMnemonicHelper, this);

        // If running Mac OS X, add 'Minimize' and 'Zoom' items
        if (OsFamily.MAC_OS_X.isCurrent()) {
            MenuToolkit.addMenuItem(windowMenu, ActionManager.getActionInstance(MinimizeWindowAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
            MenuToolkit.addMenuItem(windowMenu, ActionManager.getActionInstance(MaximizeWindowAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
            windowMenu.add(new TMenuSeparator());
        }

        MenuToolkit.addMenuItem(windowMenu, ActionManager.getActionInstance(SplitEquallyAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        buttonGroup = new ButtonGroup();
        buttonGroup.add(splitVerticallyItem = MenuToolkit.addCheckBoxMenuItem(windowMenu, ActionManager.getActionInstance(SplitVerticallyAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper));
        buttonGroup.add(splitHorizontallyItem = MenuToolkit.addCheckBoxMenuItem(windowMenu, ActionManager.getActionInstance(SplitHorizontallyAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper));

        windowMenu.add(new TMenuSeparator());
        themesMenu = MenuToolkit.addMenu(Translator.get("prefs_dialog.themes"), null, this);
        // Theme menu items will be added when the themes menu is selected
        windowMenu.add(themesMenu);

        windowMenu.add(new TMenuSeparator());
        MenuToolkit.addMenuItem(windowMenu, ActionManager.getActionInstance(RecallPreviousWindowAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(windowMenu, ActionManager.getActionInstance(RecallNextWindowAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(windowMenu, ActionManager.getActionInstance(BringAllToFrontAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        // All other window menu items will be added when the menu gets selected
        windowMenu.add(new TMenuSeparator());

        // Save the first window menu item's offset for later
        this.windowOffset = windowMenu.getItemCount();

        add(windowMenu);

        // Help menu
        menuItemMnemonicHelper.clear();
        JMenu helpMenu = MenuToolkit.addMenu(Translator.get("help_menu"), menuMnemonicHelper, null);

        MenuToolkit.addMenuItem(helpMenu, ActionManager.getActionInstance(GoToDocumentationAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(helpMenu, ActionManager.getActionInstance(ShowKeyboardShortcutsAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        MenuToolkit.addMenuItem(helpMenu, ActionManager.getActionInstance(ShowDebugConsoleAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

        // Links to website, only shows for OS/Window manager that can launch the default browser to open URLs
        if (DesktopManager.canBrowse()) {
            helpMenu.add(new TMenuSeparator());
            MenuToolkit.addMenuItem(helpMenu, ActionManager.getActionInstance(GoToWebsiteAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
            //MenuToolkit.addMenuItem(helpMenu, ActionManager.getActionInstance(GoToForumsAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
            MenuToolkit.addMenuItem(helpMenu, ActionManager.getActionInstance(ReportBugAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
            MenuToolkit.addMenuItem(helpMenu, ActionManager.getActionInstance(DonateAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);

            helpMenu.add(new TMenuSeparator());
            MenuToolkit.addMenuItem(helpMenu, ActionManager.getActionInstance(CheckForUpdatesAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        }
		
        // Under Mac OS X, 'About' already appears in the application (muCommander) menu, do not display it again
        if (!OsFamily.MAC_OS_X.isCurrent()) {
            helpMenu.add(new TMenuSeparator());
            MenuToolkit.addMenuItem(helpMenu, ActionManager.getActionInstance(ShowAboutAction.Descriptor.ACTION_ID, mainFrame), menuItemMnemonicHelper);
        }
		
        add(helpMenu);
    }
	

    ///////////////////////////
    // ActionListener method //
    ///////////////////////////

    public void actionPerformed(ActionEvent e) {
        // Discard action events while in 'no events mode'
        if (mainFrame.getNoEventsMode()) {
            return;
        }

        // Bring the frame corresponding to the clicked menu item to the front
        JMenuItem source = (JMenuItem) e.getSource();
        windowMenuFrames.get(source).toFront();
    }


    //////////////////////////
    // MenuListener methods //
    //////////////////////////

    public void menuSelected(MenuEvent e) {
        Object source = e.getSource();

        if (source == viewMenu) {
            updateViewMenu();
        } else if (source == columnsMenu) {
            updateShowHideColumnsMenu();
        } else if (source == goMenu) {
            updateGoMenu();
        } else if (source == ejectDrivesMenu) {
            updateEjectDriveMenu();
        } else if (source == bookmarksMenu) {
            updateBookmarksMenu();
        } else if (source == windowMenu) {
            updateWindowsMenu();
        } else if (source == themesMenu) {
            updateThemesMenu();
        } else if (source == tableModeMenu) {
            int mode = mainFrame.getActiveTable().getViewMode().ordinal();
            cbTableModeItems[mode].setSelected(true);
        } else if (source == openWithMenu) {
            openWithMenu.populate(mainFrame.getActiveTable().getSelectedFile());
        } else if (source == openAsMenu) {
            final AbstractFile selectedFile = mainFrame.getActiveTable().getSelectedFile();
            openAsMenu.setEnabled(selectedFile != null && !selectedFile.isDirectory());
        }
    }

    private void updateViewMenu() {
        FileTable activeTable = mainFrame.getActiveTable();

        // Select the 'sort by' criterion currently in use in the active table
        cbSortByItems[activeTable.getSortInfo().getCriterion().ordinal()].setSelected(true);

        Boolean foldersFirst = activeTable.getSortInfo().getFoldersFirst();
        cbToggleShowFoldersFirstItem.setSelected(foldersFirst);
        cbToggleFoldersAlwaysAlphabeticalItem.setEnabled(foldersFirst);
        cbToggleFoldersAlwaysAlphabeticalItem.setSelected(foldersFirst && activeTable.getSortInfo().getFoldersAlwaysAlphabetical());
        cbToggleShowHiddenFilesItem.setSelected(MuConfigurations.getPreferences().getVariable(MuPreference.SHOW_HIDDEN_FILES, MuPreferences.DEFAULT_SHOW_HIDDEN_FILES));
        cbToggleTreeItem.setSelected(activeTable.getFolderPanel().isTreeVisible());
        cbToggleToggleAutoSizeItem.setSelected(mainFrame.isAutoSizeColumnsEnabled());
        cbToggleSinglePanel.setSelected(mainFrame.isSinglePanel());
        // TODO branch toggleBranchView.setSelected(activeTable.getFolderPanel().isBranchView());
    }

    private void updateShowHideColumnsMenu() {
        // Update the selected and enabled state of each column menu item.
        FileTable activeTable = mainFrame.getActiveTable();
        for (Column c : Column.values()) {
            if (c == Column.NAME) {    // Name column doesn't have a menu item as it cannot be disabled
                continue;
            }

            JCheckBoxMenuItem item = cbToggleColumnItems[c.ordinal()];
            item.setSelected(activeTable.isColumnEnabled(c));
            item.setEnabled(activeTable.isColumnDisplayable(c));
            // Override the action's label to a shorter one
            item.setText(c.getLabel());
        }
    }

    private void updateGoMenu() {
        // Remove any previous volumes from the Go menu
        // as they might have changed since menu was last selected
        for (int i = goMenu.getItemCount(); i > volumeOffset; i--) {
            goMenu.remove(volumeOffset);
        }

        AbstractFile volumes[] = LocalFile.getVolumes();
        for (AbstractFile volume : volumes) {
            goMenu.add(new OpenLocationAction(mainFrame, new Hashtable<>(), volume));
        }
    }

    private void updateEjectDriveMenu() {
        // Remove any previous drives menu items from menu
        // as there might have changed since menu was last selected
        ejectDrivesMenu.removeAll();

        AbstractFile[] volumes = LocalFile.getVolumes();
        boolean empty = true;
        for (AbstractFile volume : volumes) {
            if (volume != null && !volume.isSymlink() && !volume.getPath().toLowerCase().startsWith("/users/")) {
                MenuToolkit.addMenuItem(ejectDrivesMenu, volume.getName(), null, null, event -> {
                    EjectDriveAction.eject(mainFrame, volume);
                    mainFrame.tryRefreshCurrentFolders();
                });
                empty = false;
            }
        }
        if (empty) {
            JMenuItem menuItem = new JMenuItem(Translator.get("eject.no_mounted_devices"));
            menuItem.setEnabled(false);
            ejectDrivesMenu.add(menuItem);
        }
    }

    private void updateBookmarksMenu() {
        // Remove any previous bookmarks menu items from menu
        // as bookmarks might have changed since menu was last selected
        for (int i = bookmarksMenu.getItemCount(); i > bookmarksOffset; i--) {
            bookmarksMenu.remove(bookmarksOffset);
        }

        // Add bookmarks menu items
        List<Bookmark> bookmarks = BookmarkManager.getBookmarks();
        if (!bookmarks.isEmpty()) {
            addBookmarksForGroup(bookmarksMenu, bookmarks, null);
        } else {
            // Show 'No bookmark' as a disabled menu item instead showing nothing
            JMenuItem noBookmarkItem = MenuToolkit.addMenuItem(bookmarksMenu, Translator.get("bookmarks_menu.no_bookmark"), null, null, null);
            noBookmarkItem.setEnabled(false);
        }
    }

    private void addBookmarksForGroup(JMenu menu, List<Bookmark> bookmarks, String parent) {
        for (Bookmark bookmark : bookmarks) {
            if ((bookmark.getParent() == null && parent == null) || (parent != null && parent.equals(bookmark.getParent()))) {
                if (bookmark.getLocation().isEmpty() && !bookmark.getName().equals(BookmarkManager.BOOKMARKS_SEPARATOR)) {
                    JMenu groupMenu = MenuToolkit.addMenu(bookmark.getName(), null, null);
                    menu.add(groupMenu);
                    addBookmarksForGroup(groupMenu, bookmarks, bookmark.getName());
                } else {
                    MenuToolkit.addMenuItem(menu, new OpenLocationAction(mainFrame, new HashMap<>(), bookmark), null);
                }
            }
        }
    }


    private void updateWindowsMenu() {
        // Select the split orientation currently in use
        if (mainFrame.getSplitPaneOrientation()) {
            splitVerticallyItem.setSelected(true);
        } else {
            splitHorizontallyItem.setSelected(true);
        }

        // Removing any window menu item previously added
        // Note: menu item cannot be removed by menuDeselected() as actionPerformed() will be called after
        // menu has been deselected.
        for (int i = windowMenu.getItemCount(); i > windowOffset; i--) {
            windowMenu.remove(windowOffset);
        }

        // This WeakHashMap maps menu items to frame instances. It has to be a weakly referenced hash map
        // and not a regular hash map, since it will not (and cannot) be emptied when the menu has been deselected
        // and we really do not want this hash map to prevent the frames to be GCed
        windowMenuFrames = new WeakHashMap<>();

        // create a menu item for each of the MainFrame instances, that displays the MainFrame's path
        // and a keyboard accelerator to recall the frame (for the first 10 frames only).
        List<MainFrame> mainFrames = WindowManager.getMainFrames();
        int nbFrames = mainFrames.size();
        for (int i = 0; i < nbFrames; i++) {
            MainFrame mainFrame = mainFrames.get(i);
            JCheckBoxMenuItem checkBoxMenuItem = new TCheckBoxMenuItem();

            // If frame number is less than 10, use the corresponding action class (accelerator will be displayed in the menu item)
            MuAction recallWindowAction;
            if (i < 10) {
                recallWindowAction = ActionManager.getActionInstance(RECALL_WINDOW_ACTION_IDS[i], this.mainFrame);
            } else {    // Else use the generic RecallWindowAction
                Map<String, Object> actionProps = new HashMap<>();
                // Specify the window number using the dedicated property
                actionProps.put(RecallWindowAction.WINDOW_NUMBER_PROPERTY_KEY, ""+(i+1));
                recallWindowAction = ActionManager.getActionInstance(new ActionParameters(RecallWindowAction.Descriptor.ACTION_ID, actionProps), this.mainFrame);
            }

            checkBoxMenuItem.setAction(recallWindowAction);

            // Replace the action's label and use the MainFrame's current folder path instead
            checkBoxMenuItem.setText((i+1)+" "+mainFrame.getActiveTable().getFolderPanel().getCurrentFolder().getAbsolutePath());

            // Use the action's label as a tooltip
            checkBoxMenuItem.setToolTipText(recallWindowAction.getLabel());

            // Check current MainFrame (the one this menu bar belongs to)
            checkBoxMenuItem.setSelected(mainFrame == this.mainFrame);

            windowMenu.add(checkBoxMenuItem);
        }

        // Add 'other' (non-MainFrame) windows : viewer and editor frames, no associated accelerator
        Frame frames[] = Frame.getFrames();
        nbFrames = frames.length;
        boolean firstFrame = true;
        for (int i = 0; i < nbFrames; i++) {
            Frame frame = frames[i];
            // Test if Frame is not hidden (disposed), Frame.getFrames() returns both active and disposed frames
            if (frame.isShowing() && (frame instanceof FileFrame)) {
                // Add a separator before the first non-MainFrame frame to mark a separation between MainFrames
                // and other frames
                if (firstFrame) {
                    windowMenu.add(new TMenuSeparator());
                    firstFrame = false;
                }
                // Use frame's window title
                JMenuItem menuItem = new JMenuItem(frame.getTitle());
                menuItem.addActionListener(this);
                windowMenu.add(menuItem);
                windowMenuFrames.put(menuItem, frame);
            }
        }
    }

    private void updateThemesMenu() {
        // Remove all previous theme items, create new ones for each available theme and select the current theme
        themesMenu.removeAll();
        ButtonGroup buttonGroup = new ButtonGroup();
        Iterator<Theme> themes = ThemeManager.availableThemes();
        themesMenu.add(new JMenuItem(new EditCurrentThemeAction()));
        themesMenu.add(new TMenuSeparator());
        while (themes.hasNext()) {
            Theme theme = themes.next();
            JCheckBoxMenuItem item = new TCheckBoxMenuItem(new ChangeCurrentThemeAction(theme));
            buttonGroup.add(item);
            if (ThemeManager.isCurrentTheme(theme)) {
                item.setSelected(true);
            }

            themesMenu.add(item);
        }
    }


    public void menuDeselected(MenuEvent e) {
    }
	 
    public void menuCanceled(MenuEvent e) {
    }


    /**
     * Action that changes the current theme to the specified in the constructor.
     */
    private class ChangeCurrentThemeAction extends AbstractAction {

        private Theme theme;

        ChangeCurrentThemeAction(Theme theme) {
            super(theme.getName());
            this.theme = theme;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            try {
                ThemeManager.setCurrentTheme(theme);
            } catch(IllegalArgumentException e) {
                InformationDialog.showErrorDialog(mainFrame, Translator.get("theme_could_not_be_loaded"));
            }
        }
    }

    /**
     * Actions that edits the current theme.
     */
    private class EditCurrentThemeAction extends AbstractAction {
        EditCurrentThemeAction() {
            super(Translator.get("prefs_dialog.edit_current_theme"));
        }

        public void actionPerformed(ActionEvent actionEvent) {
            new ThemeEditorDialog(mainFrame, ThemeManager.getCurrentTheme()).editTheme();
        }
    }


}
