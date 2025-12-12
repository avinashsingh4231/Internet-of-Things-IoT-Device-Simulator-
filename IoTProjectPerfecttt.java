import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * IoTProjectPerfecttt.java
 * Final single-file IoT simulator (Fullscreen Enabled)
 *
 * Compile: javac IoTProjectPerfecttt.java
 * Run:     java IoTProjectPerfecttt
 */
public class IoTProjectPerfecttt {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(StartScreen::new);
    }
}

/* --------------------- Start Screen --------------------- */
class StartScreen extends JFrame {
    private SensorCard tempCard;
    private SensorCard motionCard;

    StartScreen() {
        setTitle("IoT Simulator — Select Sensors");

        /* ---------- FULL SCREEN ENABLED ---------- */
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel background = new GradientPanel(new Color(10, 15, 28), new Color(28, 40, 62));
        background.setLayout(new BorderLayout(10,10));
        add(background);

        JLabel title = new JLabel(
                "<html><center><b style='font-size:30px;color:#FFFFFF;'>IoT Device Simulator</b><br>" +
                "<span style='color:#b0d5ff;font-size:14px;'>Choose which sensors you want to run</span></center></html>",
                SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(22,10,6,10));
        background.add(title, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1,2,28,10));
        cards.setOpaque(false);
        cards.setBorder(new EmptyBorder(24, 60, 24, 60));

        tempCard = new SensorCard("Temperature Sensor", "Measures temperature (°C)", new Color(72,201,176), true);
        motionCard = new SensorCard("Motion Sensor", "Detects activity (0/1)", new Color(245,158,11), true);

        cards.add(tempCard);
        cards.add(motionCard);
        background.add(cards, BorderLayout.CENTER);

        NeonButton startBtn = new NeonButton("START SIMULATION", new Color(0,205,140), new Color(0,110,72));
        startBtn.setFont(new Font("Segoe UI", Font.BOLD, 22));
        startBtn.setPreferredSize(new Dimension(520, 70));

        startBtn.addActionListener(e -> {
            boolean t = tempCard.isSelected();
            boolean m = motionCard.isSelected();
            if (!t && !m) {
                JOptionPane.showMessageDialog(this, "Please select at least one sensor!", "No Sensor", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new Dashboard(t, m);
            dispose();
        });

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.add(startBtn);
        bottom.setBorder(new EmptyBorder(8,0,24,0));
        background.add(bottom, BorderLayout.SOUTH);

        setVisible(true);
    }
}

/* --------------------- SensorCard --------------------- */
class SensorCard extends JPanel {
    private boolean selected;
    private final Color accent;

    SensorCard(String title, String subtitle, Color accent, boolean defaultSelected) {
        this.accent = accent;
        this.selected = defaultSelected;

        setPreferredSize(new Dimension(360, 240));
        setLayout(new BorderLayout());
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new LineBorder(selected ? accent : new Color(120,120,120), selected ? 3 : 2, true));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(Color.WHITE);

        JLabel subLbl = new JLabel("<html><center><span style='color:#cfe8d9'>" + subtitle + "</span></center></html>", SwingConstants.CENTER);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel status = new JLabel(selected ? "Selected" : "Not selected", SwingConstants.CENTER);
        status.setFont(new Font("Segoe UI", Font.BOLD, 14));
        status.setForeground(selected ? accent : Color.LIGHT_GRAY);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleLbl, BorderLayout.NORTH);
        top.add(subLbl, BorderLayout.CENTER);

        add(top, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggle(); updateStatus(status); }
            @Override public void mouseEntered(MouseEvent e) { setBorder(new LineBorder(accent, 4, true)); }
            @Override public void mouseExited(MouseEvent e) { setBorder(new LineBorder(selected ? accent : new Color(120,120,120), selected ? 3 : 2, true)); }
        });
    }

    boolean isSelected() { return selected; }

    private void toggle() { selected = !selected; repaint(); }

    private void updateStatus(JLabel status) {
        status.setText(selected ? "Selected" : "Not selected");
        status.setForeground(selected ? accent : Color.LIGHT_GRAY);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg = selected ? new Color(30,40,55) : new Color(18,24,34);
        g2.setColor(bg);
        g2.fillRoundRect(0,0,getWidth(),getHeight(), 20,20);

        if (selected) {
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
            g2.fillRoundRect(6,6,getWidth()-12,getHeight()-12, 18,18);
        }
        g2.dispose();
    }
}

/* --------------------- Dashboard --------------------- */
class Dashboard extends JFrame {
    private final boolean enableTemp, enableMotion;
    private final DefaultListModel<String> deviceModel = new DefaultListModel<>();
    private final JTextArea logArea = new JTextArea();
    private final JLabel serverLabel = new JLabel("Server: STOPPED", SwingConstants.CENTER);
    private final JLabel devicesLabel = new JLabel("Devices: 0", SwingConstants.CENTER);

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> tempTask, motionTask;

    private final Deque<Sample> tempBuf = new ArrayDeque<>();
    private final Deque<Sample> motionBuf = new ArrayDeque<>();
    private final int MAX = 90;

    private final ValueCard tempCard = new ValueCard("Temperature", "— °C", new Color(72,201,176));
    private final ValueCard motionCard = new ValueCard("Motion", "—", new Color(245,158,11));

    Dashboard(boolean t, boolean m) {
        this.enableTemp = t;
        this.enableMotion = m;

        setTitle("IoT Dashboard");

        /* ---------- FULL SCREEN ENABLED ---------- */
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(250,250,252));
        setLayout(new BorderLayout(12,12));

        // Header
        JPanel header = new JPanel(new GridLayout(1,3));
        header.setBackground(new Color(33,37,41));
        JLabel logo = new JLabel("  •  IoT Simulator", SwingConstants.LEFT);
        logo.setForeground(Color.WHITE); logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        serverLabel.setForeground(Color.WHITE); serverLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        devicesLabel.setForeground(Color.WHITE); devicesLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.add(logo); header.add(serverLabel); header.add(devicesLabel);
        add(header, BorderLayout.NORTH);

        // Left device list
        JList<String> deviceList = new JList<>(deviceModel);
        deviceList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        add(cardWrap("Connected Devices", new JScrollPane(deviceList), 300), BorderLayout.WEST);

        // Center panel layout
        JPanel center = new JPanel(new BorderLayout(10,10));
        center.setOpaque(false);

        // Cards at top
        JPanel cards = new JPanel(new FlowLayout(FlowLayout.LEFT,12,8));
        cards.setOpaque(false);
        cards.add(tempCard);
        cards.add(motionCard);
        center.add(cards, BorderLayout.NORTH);

        if (enableTemp && enableMotion) {
            JPanel graphs = new JPanel(new GridLayout(1, 2, 12, 12));
            graphs.setOpaque(false);
            graphs.add(cardWrap("Temperature Sensor (°C)", new TempPanel(420), -1));
            graphs.add(cardWrap("Motion Sensor (Activity)", new MotionPanel(420), -1));
            center.add(graphs, BorderLayout.CENTER);
        } else if (enableTemp) {
            center.add(cardWrap("Temperature Sensor (°C)", new TempPanel(560), -1), BorderLayout.CENTER);
        } else {
            center.add(cardWrap("Motion Sensor (Activity)", new MotionPanel(560), -1), BorderLayout.CENTER);
        }

        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(0, 160));
        center.add(cardWrap("Activity Logs", logScroll, -1), BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        // Buttons bottom
        NeonButton startBtn = new NeonButton("START SERVER", new Color(0,220,140), new Color(0,110,70));
        NeonButton stopBtn  = new NeonButton("STOP SERVER", new Color(255,90,90), new Color(180,40,40));
        NeonButton clearBtn = new NeonButton("CLEAR LOGS", new Color(85,150,255), new Color(30,90,180));
        NeonButton backBtn  = new NeonButton("SELECT SENSORS", new Color(85,120,255), new Color(30,70,180));

        startBtn.setPreferredSize(new Dimension(170,46));
        stopBtn.setPreferredSize(new Dimension(170,46));
        clearBtn.setPreferredSize(new Dimension(150,46));
        backBtn.setPreferredSize(new Dimension(180,46));
        stopBtn.setEnabled(false);

        startBtn.addActionListener(e -> {
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            startSensors();
        });

        stopBtn.addActionListener(e -> {
            stopBtn.setEnabled(false);
            startBtn.setEnabled(true);
            stopSensors();
        });

        clearBtn.addActionListener(e -> logArea.setText(""));

        backBtn.addActionListener(e -> {
            stopSensors();
            dispose();
            SwingUtilities.invokeLater(StartScreen::new);
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER,12,12));
        bottom.setOpaque(false);
        bottom.add(startBtn);
        bottom.add(stopBtn);
        bottom.add(clearBtn);
        bottom.add(backBtn);

        add(bottom, BorderLayout.SOUTH);

        new javax.swing.Timer(200, e -> repaint()).start();

        seed();
        log("Dashboard ready. Press START SERVER to begin.");
        setVisible(true);
    }

    private void seed() {
        if (enableTemp) {
            double v = 22 + Math.random()*4;
            tempBuf.add(new Sample(System.currentTimeMillis(), v));
            tempCard.setValue(String.format("%.2f °C", v));
        }
        if (enableMotion) {
            int v = Math.random() > 0.5 ? 1 : 0;
            motionBuf.add(new Sample(System.currentTimeMillis(), v));
            motionCard.setValue(v==1 ? "DETECTED" : "NONE");
        }
    }

    private JPanel cardWrap(String title, JComponent comp, int width) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        JLabel lab = new JLabel(title);
        lab.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lab.setBorder(new EmptyBorder(6,6,6,6));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new EmptyBorder(10,10,10,10),
                                          new LineBorder(new Color(220,220,220),1,true)));
        card.add(comp, BorderLayout.CENTER);

        wrap.add(lab, BorderLayout.NORTH);
        wrap.add(card, BorderLayout.CENTER);

        if (width > 0) wrap.setPreferredSize(new Dimension(width, 0));
        return wrap;
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            String t = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
            logArea.append("[" + t + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void addDevice(String name) {
        SwingUtilities.invokeLater(() -> {
            if (!deviceModel.contains(name)) deviceModel.addElement(name);
            devicesLabel.setText("Devices: " + deviceModel.size());
        });
    }

    private void removeAllDevices() {
        SwingUtilities.invokeLater(() -> {
            deviceModel.clear();
            devicesLabel.setText("Devices: 0");
        });
    }

    private void startSensors() {
        if (executor != null && !executor.isShutdown()) return;

        executor = Executors.newScheduledThreadPool(3);
        serverLabel.setText("Server: RUNNING");
        serverLabel.setForeground(new Color(72,201,176));
        log("Server started.");

        if (enableTemp) {
            addDevice("TempSensor-1");
            tempTask = executor.scheduleAtFixedRate(() -> {
                double val = 20 + Math.random()*10;
                synchronized (tempBuf) {
                    if (tempBuf.size() >= MAX) tempBuf.removeFirst();
                    tempBuf.addLast(new Sample(System.currentTimeMillis(), val));
                }
                tempCard.setValue(String.format("%.2f °C", val));
                log("TempSensor-1 → " + String.format("%.2f °C", val));
            }, 0, 2, TimeUnit.SECONDS);
        }

        if (enableMotion) {
            addDevice("MotionSensor-1");
            motionTask = executor.scheduleAtFixedRate(() -> {
                int v = Math.random() > 0.6 ? 1 : 0;
                synchronized (motionBuf) {
                    if (motionBuf.size() >= MAX) motionBuf.removeFirst();
                    motionBuf.addLast(new Sample(System.currentTimeMillis(), v));
                }
                motionCard.setValue(v==1 ? "DETECTED" : "NONE");
                log("MotionSensor-1 → " + (v==1 ? "DETECTED" : "NONE"));
            }, 0, 1500, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSensors() {
        log("Server stopped.");
        serverLabel.setForeground(Color.RED);
        serverLabel.setText("Server: STOPPED");

        try {
            if (tempTask != null) tempTask.cancel(true);
            if (motionTask != null) motionTask.cancel(true);
            if (executor != null) executor.shutdownNow();
        } catch (Exception ignored) {}

        tempTask = null; 
        motionTask = null;
        removeAllDevices();
    }

    /* ---------------- Temperature Panel ---------------- */
    class TempPanel extends JPanel {
        private final Color lineColor = new Color(20,140,90);
        private final Color fillTop = new Color(72,201,176,200);
        private final Color fillBottom = new Color(72,201,176,40);

        TempPanel(int fixedHeight) {
            setPreferredSize(new Dimension(600, fixedHeight));
            setBackground(Color.WHITE);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            List<Sample> data;
            synchronized (tempBuf) { data = new ArrayList<>(tempBuf); }

            int w = getWidth(), h = getHeight();
            int left = 60, right = 20, top = 26, bottom = 40;
            int gw = w - left - right, gh = h - top - bottom;

            g2.setColor(new Color(248,248,249));
            g2.fillRect(0,0,w,h);

            g2.setColor(new Color(224,224,226));
            for (int i=0;i<=4;i++) {
                int y = top + i*gh/4;
                g2.drawLine(left, y, left+gw, y);
            }

            if (data.isEmpty()) {
                g2.setColor(new Color(130,130,130));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                g2.drawString("No temperature data yet.", left+8, top + gh/2);
                g2.dispose();
                return;
            }

            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (Sample s : data) {
                min = Math.min(min, s.v);
                max = Math.max(max, s.v);
            }
            if (min == max) { min -= 1; max += 1; }

            g2.setColor(new Color(70,70,70));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            for (int i=0;i<=4;i++) {
                double val = max - i*(max-min)/4.0;
                int yy = top + i*gh/4;
                g2.drawString(String.format("%.1f°C", val), 8, yy+4);
            }

            Path2D.Double path = new Path2D.Double();
            Path2D.Double area = new Path2D.Double();
            long t0 = data.get(0).t;
            long t1 = data.get(data.size()-1).t;
            long dt = Math.max(1, t1 - t0);

            for (int i=0;i<data.size();i++) {
                Sample s = data.get(i);
                double nx = left + ((double)(s.t - t0) / dt) * gw;
                double ny = top + (max - s.v) / (max - min) * gh;

                if (i==0) {
                    path.moveTo(nx, ny);
                    area.moveTo(nx, top+gh);
                    area.lineTo(nx, ny);
                } else {
                    path.lineTo(nx, ny);
                    area.lineTo(nx, ny);
                }
                if (i == data.size()-1) {
                    area.lineTo(nx, top+gh);
                    area.closePath();
                }
            }

            Rectangle bounds = area.getBounds();
            g2.setPaint(new GradientPaint(0, bounds.y, fillTop, 0, bounds.y + bounds.height, fillBottom));
            g2.fill(area);

            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(path);

            for (Sample s : data) {
                double nx = left + ((double)(s.t - t0) / dt) * gw;
                double ny = top + (max - s.v) / (max - min) * gh;

                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(nx-4, ny-4, 8, 8));
                g2.setColor(lineColor.darker());
                g2.draw(new Ellipse2D.Double(nx-4, ny-4, 8, 8));
            }

            Sample last = data.get(data.size()-1);
            g2.setColor(new Color(30,30,30));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString("Latest: " + String.format("%.2f °C", last.v), left+6, top+18);

            g2.dispose();
        }
    }

    /* ---------------- Motion Panel ---------------- */
    class MotionPanel extends JPanel {
        MotionPanel(int fixedHeight) {
            setPreferredSize(new Dimension(600, fixedHeight));
            setBackground(Color.WHITE);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            List<Sample> data;
            synchronized (motionBuf) { data = new ArrayList<>(motionBuf); }

            int w = getWidth(), h = getHeight();
            int left = 52, right = 20, top = 28, bottom = 40;
            int gw = w - left - right, gh = h - top - bottom;
            int yHigh = top + (int)(gh * 0.28);
            int yLow  = top + (int)(gh * 0.72);

            g2.setColor(new Color(248,248,249));
            g2.fillRect(0,0,w,h);

            if (data.isEmpty()) {
                g2.setColor(new Color(130,130,130));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                g2.drawString("No motion data yet.", left+8, top + gh/2);
                g2.dispose();
                return;
            }

            g2.setColor(new Color(230,230,235));
            g2.drawLine(left, yHigh, left+gw, yHigh);
            g2.drawLine(left, yLow, left+gw, yLow);

            long t0 = data.get(0).t;
            long t1 = data.get(data.size()-1).t;
            long dt = Math.max(1, t1 - t0);

            for (int r = 8; r >= 4; r -= 2) {
                g2.setStroke(new BasicStroke(r, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(245,158,11, 30 + (r*6)));
                drawStep(g2, data, left, gw, t0, dt, yHigh, yLow);
            }

            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Paint old = g2.getPaint();
            g2.setPaint(new GradientPaint(0, top, new Color(255,190,80), 0, top + gh, new Color(245,120,20)));
            drawStep(g2, data, left, gw, t0, dt, yHigh, yLow);
            g2.setPaint(old);

            for (Sample s : data) {
                int cx = left + (int)(((s.t - t0) / (double)dt) * gw);
                int cy = s.v >= 0.5 ? yHigh : yLow;

                g2.setColor(new Color(245,158,11, 100));
                g2.fill(new Ellipse2D.Double(cx-8, cy-8, 16, 16));

                g2.setColor(new Color(30,30,30));
                g2.fill(new Ellipse2D.Double(cx-4, cy-4, 8, 8));
            }

            Sample last = data.get(data.size()-1);
            g2.setColor(new Color(30,30,30));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString("Latest: " + (last.v >= 0.5 ? "Motion DETECTED" : "No Motion"), left+8, top+16);

            g2.dispose();
        }

        private void drawStep(Graphics2D g2, List<Sample> data, int left, int gw, long t0, long dt, int yHigh, int yLow) {
            int prevX = left;
            int prevY = data.get(0).v >= 0.5 ? yHigh : yLow;

            for (Sample s : data) {
                int cx = left + (int)(((s.t - t0) / (double)dt) * gw);
                int cy = s.v >= 0.5 ? yHigh : yLow;

                g2.drawLine(prevX, prevY, cx, prevY);
                g2.drawLine(cx, prevY, cx, cy);

                prevX = cx;
                prevY = cy;
            }
        }
    }
}

/* --------------------- Shared Helpers --------------------- */
class Sample {
    long t;
    double v;
    Sample(long t, double v) { this.t = t; this.v = v; }
}

class GradientPanel extends JPanel {
    private final Color a,b;
    GradientPanel(Color a, Color b) { this.a=a; this.b=b; setOpaque(true); }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0,0,a,0,getHeight(),b));
        g2.fillRect(0,0,getWidth(),getHeight());
    }
}

class ValueCard extends JPanel {
    private final JLabel value = new JLabel();
    ValueCard(String name, String initial, Color accent) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(220,72));
        setBackground(Color.WHITE);
        setBorder(new CompoundBorder(new EmptyBorder(10,12,10,12),
                new LineBorder(new Color(230,230,230),1,true)));

        JLabel t = new JLabel(name);
        t.setFont(new Font("Segoe UI", Font.BOLD, 14));
        value.setText(initial);
        value.setFont(new Font("Segoe UI", Font.BOLD, 18));
        value.setForeground(accent.darker());

        add(t, BorderLayout.NORTH);
        add(value, BorderLayout.CENTER);
    }

    void setValue(String v) {
        SwingUtilities.invokeLater(() -> value.setText(v));
    }
}

class NeonButton extends JButton {
    private final Color base, glow;

    NeonButton(String text, Color base, Color glow) {
        super(text);
        this.base = base;
        this.glow = glow;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setForeground(Color.WHITE);
        setFont(new Font("Segoe UI", Font.BOLD, 14));
        setBorder(new EmptyBorder(8,18,8,18));

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
            public void mouseExited(MouseEvent e) { setCursor(Cursor.getDefaultCursor()); }
        });
    }

    @Override protected void paintComponent(Graphics g) {
        int w=getWidth(), h=getHeight();
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint gp = new GradientPaint(0,0, base.brighter(), 0, h, base.darker());
        g2.setPaint(gp);
        g2.fillRoundRect(0,0,w,h,14,14);

        g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 90));
        for(int i=0;i<3;i++)
            g2.drawRoundRect(2-i,2-i,w-4+2*i,h-4+2*i,16,16);

        g2.setColor(Color.WHITE);
        FontMetrics fm=g2.getFontMetrics();
        int tw=fm.stringWidth(getText()), th=fm.getAscent();
        g2.setFont(getFont());
        g2.drawString(getText(), (w-tw)/2, (h+th)/2 - 3);

        g2.dispose();
    }
}
