package com.example.firewatch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FloorMapView extends View {

    private Paint paintSafe, paintFire, paintStairs, paintLift, paintText, paintWall, paintBorder;
    private BuildingConfig.Floor floor;
    private Set<String> fireRooms;

    private float scale;
    private static final int BORDER_WIDTH = 3;

    private static class RoomSpec {
        RectF bounds;
        String label;
        String type;

        RoomSpec(float left, float top, float right, float bottom, String label, String type) {
            this.bounds = new RectF(left, top, right, bottom);
            this.label = label;
            this.type = type;
        }
    }

    public FloorMapView(Context context) { super(context); init(); }
    public FloorMapView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        fireRooms = new HashSet<>();
        scale = 1.0f;

        paintSafe = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSafe.setColor(Color.parseColor("#86efac"));
        paintSafe.setStyle(Paint.Style.FILL);

        paintFire = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintFire.setColor(Color.parseColor("#FF0000"));
        paintFire.setStyle(Paint.Style.FILL);

        paintStairs = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintStairs.setColor(Color.parseColor("#7DD3FC")); // Light blue color for stairs
        paintStairs.setStyle(Paint.Style.FILL);

        paintLift = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLift.setColor(Color.parseColor("#86efac"));
        paintLift.setStyle(Paint.Style.FILL);

        paintWall = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintWall.setColor(Color.BLACK);
        paintWall.setStyle(Paint.Style.STROKE);
        paintWall.setStrokeWidth(2);

        paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBorder.setColor(Color.BLACK);
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(BORDER_WIDTH);

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(Color.BLACK);
        paintText.setTextSize(16); // Increased base font size
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
    }

    public void setFloor(BuildingConfig.Floor floor, Set<String> fireRooms) {
        this.floor = floor;
        this.fireRooms = fireRooms != null ? fireRooms : new HashSet<>();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);

        // Use full width minus border
        float availableWidth = parentWidth - (BORDER_WIDTH * 2);
        scale = availableWidth / 1000f;

        // Scale text size proportionally with larger base size
        paintText.setTextSize(16 * scale);

        int width = parentWidth;
        int height = (int)(500 * scale) + (BORDER_WIDTH * 2);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);

        if (floor == null) return;

        // Draw the floor layout
        drawFloorLayout(canvas, floor.floorNumber);

        // Draw black border around entire view
        canvas.drawRect(
                BORDER_WIDTH / 2f,
                BORDER_WIDTH / 2f,
                getWidth() - BORDER_WIDTH / 2f,
                getHeight() - BORDER_WIDTH / 2f,
                paintBorder
        );
    }

    private void drawFloorLayout(Canvas canvas, int floorNumber) {
        List<RoomSpec> layout = createFloorLayout(floorNumber);

        for (RoomSpec spec : layout) {
            if ("wall".equals(spec.type)) {
                drawWall(canvas, spec);
                continue;
            }

            BuildingConfig.Room room = null;
            if (floor != null && floor.rooms != null) {
                for (BuildingConfig.Room r : floor.rooms) {
                    if (spec.label.equals(r.id) ||
                            (spec.label.equals("Stairs") && r.isStairs) ||
                            (spec.label.equals("Lift") && "Lift".equals(spec.label))) {
                        room = r;
                        break;
                    }
                }
            }

            drawRoom(canvas, spec, room);
        }
    }

    private List<RoomSpec> createFloorLayout(int floorNumber) {
        List<RoomSpec> specs = new ArrayList<>();
        float offsetX = BORDER_WIDTH;
        float offsetY = BORDER_WIDTH;

        String room1 = floorNumber + "01";
        String room2 = floorNumber + "02";
        String room3 = floorNumber + "03";
        String room4 = floorNumber + "04";
        String room5 = floorNumber + "05";
        String room6 = floorNumber + "06";
        String room7 = floorNumber + "07";
        String room8 = floorNumber + "08";
        String room9 = floorNumber + "09";
        String room10 = floorNumber + "10";
        String room11 = floorNumber + "11";

        // Room 1 - Bottom Left
        specs.add(new RoomSpec(
                offsetX + scale(50), offsetY + scale(280),
                offsetX + scale(110), offsetY + scale(380),
                room1, "room"
        ));

        // Room 2 - Lift
        specs.add(new RoomSpec(
                offsetX + scale(120), offsetY + scale(230),
                offsetX + scale(170), offsetY + scale(330),
                "Lift", "lift"
        ));

        // Room 3 - Next to Lift
        specs.add(new RoomSpec(
                offsetX + scale(180), offsetY + scale(270),
                offsetX + scale(270), offsetY + scale(330),
                room2, "room"
        ));

        // Room 4 - Tall room
        specs.add(new RoomSpec(
                offsetX + scale(275), offsetY + scale(220),
                offsetX + scale(350), offsetY + scale(330),
                room3, "room"
        ));

        // Room 5 - Bottom area left
        specs.add(new RoomSpec(
                offsetX + scale(85), offsetY + scale(405),
                offsetX + scale(165), offsetY + scale(455),
                room4, "room"
        ));

        // Room 6 - Stairs (bottom)
        specs.add(new RoomSpec(
                offsetX + scale(180), offsetY + scale(405),
                offsetX + scale(240), offsetY + scale(455),
                "Stairs", "stairs"
        ));

        // Room 7 - Bottom middle
        specs.add(new RoomSpec(
                offsetX + scale(250), offsetY + scale(405),
                offsetX + scale(325), offsetY + scale(455),
                room5, "room"
        ));

        // Room 8 - Bottom right of left section
        specs.add(new RoomSpec(
                offsetX + scale(355), offsetY + scale(405),
                offsetX + scale(455), offsetY + scale(455),
                room6, "room"
        ));

        // Vertical wall separator
        specs.add(new RoomSpec(
                offsetX + scale(435), offsetY + scale(340),
                offsetX + scale(435), offsetY + scale(395),
                "", "wall"
        ));

        // Room 9 - Middle section left
        specs.add(new RoomSpec(
                offsetX + scale(415), offsetY + scale(255),
                offsetX + scale(555), offsetY + scale(330),
                room7, "room"
        ));

        // Room 10 - Middle section center
        specs.add(new RoomSpec(
                offsetX + scale(570), offsetY + scale(255),
                offsetX + scale(660), offsetY + scale(330),
                room8, "room"
        ));

        // Room 11 - Middle section right
        specs.add(new RoomSpec(
                offsetX + scale(675), offsetY + scale(255),
                offsetX + scale(785), offsetY + scale(330),
                room9, "room"
        ));

        // Horizontal wall line - top
        specs.add(new RoomSpec(
                offsetX + scale(340), offsetY + scale(160),
                offsetX + scale(770), offsetY + scale(160),
                "", "wall"
        ));

        // Room 12 - Top Right
        specs.add(new RoomSpec(
                offsetX + scale(810), offsetY + scale(45),
                offsetX + scale(905), offsetY + scale(165),
                room10, "room"
        ));

        // Room 13 - Right side middle
        specs.add(new RoomSpec(
                offsetX + scale(900), offsetY + scale(185),
                offsetX + scale(975), offsetY + scale(260),
                room11, "room"
        ));

        // Room 14 - Stairs (right side)
        specs.add(new RoomSpec(
                offsetX + scale(808), offsetY + scale(280),
                offsetX + scale(903), offsetY + scale(395),
                "Stairs", "stairs"
        ));

        return specs;
    }

    private float scale(float value) {
        return value * scale;
    }

    private void drawRoom(Canvas canvas, RoomSpec spec, BuildingConfig.Room room) {
        RectF rect = spec.bounds;

        boolean isFire = false;
        if (room != null && fireRooms.contains(room.id)) {
            isFire = true;
        }

        Paint fillPaint;
        if (isFire) {
            fillPaint = paintFire;
        } else if ("stairs".equals(spec.type)) {
            fillPaint = paintStairs;
        } else if ("lift".equals(spec.type)) {
            fillPaint = paintLift;
        } else {
            fillPaint = paintSafe;
        }

        canvas.drawRect(rect, fillPaint);
        canvas.drawRect(rect, paintWall);

        if (spec.label != null && !spec.label.isEmpty()) {
            float textX = rect.centerX();
            float textY = rect.centerY() + (paintText.getTextSize() / 3);

            if (isFire) {
                paintText.setColor(Color.WHITE);
            } else {
                paintText.setColor(Color.BLACK);
            }

            canvas.drawText(spec.label, textX, textY, paintText);

            paintText.setColor(Color.BLACK);
        }
    }

    private void drawWall(Canvas canvas, RoomSpec spec) {
        RectF bounds = spec.bounds;
        canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.bottom, paintWall);
    }
}