package com.example.resort;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class ViewPagerLanding extends FragmentStatePagerAdapter {

    public ViewPagerLanding(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new FragmentOne();
            case 1:
                return new FragmentThree();
            case 2:
                return new FragmentFour();
            case 3:
                return new FragmentTwo();
            default:
                //noinspection DataFlowIssue
                return null;
        }
    }

    @Override
    public int getCount() {
        return 4; /// Number of fragments
    }
}

///Fix Current
//package com.example.resort;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentStatePagerAdapter;
//
//public class ViewPagerLanding extends FragmentStatePagerAdapter {
//
//    public ViewPagerLanding(@NonNull FragmentManager fm) {
//        super(fm);
//    }
//
//    @NonNull
//    @Override
//    public Fragment getItem(int position) {
//        switch (position) {
//            case 0:
//                return new FragmentOne();
//            case 1:
//                return new FragmentThree();
//            case 2:
//                return new FragmentFour();
//            case 3:
//                return new FragmentTwo();
//            default:
//                //noinspection DataFlowIssue
//                return null;
//        }
//    }
//
//    @Override
//    public int getCount() {
//        return 4; /// Number of fragments
//    }
//}
