package com.soulknight.map;
//(vitdung) viết class này để xác định trạng thái của các phòng một
import javafx.geometry.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String name;
    private BoundingBox bound;
    private boolean isActived = false; //biến check main đã vào room chưa
    private boolean isCleared = false; // biến check tiêu diện quái
    private List<BoundingBox> doors = new ArrayList<>();

    public Room(String name, double x, double y, double width, double height) {
        this.name = name;
        this.bound = new BoundingBox(x, y, width, height);
    }
    //(vitdung) hàm nhận tọa độ cửa từ Json
    public void addDoorCoordinate(double x,double y, double width, double height) {
        this.doors.add(new BoundingBox(x, y, width, height));
    }

    public void update(double playerX, double playerY) {
        if (isCleared) return;
        //kiểm tra player vào room
        if (!isActived && bound.contains(playerX, playerY)) {
            activeRoom();
        }
        //kiểm tra kill hết enemy chưa
        if (isActived) {
            checkRoomClear();
        }
    }

    //hàm activeRoom để kích hoạt trạng thái chiến đấu trong room: đóng cửa, enemy di chuyển.
    private void activeRoom() {
        this.isActived = true;
        System.out.println(name + " is actived");

        closeDoors();
    }

    private void checkRoomClear() {
        boolean enemiesAllDead = false;

        if(enemiesAllDead) {
            this.isActived = false;
            this.isCleared = true;
            System.out.println(name + " clear all enemies");
            openDoors();
        }
    }
    private void closeDoors() {
        // code close doors ở đây.
        System.out.println("Close the door");
    }

    private void openDoors() {
        System.out.println("Open the door");
    }



    //getter và setter
    public String getName() {
        return name;
    }
    public BoundingBox getBound() {
        return bound;
    }
    public boolean isActived() {
        return isActived;
    }
    public boolean isCleared() {
        return isCleared;
    }
    public List<BoundingBox> getDoors() {
        return doors;
    }
}