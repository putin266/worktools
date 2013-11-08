package edu.tongji.putin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ArrayList<String> test = new ArrayList<String>();
		test.add("a");
		test.add("b");
		test.add("c");
		for(int i = 0 ;i < test.size();i++){
			System.out.println(test.get(i));
			test.remove(i--);
		}
		
	}

}
