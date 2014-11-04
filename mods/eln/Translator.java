package mods.eln;

import net.minecraft.client.resources.I18n;

public class Translator {

	public static String translate(String s){
		String r = I18n.format(s, new Object[]{null});
		/*{if(r == s){
			r = "Unnamed";
		}*/
		return r;
	}
	
}
