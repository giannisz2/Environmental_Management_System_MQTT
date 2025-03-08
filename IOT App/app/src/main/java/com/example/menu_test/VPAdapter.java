package com.example.menu_test;

import androidx.annotation.NonNull;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;


public class VPAdapter extends FragmentStateAdapter {


    private final ArrayList<Fragment> fragments = new ArrayList<>();
    private final ArrayList<String> fragmentTitles = new ArrayList<>();

    public VPAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);

        fragments.add(new smoke_fragment());
        fragmentTitles.add("Smoke");

        fragments.add(new gas_fragment());
        fragmentTitles.add("Gas");

    }


    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    public int getItemCount() {
        return fragments.size();
    }

    public void addFragment(Fragment fragment, String title) {
        fragments.add(fragment);
        fragmentTitles.add(title);
        notifyItemInserted(fragments.size() - 1);
    }

    public CharSequence getPageTitle(int position) {
        return fragmentTitles.get(position);
    }

}
