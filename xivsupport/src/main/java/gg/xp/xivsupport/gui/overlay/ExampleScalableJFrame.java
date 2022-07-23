package gg.xp.xivsupport.gui.overlay;

import gg.xp.xivsupport.gui.CommonGuiSetup;

import javax.swing.*;
import java.awt.*;

//@ScanMe
public class ExampleScalableJFrame {

	public ExampleScalableJFrame() {
	}

	public static void main(String[] args) throws InterruptedException {
		CommonGuiSetup.setup();
		ScalableJFrame frame = ScalableJFrameLinuxNoopImpl.construct("Foo");

		JPanel panel = new JPanel() {
//			@Override
//			public void paintComponent(Graphics g) {
//				((Graphics2D) g).setBackground(new Color(0, 255, 0, 0));
//				g.clearRect(0, 0, getWidth(), getHeight());
//				super.paintComponent(g);
//			}
//
		};

		panel.setLayout(new GridLayout(2, 2));
		JButton button1 = new JButton("Bigger");
		panel.add(button1);
		JButton button2 = new JButton("Smaller");
		panel.add(button2);
		JButton reset = new JButton("Reset");
		panel.add(reset);
		panel.setOpaque(false);
		panel.setBackground(new Color(200, 100, 0, 0));

		frame.setContentPane(panel);
		frame.setUndecorated(true);
		frame.setBackground(new Color(200, 100, 0, 0));
		frame.setSize(new Dimension(500, 500));
		frame.setLocationRelativeTo(null);

		frame.setVisible(true);

		Thread.sleep(5000);
		frame.repaint();
	}
}
