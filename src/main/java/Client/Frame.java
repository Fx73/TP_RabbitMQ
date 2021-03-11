package Client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

class Frame extends JFrame {
    private static Frame window = null;

    public static Frame getWindow() {
        int timeout = 10;
        while (window == null) {
            try {
                Thread.sleep(2000);
                timeout--;
            } catch (Exception e) {
            }
            if (timeout <= 0) {

                System.out.println("Client.Frame is missing");
                System.exit(1);
            }
        }
        return window;
    }

    public static Frame setWindow(String name) {
        System.out.println("Building Client.Frame ...");
        return window = new Frame(name);
    }

    private final JTextArea _chattextarea = new JTextArea();
    private final JTextArea _messagearea = new JTextArea();
    private final JPanel roombuttoncontainer = new JPanel();
    private final JPanel roomnamecontainer = new JPanel();

    public JTextField user;
    private final JTextPane usersconnected = new JTextPane();

    private Frame(String target) {
        super("RabbitMQ Chat : " + target);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1000, 600));


        //Barre de chat
        final JButton buttonsay = new JButton("Say");
        buttonsay.addActionListener(e -> {
            ChatClient.Say(_messagearea.getText());
            _messagearea.setText("");
        });
        buttonsay.setMinimumSize(new Dimension(40, 40));
        final JSplitPaneWithZeroSizeDivider saypane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT, buttonsay, _messagearea);
        saypane.setResizeWeight(0.1);
        saypane.setEnabled(false);

        //Zone de dialogue
        _chattextarea.setEditable(false);
        _chattextarea.setLineWrap(true);
        final JSplitPaneWithZeroSizeDivider chatpane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(_chattextarea), roomnamecontainer);
        chatpane.setResizeWeight(1);
        final JSplitPaneWithZeroSizeDivider rightpane = new JSplitPaneWithZeroSizeDivider(JSplitPane.VERTICAL_SPLIT, chatpane, saypane);
        rightpane.setResizeWeight(1);


        //panel de droite
        roombuttoncontainer.setLayout(new BoxLayout(roombuttoncontainer, BoxLayout.Y_AXIS));
        roombuttoncontainer.setBorder(new EmptyBorder(new Insets(3, 3, 2000, 3)));
        usersconnected.setContentType("text/html");
        usersconnected.setText("<html><center><b>Connected\n<br>\nUsers</b></center></html>");
        usersconnected.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        usersconnected.setEnabled(false);

        roomnamecontainer.setLayout(new BoxLayout(roomnamecontainer, BoxLayout.Y_AXIS));
        roomnamecontainer.setBorder(new EmptyBorder(new Insets(3, 3, 2000, 3)));
        roomnamecontainer.add(usersconnected);


        //Pannel de gauche
        user = new JTextField();
        user.setText(System.getProperty("user.name"));
        user.setHorizontalAlignment(JTextField.CENTER);
        user.setEnabled(false);
        user.addMouseListener(nommouselistener);
        roombuttoncontainer.setLayout(new BoxLayout(roombuttoncontainer, BoxLayout.Y_AXIS));
        roombuttoncontainer.setBorder(new EmptyBorder(new Insets(3, 3, 2000, 3)));
        final JSplitPaneWithZeroSizeDivider roompane = new JSplitPaneWithZeroSizeDivider(JSplitPane.VERTICAL_SPLIT, user, roombuttoncontainer);


        //Panel + et -
        final JButton buttonplus = new JButton("+");
        buttonplus.addActionListener(e -> ChatClient.Create_Room());
        final JButton buttonmoins = new JButton("-");
        buttonmoins.addActionListener(e -> ChatClient.Delete_Room());

        final JSplitPaneWithZeroSizeDivider toolpane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT, buttonplus, buttonmoins);
        toolpane.setResizeWeight(0.5);

        final JSplitPaneWithZeroSizeDivider leftpane = new JSplitPaneWithZeroSizeDivider(JSplitPane.VERTICAL_SPLIT, roompane, toolpane);
        leftpane.setResizeWeight(1);
        final JSplitPaneWithZeroSizeDivider mainpane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT, leftpane, rightpane);
        mainpane.setResizeWeight(0.05);
        mainpane.setEnabled(false);


        add(mainpane);
        pack();
    }

    public void set_chattextarea(String area) {
        _chattextarea.setText(area);
        Frame.getWindow().revalidate();
    }

    public String[] buttonlist = new String[0];

    public void UpdateButtons(String[] newbuttonlist) {
        if (Arrays.equals(buttonlist, newbuttonlist)) return;
        roombuttoncontainer.removeAll();

        for (int i = 0; i < newbuttonlist.length; i++) {
            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.setBorder(new EmptyBorder(new Insets(3, 0, 0, 0)));

            JButton b = new JButton(newbuttonlist[i]);
            b.addActionListener(e -> ChatClient.Select_Room(((JButton) e.getSource()).getText()));

            p.add(b);
            roombuttoncontainer.add(p);
        }
        buttonlist = newbuttonlist;
        pack();
    }

    public String[] namelist = new String[0];

    public void UpdateNames(String[] newnamelist) {
        if (Arrays.equals(buttonlist, newnamelist)) return;
        roomnamecontainer.removeAll();
        roomnamecontainer.add(usersconnected);

        for (int i = 0; i < newnamelist.length; i++) {
            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.setBorder(new EmptyBorder(new Insets(3, 0, 0, 0)));

            JTextField t = new JTextField(newnamelist[i]);
            t.setForeground(Color.BLUE);

            p.add(t);
            roomnamecontainer.add(p);
        }
        buttonlist = newnamelist;
        pack();
    }

    MouseListener nommouselistener = new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent e) {
            String result = (String) JOptionPane.showInputDialog(
                    Frame.getWindow(),
                    "Change your name",
                    "Name",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    Frame.getWindow().user.getText()
            );

            if (result != null)
                Frame.getWindow().user.setText(result);
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }
    };
}
