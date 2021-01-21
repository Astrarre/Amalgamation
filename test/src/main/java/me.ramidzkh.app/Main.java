package me.ramidzkh.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Main {
	public static void main(String[] args) {
		System.out.println(new Gson().getAdapter(TypeToken.get(Main.class)));
	}
}