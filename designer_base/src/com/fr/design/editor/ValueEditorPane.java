package com.fr.design.editor;

import com.fr.base.Formula;
import com.fr.design.DesignerEnvManager;
import com.fr.design.event.GlobalNameListener;
import com.fr.design.event.GlobalNameObserver;
import com.fr.design.event.UIObserver;
import com.fr.design.event.UIObserverListener;
import com.fr.design.gui.ibutton.UIButton;
import com.fr.design.gui.imenu.UIMenuItem;
import com.fr.design.layout.FRGUIPaneFactory;
import com.fr.design.mainframe.DesignerContext;
import com.fr.design.dialog.BasicPane;
import com.fr.design.editor.editor.ColumnNameEditor;
import com.fr.design.editor.editor.Editor;
import com.fr.design.editor.editor.TextEditor;
import com.fr.design.editor.editor.XMLANameEditor;
import com.fr.general.ComparatorUtils;
import com.fr.general.Inter;
import com.fr.stable.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ValueEditorPane extends BasicPane implements UIObserver, GlobalNameObserver {

    private Editor[] cards;

    private Editor currentEditor;

    private UIButton arrowButton;
    private JPopupMenu menu;
    private JPanel centerPane;

    private Object value;

    private GlobalNameListener globalNameListener = null;
    private UIObserverListener uiObserverListener = null;

    public ValueEditorPane(Editor[] cards) {
        this(cards, null, null);
    }

    public ValueEditorPane(Editor[] cards, String popupName, String textEditorValue) {
        initComponents(cards, popupName, textEditorValue, 200);
    }

    public ValueEditorPane(Editor[] cards, String popupName, String textEditorValue, int centerPaneWidth) {
        initComponents(cards, popupName, textEditorValue, centerPaneWidth);
    }

    private void initComponents(final Editor[] cards, String popupName, String textEditorValue, int centerPaneWidth) {

        this.cards = cards;

        // Frank：布局
        this.setLayout(new BorderLayout(2, 0));
        centerPane = FRGUIPaneFactory.createBorderLayout_S_Pane();
        arrowButton = new UIButton();
        arrowButton.set4ToolbarButton();
        setCurrentEditor(0);
        centerPane.setPreferredSize(new Dimension(centerPaneWidth, centerPane.getPreferredSize().height));
        arrowButton.setPreferredSize(new Dimension(20, centerPane.getPreferredSize().height));
        final Color beforeColor = arrowButton.getBackground();
        menu = createPopMenu();

        arrowButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent a) {
                if (cards != null && cards.length > 1) {
                    arrowButton.setBackground(new Color(228, 246, 255));
                    arrowButton.repaint();
                }
            }

            public void mouseExited(MouseEvent a) {
                arrowButton.setBackground(beforeColor);
                arrowButton.setBorder(null);
            }
        });
        arrowButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (cards != null && cards.length > 1) { // 如果只有“列”的话，就不需要弹出菜单了
                    Rectangle re = centerPane.getBounds();
                    menu.setPopupSize(re.width + arrowButton.getWidth(), menu.getPreferredSize().height);
                    menu.show(centerPane, -arrowButton.getWidth(), re.height);
                }
            }
        });

        this.add(centerPane, BorderLayout.CENTER);
        if (cards.length > 1) {
            this.add(arrowButton, BorderLayout.WEST);
        }
    }

    @Override
    protected String title4PopupWindow() {
        return Inter.getLocText("FR-Designer_Values-Editor");
    }

    public Editor getCurrentEditor() {
        return currentEditor;
    }

    public void setCurrentEditor(int i) {
        this.add(arrowButton, BorderLayout.WEST);
        currentEditor = this.cards[i];
        centerPane.removeAll();
        centerPane.add(currentEditor);
        centerPane.validate();
        centerPane.repaint();
        arrowButton.setIcon(cards[i].getIcon());
        if (this.cards.length == 1) {
            this.remove(arrowButton);
        }
    }

    public void setCurrentEditor(Class editorClass) {
        for (int i = 0; i < cards.length; i++) {
            if (cards[i].getClass() == editorClass) {
                setCurrentEditor(i);
                break;
            }
        }
    }


    private JPopupMenu createPopMenu() {
        JPopupMenu scate = new JPopupMenu();

        if (this.cards == null) {
            return scate;
        }

        for (int i = 0; i < this.cards.length; i++) {
            UIMenuItem item = new UIMenuItem(cards[i].getName());
            final int j = i;
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (globalNameListener != null) {
                        globalNameListener.setGlobalName(Inter.getLocText("CellWrite-InsertRow_Policy"));
                    }
                    Object oldValue = currentEditor.getValue();
                    setCurrentEditor(j);
                    currentEditor.selected();
                    value = currentEditor.getValue();
                    if (uiObserverListener != null) {
                        uiObserverListener.doChange();
                    }

                    ValueEditorPane.this.firePropertyChange("value", oldValue, value);
                }
            });
            scate.add(item);
            if (i < cards.length - 1) {
                scate.addSeparator();
            }
        }
        return scate;
    }

    public void populate(Object object) {
        for (int i = 0; i < cards.length; i++) {
            if (cards[i].accept(object)) {
                setCardValue(i,object);
                break;
            }
        }
    }

    public void populate(Object object,String name) {
        for (int i = 0; i < cards.length; i++) {
            if (cards[i].accept(object) && ComparatorUtils.equals(cards[i].getName(),name)) {
                setCardValue(i,object);
                break;
            }
        }
    }

    private void setCardValue(int i,Object object){
        setCurrentEditor(i);
        cards[i].setValue(object);
        // kunsnat: bug7861 所有的Editor值都要跟随改变, 因为populate的editor 从""
        // 一定是最后的Editor哦.
        for (int j = 0; j < cards.length; j++) {
            if (i == j) {
                continue;
            }
            this.cards[j].setValue(null);
        }
    }

    public Object update() {
        String name = currentEditor.getName();
        Object columnIndex = currentEditor.getValue();
        //bug86542,这边为啥要new一个公式出来，只保留content,其他属性全不要了?
//        if (ComparatorUtils.equals(name, Inter.getLocText("Formula"))) {
//            columnIndex = new Formula(columnIndex == null ? "" : columnIndex.toString());
//        }

        return columnIndex;
    }

    public Object update(String makeAdiff) {
        String name = currentEditor.getName();
        Object columnIndex = currentEditor.getValue();
        Object columnName = StringUtils.EMPTY;

        if (ComparatorUtils.equals(name, Inter.getLocText("FR-Designer_Formula"))) {
            columnIndex = new Formula(columnIndex == null ? "" : columnIndex.toString());
        }

        if (currentEditor instanceof ColumnNameEditor) {
            columnName = ((ColumnNameEditor) currentEditor).getColumnName();
        }

        return new Object[]{columnIndex, columnName};
    }

    public Object update(boolean isXMLA) {
        String name = currentEditor.getName();
        Object columnIndex = currentEditor.getValue();
        Object columnName = StringUtils.EMPTY;

        if (ComparatorUtils.equals(name, Inter.getLocText("FR-Designer_Formula"))) {
            columnIndex = new Formula(columnIndex == null ? "" : columnIndex.toString());
        }

        if (isXMLA) {
            columnName = ((XMLANameEditor) currentEditor).getColumnName();
        }

        return new Object[]{columnIndex, columnName};
    }

    public void setEditors(Editor[] editors, Object obj) {
        this.cards = editors;
        this.populate(obj);
    }

    /**
     * 检查是否有效
     *
     * @throws Exception 异常
     */
    public void checkValid() throws Exception {
        if (!(currentEditor instanceof TextEditor)) {
            return;
        }

        int i;
        boolean containFormulaType = false;
        for (i = 0; i < cards.length; i++) {
            if (ComparatorUtils.equals(cards[i].getName(), Inter.getLocText("FR-Engine_Parameter-Formula"))) {
                containFormulaType = true;
                break;
            }
        }
        if (!containFormulaType) {
            return;
        }

        final int j = i;

        if (!(currentEditor instanceof TextEditor)) {
            return;
        }
        String string = (String) currentEditor.getValue();
        if (isFormula(string)) {
            DesignerEnvManager designerEnvManager = DesignerEnvManager.getEnvManager();
            if (designerEnvManager.isSupportStringToFormula()) {
                if (!designerEnvManager.isDefaultStringToFormula()) {
                    int returnValue = JOptionPane.showConfirmDialog(DesignerContext.getDesignerFrame(), Inter.getLocText("FR-Designer_Edit_String_To_Formula")
                            + "?", Inter.getLocText("FR-Designer_Tooltips"), JOptionPane.YES_NO_OPTION);
                    if (returnValue == JOptionPane.OK_OPTION) {

                        setCurrentEditor(j);
                        Formula formula = new Formula(string);
                        currentEditor.setValue(formula);
                    }
                } else {
                    setCurrentEditor(j);
                    Formula formula = new Formula(string);
                    currentEditor.setValue(formula);
                }
            }
        }

    }

    private boolean isFormula(String string) {
        return StringUtils.isNotBlank(string) && (string.length() > 0 && string.charAt(0) == '=');
    }

    @Override
    public void setEnabled(boolean enabled) {
        arrowButton.setEnabled(enabled);
        for (Editor card : cards) {
            card.setEnabled(enabled);
        }
    }

    /**
     * 重置组件
     */
    public void resetComponets() {
        for (Editor card : cards) {
            card.reset();
        }
    }

    /**
     * 清除组件数据
     */
    public void clearComponentsData() {
        for (Editor card : cards) {
            card.clearData();
        }
    }

    public Editor[] getCards() {
        return this.cards;
    }

    public JPopupMenu getMenu() {
        return this.menu;
    }


    /**
     * 注册全局名字监听事件
     *
     * @param listener 观察者监听事件
     */
    public void registerNameListener(GlobalNameListener listener) {
        globalNameListener = listener;
    }

    /**
     * 是否对名字listener监听器做出响应
     *
     * @return 如果要做出响应，则返回true
     */
    public boolean shouldResponseNameListener() {
        return false;
    }

    public void setGlobalName(String name) {
        for (Editor card : cards) {
            setComponentGlobalName(card, name);
        }
    }

    private void setComponentGlobalName(Container card, String name) {
        for (int i = 0, len = card.getComponentCount(); i < len; i++) {
            Component component = card.getComponent(i);
            if (component instanceof GlobalNameObserver) {
                ((GlobalNameObserver) component).setGlobalName(name);
            } else {
                setComponentGlobalName((Container) component, name);
            }
        }
    }

    /**
     * 给组件登记一个观察者监听事件
     *
     * @param listener 观察者监听事件
     */
    public void registerChangeListener(UIObserverListener listener) {
        uiObserverListener = listener;
        for (Editor card : cards) {
            doLoop(card, listener);
        }
    }

    private void doLoop(Container card, UIObserverListener listener) {
        for (int i = 0, len = card.getComponentCount(); i < len; i++) {
            Component component = card.getComponent(i);
            if (component instanceof UIObserver) {
                ((UIObserver) component).registerChangeListener(listener);
            } else {
                doLoop((Container) component, listener);
            }
        }
    }

    /**
     * 组件是否需要响应添加的观察者事件
     *
     * @return 如果需要响应观察者事件则返回true，否则返回false
     */
    public boolean shouldResponseChangeListener() {
        return true;
    }
}