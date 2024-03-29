package us.ilite.display.simulation.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import us.ilite.common.lib.geometry.Pose2d;
import us.ilite.common.lib.geometry.Translation2d;

import java.util.ArrayList;
import java.util.List;

public class DrawablePath extends ADrawable {

    private final Color kLineColor;
    private List<Translation2d> mPointList = new ArrayList<>();

    public DrawablePath(Color pLineColor) {
        kLineColor = pLineColor;
    }

    @Override
    public void draw(GraphicsContext gc, Pose2d pose, Translation2d aspectRatio) {
        mPointList.add(pose.translation_);

        gc.moveTo(mPointList.get(0).x(), mPointList.get(0).y());
        gc.setStroke(kLineColor);
        gc.beginPath();
        for(Translation2d point : mPointList) {
            Translation2d pointToDraw = new Translation2d(point.x() * aspectRatio.x(), point.y() * aspectRatio.y());
            gc.lineTo(pointToDraw.x(), pointToDraw.y());
        }
        gc.stroke();
        gc.closePath();
    }

    @Override
    public void draw(GraphicsContext gc, Pose2d pose) {
        draw(gc, pose, new Translation2d(1.0, 1.0));
    }

    public void clear() {
        mPointList.clear();
    }

}
