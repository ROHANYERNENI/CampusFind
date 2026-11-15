import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

/*
 * CampusFind - beginner-friendly Java Swing prototype.
 * Compile: javac CampusFind.java
 * Run:     java CampusFind
 *
 * Demo admin login: admin / admin123
 * Students can register from the login screen.
 *
 * Data is saved in campusfind-data.ser in the working directory.
 * This local prototype uses Java serialization and is NOT intended for
 * production use or for storing real sensitive student information.
 */
public class CampusFind {
    private static final String DATA_FILE = "campusfind-data.ser";
    private static Store store = loadStore();
    private static User currentUser;

    private JFrame frame;
    private JTabbedPane tabs;
    private JLabel statusLabel;

    // ---------- Data classes ----------
    static class User implements Serializable {
        String id, name, password, role;
        int points;
        User(String id, String name, String password, String role) {
            this.id=id; this.name=name; this.password=password;
            this.role=role; this.points=0;
        }
    }

    static class Item implements Serializable {
        int id;
        String type, reporterId, name, category, color, location, description;
        String secretClue, status;
        LocalDate reportDate;
        Item(int id, String type, String reporterId, String name, String category,
             String color, String location, String description, String secretClue) {
            this.id=id; this.type=type; this.reporterId=reporterId;
            this.name=name; this.category=category; this.color=color;
            this.location=location; this.description=description;
            this.secretClue=secretClue; this.reportDate=LocalDate.now();
            this.status=type.equals("LOST") ? "LOST" : "FOUND";
        }
    }

    static class Claim implements Serializable {
        int id, itemId;
        String studentId, status, providedClue;
        LocalDateTime created;
        Claim(int id, int itemId, String studentId, String providedClue) {
            this.id=id; this.itemId=itemId; this.studentId=studentId;
            this.providedClue=providedClue;
            this.status="PENDING"; this.created=LocalDateTime.now();
        }
    }

    static class Notice implements Serializable {
        String userId, message;
        LocalDateTime created;
        boolean read;
        Notice(String userId, String message) {
            this.userId=userId; this.message=message;
            this.created=LocalDateTime.now(); this.read=false;
        }
    }

    static class History implements Serializable {
        int itemId;
        String status, note;
        LocalDateTime time;
        History(int itemId, String status, String note) {
            this.itemId=itemId; this.status=status; this.note=note;
            this.time=LocalDateTime.now();
        }
    }

    static class Store implements Serializable {
        ArrayList<User> users = new ArrayList<>();
        ArrayList<Item> items = new ArrayList<>();
        ArrayList<Claim> claims = new ArrayList<>();
        ArrayList<Notice> notices = new ArrayList<>();
        ArrayList<History> histories = new ArrayList<>();
        int nextItemId=1, nextClaimId=1;

        Store() {
            users.add(new User("admin", "Campus Admin", "admin123", "ADMIN"));
        }
    }

    // ---------- Persistence ----------
    private static Store loadStore() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            return (Store) in.readObject();
        } catch (Exception ex) {
            return new Store();
        }
    }

    private static void saveStore() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(store);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Could not save data: " + ex.getMessage());
        }
    }

    // ---------- Startup / login ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CampusFind().showLogin());
    }

    private void showLogin() {
        JFrame login = new JFrame("CampusFind - Login");
        login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        login.setSize(430, 330);
        login.setLocationRelativeTo(null);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7,7,7,7);
        c.fill = GridBagConstraints.HORIZONTAL;
        JLabel title = new JLabel("CampusFind", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        c.gridx=0; c.gridy=0; c.gridwidth=2; p.add(title,c);
        c.gridwidth=1;
        c.gridy++; c.gridx=0; p.add(new JLabel("Student ID / Username:"),c);
        JTextField id = new JTextField(18); c.gridx=1; p.add(id,c);
        c.gridy++; c.gridx=0; p.add(new JLabel("Password:"),c);
        JPasswordField pw = new JPasswordField(18); c.gridx=1; p.add(pw,c);
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register Student");
        c.gridy++; c.gridx=0; p.add(loginBtn,c); c.gridx=1; p.add(registerBtn,c);
        JLabel hint = new JLabel("<html><center>Demo admin: admin / admin123</center></html>",SwingConstants.CENTER);
        c.gridy++; c.gridx=0; c.gridwidth=2; p.add(hint,c);

        loginBtn.addActionListener(e -> {
            String username=id.getText().trim();
            String password=new String(pw.getPassword());
            User found=null;
            for(User u:store.users)
                if(u.id.equalsIgnoreCase(username) && u.password.equals(password)) { found=u; break; }
            if(found==null) {
                JOptionPane.showMessageDialog(login,"Invalid username or password.");
            } else {
                currentUser=found; login.dispose(); showMain();
            }
        });

        registerBtn.addActionListener(e -> {
            JTextField sid=new JTextField(), name=new JTextField();
            JPasswordField pass=new JPasswordField();
            Object[] fields={"Student ID:",sid,"Full name:",name,"Password:",pass};
            int result=JOptionPane.showConfirmDialog(login,fields,"Student Registration",JOptionPane.OK_CANCEL_OPTION);
            if(result==JOptionPane.OK_OPTION) {
                String studentId=sid.getText().trim(), studentName=name.getText().trim();
                String password=new String(pass.getPassword());
                if(studentId.isEmpty() || studentName.isEmpty() || password.length()<4) {
                    JOptionPane.showMessageDialog(login,"Enter all details. Password must be at least 4 characters."); return;
                }
                for(User u:store.users) if(u.id.equalsIgnoreCase(studentId)) {
                    JOptionPane.showMessageDialog(login,"That Student ID is already registered."); return;
                }
                store.users.add(new User(studentId,studentName,password,"STUDENT"));
                saveStore();
                JOptionPane.showMessageDialog(login,"Registration successful. You can now log in.");
            }
        });

        login.add(p);
        login.setVisible(true);
    }

    // ---------- Main window ----------
    private void showMain() {
        frame=new JFrame("CampusFind - Welcome " + currentUser.name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000,700);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JPanel top=new JPanel(new BorderLayout());
        JLabel heading=new JLabel("  CAMPUSFIND  |  Lost-and-Found Recovery System");
        heading.setFont(new Font("SansSerif",Font.BOLD,19));
        top.add(heading,BorderLayout.WEST);
        JButton logout=new JButton("Logout");
        logout.addActionListener(e->{ frame.dispose(); showLogin(); });
        top.add(logout,BorderLayout.EAST);
        frame.add(top,BorderLayout.NORTH);

        tabs=new JTabbedPane();
        tabs.addTab("Home",homePanel());
        tabs.addTab("Report Lost",reportPanel("LOST"));
        tabs.addTab("Report Found",reportPanel("FOUND"));
        tabs.addTab("Find Matches",matchesPanel());
        tabs.addTab("Claim Item",claimPanel());
        tabs.addTab("My Reports",myReportsPanel());
        tabs.addTab("Notifications",notificationsPanel());
        tabs.addTab("Journey Tracker",trackerPanel());
        if(currentUser.role.equals("ADMIN")) tabs.addTab("Admin Dashboard",adminPanel());
        frame.add(tabs,BorderLayout.CENTER);

        statusLabel=new JLabel("  Signed in as " + currentUser.role + " | Points: " + currentUser.points);
        frame.add(statusLabel,BorderLayout.SOUTH);
        frame.setVisible(true);
        checkReminders(false);
    }

    private JPanel homePanel() {
        JPanel p=new JPanel(new BorderLayout(12,12));
        p.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
        JLabel welcome=new JLabel("<html><h1>Welcome to CampusFind</h1>"
                + "<p>Report lost items, register found items and help belongings return to their owners.</p>"
                + "<p>Use the tabs above to access the project modules.</p></html>");
        p.add(welcome,BorderLayout.NORTH);
        JTextArea info=new JTextArea();
        info.setEditable(false);
        info.setFont(new Font("SansSerif",Font.PLAIN,15));
        info.setText("Quick guide\n\n"
                + "1. Report Lost: enter details of an item you lost.\n"
                + "2. Report Found: register an item you found and add a private clue.\n"
                + "3. Find Matches: compare lost and found reports.\n"
                + "4. Claim Item: submit a claim and verify the private clue.\n"
                + "5. Journey Tracker: view item status history.\n"
                + "Please do not publish private ownership clues or sensitive personal information.");
        p.add(new JScrollPane(info),BorderLayout.CENTER);
        return p;
    }

    // ---------- Report forms ----------
    private JPanel reportPanel(String type) {
        JPanel p=new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(18,35,18,35));
        GridBagConstraints c=new GridBagConstraints();
        c.insets=new Insets(6,6,6,6); c.fill=GridBagConstraints.HORIZONTAL;
        JTextField name=new JTextField(22), color=new JTextField(22), description=new JTextField(22);
        JComboBox<String> category=new JComboBox<>(new String[]{"Electronics","Books","ID Card","Bottle","Bag","Clothing","Keys","Other"});
        JComboBox<String> location=new JComboBox<>(new String[]{"Library","Canteen","Block A","Block B","Computer Lab","Laboratory","Playground","Parking Area","Auditorium","Other"});
        JTextField clue=new JTextField(22);
        int row=0;
        addFormRow(p,c,row++,"Item name:",name);
        addFormRow(p,c,row++,"Category:",category);
        addFormRow(p,c,row++,"Colour:",color);
        addFormRow(p,c,row++,"Campus location:",location);
        addFormRow(p,c,row++,"Description:",description);
        if(type.equals("FOUND")) addFormRow(p,c,row++,"Private clue (not public):",clue);
        JButton submit=new JButton(type.equals("LOST")?"Submit Lost Report":"Submit Found Report");
        c.gridx=0;c.gridy=row;c.gridwidth=2;p.add(submit,c);
        submit.addActionListener(e -> {
            String n=name.getText().trim(), col=color.getText().trim();
            if(n.isEmpty() || col.isEmpty()) {
                JOptionPane.showMessageDialog(frame,"Item name and colour are required."); return;
            }
            if(type.equals("FOUND") && clue.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame,"Enter a private clue for ownership verification."); return;
            }
            if(isDuplicate(type,n,col,location.getSelectedItem().toString())) {
                int choice=JOptionPane.showConfirmDialog(frame,
                        "A similar " + type.toLowerCase() + " report exists. Submit anyway?",
                        "Possible duplicate",JOptionPane.YES_NO_OPTION);
                if(choice!=JOptionPane.YES_OPTION) return;
            }
            Item item=new Item(store.nextItemId++,type,currentUser.id,n,
                    category.getSelectedItem().toString(),col,
                    location.getSelectedItem().toString(),description.getText().trim(),
                    type.equals("FOUND")?clue.getText().trim():"");
            store.items.add(item);
            store.histories.add(new History(item.id,item.status,"Report created by "+currentUser.id));
            addNotice(currentUser.id,type+" report submitted for "+n+".");
            saveStore();
            JOptionPane.showMessageDialog(frame,"Report submitted. Item ID: "+item.id);
            name.setText(""); color.setText(""); description.setText(""); clue.setText("");
            refreshTabs();
        });
        return p;
    }

    private void addFormRow(JPanel p,GridBagConstraints c,int row,String label,JComponent field) {
        c.gridwidth=1;c.gridy=row;c.gridx=0;c.weightx=0;
        p.add(new JLabel(label),c);
        c.gridx=1;c.weightx=1;p.add(field,c);
    }

    private boolean isDuplicate(String type,String name,String color,String location) {
        for(Item i:store.items) {
            if(i.type.equals(type) && !i.status.equals("RETURNED")
                    && i.name.equalsIgnoreCase(name)
                    && i.color.equalsIgnoreCase(color)
                    && i.location.equalsIgnoreCase(location)) return true;
        }
        return false;
    }

    // ---------- Matching ----------
    private int matchScore(Item lost,Item found) {
        int score=0;
        if(lost.name.equalsIgnoreCase(found.name)) score+=40;
        else if(lost.name.toLowerCase().contains(found.name.toLowerCase())
                || found.name.toLowerCase().contains(lost.name.toLowerCase())) score+=20;
        if(lost.category.equalsIgnoreCase(found.category)) score+=20;
        if(lost.color.equalsIgnoreCase(found.color)) score+=20;
        if(lost.location.equalsIgnoreCase(found.location)) score+=20;
        return score;
    }

    private JPanel matchesPanel() {
        JPanel p=new JPanel(new BorderLayout(8,8));
        String[] cols={"Lost ID","Lost Item","Found ID","Found Item","Location","Match %","Match level"};
        DefaultTableModel model=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        JTable table=new JTable(model);
        JButton refresh=new JButton("Refresh Matches");
        refresh.addActionListener(e->populateMatches(model));
        p.add(refresh,BorderLayout.NORTH);p.add(new JScrollPane(table),BorderLayout.CENTER);
        populateMatches(model);
        return p;
    }

    private void populateMatches(DefaultTableModel model) {
        model.setRowCount(0);
        for(Item lost:store.items) if(lost.type.equals("LOST") && lost.status.equals("LOST")) {
            for(Item found:store.items) if(found.type.equals("FOUND") && found.status.equals("FOUND")) {
                int score=matchScore(lost,found);
                if(score>=40) {
                    String level=score>=80?"Strong":score>=50?"Possible":"Low";
                    model.addRow(new Object[]{lost.id,lost.name,found.id,found.name,found.location,score+"%",level});
                    if(score>=50 && !lost.reporterId.equals(found.reporterId))
                        addNotice(lost.reporterId,"Possible match ("+score+"%) for your lost item: "+lost.name+". Found item ID: "+found.id);
                }
            }
        }
        saveStore();
    }

    // ---------- Claim / verification ----------
    private JPanel claimPanel() {
        JPanel p=new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(25,35,25,35));
        GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(8,8,8,8);c.fill=GridBagConstraints.HORIZONTAL;
        JTextField itemId=new JTextField(18), clue=new JTextField(18);
        int row=0;
        addFormRow(p,c,row++,"Found item ID:",itemId);
        addFormRow(p,c,row++,"Your private identifying detail:",clue);
        JButton submit=new JButton("Submit Claim");
        c.gridx=0;c.gridy=row;c.gridwidth=2;p.add(submit,c);
        submit.addActionListener(e->{
            try {
                int id=Integer.parseInt(itemId.getText().trim());
                Item item=findItem(id);
                if(item==null || !item.type.equals("FOUND") || !item.status.equals("FOUND")) {
                    JOptionPane.showMessageDialog(frame,"Found item not available. Check the ID.");return;
                }
                if(item.reporterId.equals(currentUser.id)) {
                    JOptionPane.showMessageDialog(frame,"You cannot claim an item that you reported as found.");return;
                }
                for(Claim old:store.claims)
                    if(old.itemId==id && old.studentId.equals(currentUser.id) && old.status.equals("APPROVED")) {
                        JOptionPane.showMessageDialog(frame,"Your claim was already approved.");return;
                    }
                Claim claim=new Claim(store.nextClaimId++,id,currentUser.id,clue.getText().trim());
                store.claims.add(claim);
                addNotice(item.reporterId,"A student submitted a claim for your found item #"+item.id+".");
                addNotice(currentUser.id,"Your claim for item #"+item.id+" was sent to the administrator for review.");
                store.histories.add(new History(item.id,"CLAIM_PENDING","A claim was submitted for review."));
                saveStore();
                JOptionPane.showMessageDialog(frame,"Claim submitted. An administrator will review the private identifying detail.");
                store.claims.add(claim);saveStore();refreshTabs();
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,"Enter a valid numeric item ID.");
            }
        });
        return p;
    }

    // ---------- My reports ----------
    private JPanel myReportsPanel() {
        JPanel p=new JPanel(new BorderLayout());
        DefaultTableModel model=new DefaultTableModel(new String[]{"ID","Type","Name","Category","Colour","Location","Date","Status"},0){
            public boolean isCellEditable(int r,int c){return false;}
        };
        JTable table=new JTable(model);
        for(Item i:store.items) if(currentUser.role.equals("ADMIN") || i.reporterId.equals(currentUser.id))
            model.addRow(new Object[]{i.id,i.type,i.name,i.category,i.color,i.location,i.reportDate,i.status});
        p.add(new JScrollPane(table),BorderLayout.CENTER);
        return p;
    }

    // ---------- Notifications ----------
    private JPanel notificationsPanel() {
        JPanel p=new JPanel(new BorderLayout());
        DefaultTableModel model=new DefaultTableModel(new String[]{"Message","Date","Read"},0){
            public boolean isCellEditable(int r,int c){return false;}
        };
        for(Notice n:store.notices) if(n.userId.equals(currentUser.id))
            model.addRow(new Object[]{n.message,n.created,n.read?"Yes":"No"});
        JTable table=new JTable(model);
        JButton mark=new JButton("Mark all as read");
        mark.addActionListener(e->{
            for(Notice n:store.notices) if(n.userId.equals(currentUser.id)) n.read=true;
            saveStore();refreshTabs();
        });
        p.add(mark,BorderLayout.NORTH);p.add(new JScrollPane(table),BorderLayout.CENTER);
        return p;
    }

    // ---------- Tracker ----------
    private JPanel trackerPanel() {
        JPanel p=new JPanel(new BorderLayout(8,8));
        JPanel top=new JPanel();
        JTextField id=new JTextField(8); JButton search=new JButton("Show Journey");
        top.add(new JLabel("Item ID:"));top.add(id);top.add(search);
        JTextArea output=new JTextArea();output.setEditable(false);output.setFont(new Font("Monospaced",Font.PLAIN,13));
        search.addActionListener(e->{
            output.setText("");
            try {
                int itemId=Integer.parseInt(id.getText().trim());
                Item item=findItem(itemId);
                if(item==null){output.setText("Item not found.");return;}
                if(!currentUser.role.equals("ADMIN") && !item.reporterId.equals(currentUser.id)) {
                    boolean involved=false;
                    for(Claim cl:store.claims) if(cl.itemId==itemId && cl.studentId.equals(currentUser.id)) involved=true;
                    if(!involved){output.setText("You do not have permission to view this item's journey.");return;}
                }
                output.append("Journey for: "+item.name+" (ID "+item.id+")\n\n");
                for(History h:store.histories) if(h.itemId==itemId)
                    output.append(h.time+"  |  "+h.status+"  |  "+h.note+"\n");
                output.append("\nCurrent status: "+item.status);
            } catch(NumberFormatException ex){output.setText("Enter a valid item ID.");}
        });
        p.add(top,BorderLayout.NORTH);p.add(new JScrollPane(output),BorderLayout.CENTER);
        return p;
    }


    // ---------- Admin ----------
    private JPanel adminPanel() {
        JPanel p=new JPanel(new BorderLayout(8,8));
        JPanel summary=new JPanel(new GridLayout(2,3,8,8));
        int lost=0,found=0,returned=0,pending=0,unclaimed=0;
        for(Item i:store.items) {
            if(i.type.equals("LOST"))lost++;
            if(i.type.equals("FOUND")) {
                found++;
                if(i.status.equals("RETURNED"))returned++;
                else unclaimed++;
            }
        }
        for(Claim cl:store.claims)if(cl.status.equals("PENDING"))pending++;
        summary.add(statCard("Lost reports",lost));
        summary.add(statCard("Found reports",found));
        summary.add(statCard("Returned items",returned));
        summary.add(statCard("Pending claims",pending));
        summary.add(statCard("Unclaimed found items",unclaimed));
        summary.add(statCard("Registered users",store.users.size()));
        p.add(summary,BorderLayout.NORTH);

        DefaultTableModel cm=new DefaultTableModel(new String[]{"Claim ID","Item ID","Student ID","Status","Submitted"},0){
            public boolean isCellEditable(int r,int c){return false;}
        };
        for(Claim cl:store.claims)cm.addRow(new Object[]{cl.id,cl.itemId,cl.studentId,cl.status,cl.created});
        JTable claims=new JTable(cm);
        JPanel buttons=new JPanel();
        JButton approve=new JButton("Approve selected (admin review)");
        JButton reject=new JButton("Reject selected");
        buttons.add(approve);buttons.add(reject);
        approve.addActionListener(e->adminReviewClaim(claims,cm,true));
        reject.addActionListener(e->adminReviewClaim(claims,cm,false));
        JPanel center=new JPanel(new BorderLayout());
        center.add(new JScrollPane(claims),BorderLayout.CENTER);center.add(buttons,BorderLayout.SOUTH);
        p.add(center,BorderLayout.CENTER);
        return p;
    }

    private JPanel statCard(String title,int value) {
        JPanel p=new JPanel(new GridLayout(2,1));
        p.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        JLabel t=new JLabel(title,SwingConstants.CENTER);
        JLabel v=new JLabel(String.valueOf(value),SwingConstants.CENTER);
        v.setFont(new Font("SansSerif",Font.BOLD,22));
        p.add(t);p.add(v);return p;
    }

    private void adminReviewClaim(JTable table,DefaultTableModel model,boolean approve) {
        int row=table.getSelectedRow();
        if(row<0){JOptionPane.showMessageDialog(frame,"Select a claim first.");return;}
        int claimId=(int)model.getValueAt(row,0);
        Claim claim=null;
        for(Claim c:store.claims)if(c.id==claimId)claim=c;
        if(claim==null || !claim.status.equals("PENDING")) {
            JOptionPane.showMessageDialog(frame,"Only pending claims can be reviewed.");return;
        }
        Item item=findItem(claim.itemId);
        if(item==null){JOptionPane.showMessageDialog(frame,"Related item not found.");return;}
        if(approve) {
            // The admin action checks the private clue without displaying it in the table.
            if(claim.providedClue.isEmpty() ||
                    !claim.providedClue.equalsIgnoreCase(item.secretClue)) {
                JOptionPane.showMessageDialog(frame,
                        "Ownership detail did not match. Reject this claim or ask for further verification.");
                return;
            }
            claim.status="APPROVED";item.status="RETURNED";
            store.histories.add(new History(item.id,"CLAIM_APPROVED","Admin verified the private ownership clue."));
            store.histories.add(new History(item.id,"RETURNED","Admin marked item returned."));
            User finder=findUser(item.reporterId);if(finder!=null)finder.points+=25;
            addNotice(claim.studentId,"Admin approved your claim for item #"+item.id+".");
            addNotice(item.reporterId,"Item #"+item.id+" was marked returned. 25 points awarded.");
        } else {
            claim.status="REJECTED";
            store.histories.add(new History(item.id,"CLAIM_REJECTED","Admin rejected claim."));
            addNotice(claim.studentId,"Admin rejected your claim for item #"+item.id+".");
        }
        saveStore();refreshTabs();
    }

    // ---------- Helpers ----------
    private void checkReminders(boolean showMessage) {
        ArrayList<String> due=new ArrayList<>();
        for(Item i:store.items) if(i.type.equals("FOUND") && i.status.equals("FOUND")) {
            long days=ChronoUnit.DAYS.between(i.reportDate,LocalDate.now());
            if(days>=7) {
                String msg="Unclaimed item #"+i.id+" ("+i.name+") has been listed for "+days+" days.";
                due.add(msg);
                if(currentUser.role.equals("ADMIN")) addNotice("admin",msg);
            }
        }
        if(showMessage && !due.isEmpty()) JOptionPane.showMessageDialog(frame,String.join("\n",due),"Unclaimed reminders",JOptionPane.INFORMATION_MESSAGE);
        saveStore();
    }

    private void addNotice(String userId,String message) {
        for(Notice n:store.notices)
            if(n.userId.equals(userId) && n.message.equals(message)) return;
        store.notices.add(new Notice(userId,message));
    }

    private Item findItem(int id) {
        for(Item i:store.items)if(i.id==id)return i;
        return null;
    }

    private User findUser(String id) {
        for(User u:store.users)if(u.id.equals(id))return u;
        return null;
    }

    private void refreshTabs() {
        if(frame==null)return;
        int selected=tabs.getSelectedIndex();
        frame.dispose();
        showMain();
        if(selected>=0 && selected<tabs.getTabCount())tabs.setSelectedIndex(selected);
    }
}
