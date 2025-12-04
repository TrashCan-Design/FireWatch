package com.example.firewatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BuildingConfig {

    public static class Room {
        public String id;
        public String name;
        public int floor;
        public int position;
        public boolean isStairs;
        public String type; // "classroom_end", "classroom_side", "stairs", "classroom_main"

        public Room(String id, String name, int floor, int position, boolean isStairs, String type) {
            this.id = id;
            this.name = name;
            this.floor = floor;
            this.position = position;
            this.isStairs = isStairs;
            this.type = type;
        }
    }

    public static class Floor {
        public int floorNumber;
        public String floorName;
        public List<Room> rooms;

        public Floor(int number, String name) {
            this.floorNumber = number;
            this.floorName = name;
            this.rooms = new ArrayList<>();
        }
    }

    private static final int TOTAL_FLOORS = 8;
    private static final int MAIN_ROOMS_PER_FLOOR = 12; // Main classrooms in center

    /**
     * Generates building with new layout:
     * Position 0: End Classroom (Left)
     * Position 1: Side Classroom (Left)
     * Position 2: Stairs (Left)
     * Position 3-14: Main Classrooms (12 rooms)
     * Position 15: Stairs (Right)
     * Position 16: Side Classroom (Right)
     * Position 17: End Classroom (Right)
     */
    public static List<Floor> generateBuilding() {
        List<Floor> building = new ArrayList<>();
        String[] floorNames = {"Ground", "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh"};

        for (int f = 0; f < TOTAL_FLOORS; f++) {
            int floorNum = f + 1;
            Floor floor = new Floor(floorNum, floorNames[f]);

            // Position 0: Left End Classroom
            String leftEndId = String.format("%dE1", floorNum);
            floor.rooms.add(new Room(
                    leftEndId,
                    "Classroom " + leftEndId,
                    floorNum,
                    0,
                    false,
                    "classroom_end"
            ));

            // Position 1: Left Side Classroom
            String leftSideId = String.format("%dS1", floorNum);
            floor.rooms.add(new Room(
                    leftSideId,
                    "Classroom " + leftSideId,
                    floorNum,
                    1,
                    false,
                    "classroom_side"
            ));

            // Position 2: Left Stairs
            floor.rooms.add(new Room(
                    "STAIRS_L_" + floorNum,
                    "Stairs",
                    floorNum,
                    2,
                    true,
                    "stairs"
            ));

            // Position 3-14: Main Classrooms (12 rooms)
            for (int r = 1; r <= MAIN_ROOMS_PER_FLOOR; r++) {
                String roomId = String.format("%d%02d", floorNum, r);
                String roomName = "Classroom " + roomId;
                floor.rooms.add(new Room(
                        roomId,
                        roomName,
                        floorNum,
                        2 + r,
                        false,
                        "classroom_main"
                ));
            }

            // Position 15: Right Stairs
            floor.rooms.add(new Room(
                    "STAIRS_R_" + floorNum,
                    "Stairs",
                    floorNum,
                    15,
                    true,
                    "stairs"
            ));

            // Position 16: Right Side Classroom
            String rightSideId = String.format("%dS2", floorNum);
            floor.rooms.add(new Room(
                    rightSideId,
                    "Classroom " + rightSideId,
                    floorNum,
                    16,
                    false,
                    "classroom_side"
            ));

            // Position 17: Right End Classroom
            String rightEndId = String.format("%dE2", floorNum);
            floor.rooms.add(new Room(
                    rightEndId,
                    "Classroom " + rightEndId,
                    floorNum,
                    17,
                    false,
                    "classroom_end"
            ));

            building.add(floor);
        }

        return building;
    }

    /**
     * Get a map of all rooms by ID for quick lookup
     */
    public static Map<String, Room> getRoomMap() {
        Map<String, Room> map = new HashMap<>();
        List<Floor> building = generateBuilding();

        for (Floor floor : building) {
            for (Room room : floor.rooms) {
                map.put(room.id, room);
            }
        }

        return map;
    }

    /**
     * Get a specific floor by number
     */
    public static Floor getFloor(int floorNumber) {
        List<Floor> building = generateBuilding();
        if (floorNumber > 0 && floorNumber <= building.size()) {
            return building.get(floorNumber - 1);
        }
        return null;
    }

    /**
     * Get total number of rooms per floor
     */
    public static int getRoomsPerFloor() {
        return 18; // 14 classrooms + 2 stairs + 2 end classrooms
    }

    /**
     * Check if a room ID represents stairs
     */
    public static boolean isStairsRoom(String roomId) {
        return roomId != null && roomId.startsWith("STAIRS_");
    }
}