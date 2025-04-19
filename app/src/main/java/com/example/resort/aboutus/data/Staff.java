package com.example.resort.aboutus.data;

public class Staff {
    private String staffName;
    private String position;

    // Default constructor required for Firebase
    public Staff() {}

    public Staff(String staffName, String position) {
        this.staffName = staffName;
        this.position = position;
    }

    public String getStaffName() {
        return staffName;
    }

    public String getPosition() {
        return position;
    }
}

///Fix Current
//package com.example.resort.aboutus.data;
//
//public class Staff {
//    private String staffName;
//    private String position;
//
//    // Default constructor required for Firebase
//    public Staff() {}
//
//    public Staff(String staffName, String position) {
//        this.staffName = staffName;
//        this.position = position;
//    }
//
//    public String getStaffName() {
//        return staffName;
//    }
//
//    public String getPosition() {
//        return position;
//    }
//}
