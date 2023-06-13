package org.cloudsimplus.examples;

import java.util.*;

public class Main {
	  public static void main(String[] args) {
		  	Scanner sc= new Scanner(System.in);
		    ArrayList<String> cars = new ArrayList<String>();
			String marque=new String();
			HashMap<String, String> modeleVoiture = new HashMap<String, String>();
			modeleVoiture.put("ford", "focus");
			modeleVoiture.put("ford", "fiesta");
			
			System.out.println(modeleVoiture.get("ford") + "\t"+ modeleVoiture.size());
		    while(!marque.equals("fin"))
		    {
		    	System.out.println("ajoutez une voiture ou fin");
		        marque=sc.nextLine();
		        if(!marque.equals("fin"))
		        {
		        cars.add(marque);
		        }
		    }
		    if(!cars.isEmpty())
		    {
		    	cars.set(0, "ford");
		    	System.out.println(cars.size()+" qui sont disponible et qui sont:" + cars);
		    }else
		    	System.out.println("Aucune marque disponible");
		  } 
}
