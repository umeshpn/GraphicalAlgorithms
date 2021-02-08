import javax.swing.*;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

public class BestCandidateVisualizer extends JFrame {
    private int width;
    private int height;

    BestCandidateVisualizer(int width, int height) {
        //Set JFrame title
        super("Best Candidate Visualizer");
        this.width = width;
        this.height = height;

        //Set default close operation for JFrame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Set JFrame size
        setSize(width, height);

        //Make JFrame visible
        setVisible(true);
    }


    public void paint(Graphics g) {
        g.clearRect(0, 0, this.width, this.height);

        BestCandidateFinder f = new BestCandidateFinder(width, height, 1, 32, 20, 20);
        System.out.println("Drawing...");
        for (;;) {
            BestCandidateFinder.Circle c = f.nextCircle();
            if (c == null) break;
            drawCircle(g, c);
        }

    }

    private void drawCircle(Graphics g, BestCandidateFinder.Circle c) {
        g.drawOval((int)c.getX(), (int)c.getY(), (int)c.getR(), (int)c.getR());
    }

    public static void main(String[] args) {
        BestCandidateVisualizer v = new BestCandidateVisualizer(420, 420);
    }
}
