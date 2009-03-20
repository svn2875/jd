package jd.plugins.optional.langfileeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.tasks.TaskPanel;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class LFETaskPane extends TaskPanel implements ActionListener {

    private static final long serialVersionUID = -5506038175097521342L;
    private JButton showGui;
    private LFEGui lfe = null;

    public LFETaskPane(String string, ImageIcon ii) {
        super(string, ii, "lfe");
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill]"));

        initGUI();
    }

    private void initGUI() {
        showGui = createButton(JDLocale.L("plugins.optional.langfileeditor.taskpane", "Show LFE"), JDTheme.II("gui.images.jd_logo", 16, 16));
        showGui.addActionListener(this);
        showGui.setHorizontalAlignment(JButton.LEFT);
        add(showGui, "alignx leading");
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == showGui) {
            if (lfe == null) lfe = new LFEGui();
            SimpleGUI.CURRENTGUI.getContentPane().display(lfe);
            SimpleGUI.CURRENTGUI.getTaskPane().switcher(this);
        }
    }

}
