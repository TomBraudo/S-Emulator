package customComponents;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.Pane;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;

public class ShrinkWrapPane extends Pane {

    private DoubleProperty hgap = new SimpleDoubleProperty(this, "hgap", 20);
    private DoubleProperty vgap = new SimpleDoubleProperty(this, "vgap", 20);

    public ShrinkWrapPane() {
        super();
    }

    public final double getHgap() {
        return hgap.get();
    }

    public final void setHgap(double value) {
        hgap.set(value);
        requestLayout();
    }

    public DoubleProperty hgapProperty() {
        return hgap;
    }

    public final double getVgap() {
        return vgap.get();
    }

    public final void setVgap(double value) {
        vgap.set(value);
        requestLayout();
    }

    public DoubleProperty vgapProperty() {
        return vgap;
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth() - getInsets().getLeft() - getInsets().getRight();
        double xStart = getInsets().getLeft();
        double y = getInsets().getTop();
        double vgap = this.vgap.get();
        double hgap = this.hgap.get();

        if (getChildren().isEmpty()) {
            return;
        }

        // Phase 1: Compute lines (each line is a list of Nodes)
        List<List<Node>> lines = new ArrayList<>();
        List<Node> currentLine = new ArrayList<>();
        double currentLineWidth = 0;

        for (Node child : getChildren()) {
            double childPrefWidth = child.prefWidth(-1);

            double childWidthWithGap = currentLine.isEmpty() ? childPrefWidth : childPrefWidth + hgap;

            if (currentLineWidth + childWidthWithGap > width && !currentLine.isEmpty()) {
                // Start new line
                lines.add(currentLine);
                currentLine = new ArrayList<>();
                currentLineWidth = 0;
                childWidthWithGap = childPrefWidth; // no hgap for first child in line
            }

            currentLine.add(child);
            currentLineWidth += childWidthWithGap;
        }
        // add last line
        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }

        // Phase 2: Layout each line
        for (List<Node> line : lines) {
            double totalPrefWidth = 0;
            for (Node child : line) {
                totalPrefWidth += child.prefWidth(-1);
            }

            if (line.size() == 1) {
                // Single child in line - shrink if needed
                Node child = line.get(0);
                double assignedWidth = Math.min(child.prefWidth(-1), width);
                double assignedHeight = child.prefHeight(assignedWidth);
                child.resizeRelocate(xStart, y, assignedWidth, assignedHeight);
                y += assignedHeight + vgap;

            } else {
                // Multiple children - normal flow layout, do not shrink children
                double x = xStart;
                double maxHeightInLine = 0;

                for (Node child : line) {
                    double childWidth = child.prefWidth(-1);
                    double childHeight = child.prefHeight(childWidth);

                    // If child does not fit in remaining space, move to next line (should not normally happen here)
                    if (x + childWidth > xStart + width) {
                        x = xStart;
                        y += maxHeightInLine + vgap;
                        maxHeightInLine = 0;
                    }

                    child.resizeRelocate(x, y, childWidth, childHeight);

                    x += childWidth + hgap;

                    if (childHeight > maxHeightInLine) {
                        maxHeightInLine = childHeight;
                    }
                }
                y += maxHeightInLine + vgap;
            }
        }
    }


    @Override
    protected double computePrefWidth(double height) {
        double maxChildWidth = 0;
        for (Node child : getChildren()) {
            double prefWidth = child.prefWidth(-1);
            if (prefWidth > maxChildWidth) {
                maxChildWidth = prefWidth;
            }
        }
        int count = getChildren().size();
        return maxChildWidth * count + hgap.get() * (count - 1) + getInsets().getLeft() + getInsets().getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        double x = getInsets().getLeft();
        double y = getInsets().getTop();
        double maxHeightInLine = 0;

        if (getChildren().isEmpty()) {
            return getInsets().getTop() + getInsets().getBottom();
        }

        for (Node child : getChildren()) {
            double childWidth = child.prefWidth(-1);

            if (x + childWidth > width) {
                x = getInsets().getLeft();
                y += maxHeightInLine + vgap.get();
                maxHeightInLine = 0;
            }
            x += childWidth + hgap.get();
            double childHeight = child.prefHeight(childWidth);

            if (childHeight > maxHeightInLine) {
                maxHeightInLine = childHeight;
            }
        }
        y += maxHeightInLine + getInsets().getBottom();
        return y;
    }
}
