package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.model.Flashcard;
import ph.studytabai.model.FlashcardSet;

import javax.swing.*;
import java.awt.*;

public final class FlashcardEditorDialog extends JDialog {
    private final AppContext ctx;
    private final FlashcardSet set;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JTextField title = new JTextField(30);

    public FlashcardEditorDialog(Window owner, AppContext ctx, FlashcardSet set) {
        super(owner, "Flashcard editor", ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        this.set = set;
        title.setText(set.getTitle());
        Ui.name(list, "Flashcards");
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) edit(); }
        });
        JPanel side = new JPanel(new GridLayout(0, 1, 4, 4));
        side.add(Ui.button("Add card", this::add));
        side.add(Ui.button("Edit selected", this::edit));
        side.add(Ui.button("Delete selected", this::delete));
        JPanel sideWrap = new JPanel(new BorderLayout());
        sideWrap.add(side, BorderLayout.NORTH);
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(Ui.labeled("Set title", title), BorderLayout.NORTH);
        p.add(Ui.scroll(list), BorderLayout.CENTER);
        p.add(sideWrap, BorderLayout.EAST);
        p.add(Ui.flow(Ui.button("Save & close", this::saveAndClose), Ui.button("Close without saving", this::dispose)), BorderLayout.SOUTH);
        setContentPane(p);
        reload();
        setSize(760, 520);
        setLocationRelativeTo(owner);
    }

    private void reload() {
        model.clear();
        int i = 0;
        for (Flashcard c : set.getCards()) model.addElement(++i + ". " + c.getFront() + "  →  " + c.getBack());
    }

    private Flashcard ask(Flashcard base) {
        JTextArea front = Ui.textArea(3, 30), back = Ui.textArea(3, 30);
        if (base != null) { front.setText(base.getFront()); back.setText(base.getBack()); }
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        p.add(Ui.labeled("Front (term or question)", Ui.scroll(front)));
        p.add(Ui.labeled("Back (answer)", Ui.scroll(back)));
        while (JOptionPane.showConfirmDialog(this, p, "Flashcard", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                return new Flashcard(front.getText(), back.getText());
            } catch (IllegalArgumentException e) { Ui.error(this, e.getMessage()); }
        }
        return null;
    }

    private void add() { Flashcard c = ask(null); if (c != null) { set.addCard(c); reload(); } }

    private void edit() {
        int i = list.getSelectedIndex();
        if (i < 0) { Ui.error(this, "Select a card first."); return; }
        Flashcard c = ask(set.getCards().get(i));
        if (c != null) { set.setCard(i, c); reload(); }
    }

    private void delete() {
        int i = list.getSelectedIndex();
        if (i >= 0 && Ui.confirm(this, "Delete this card?")) { set.removeCard(i); reload(); }
    }

    private void saveAndClose() {
        try { set.setTitle(title.getText()); } catch (IllegalArgumentException e) { Ui.error(this, e.getMessage()); return; }
        ctx.activities.save(set);
        dispose();
    }
}
