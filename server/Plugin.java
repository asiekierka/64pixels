package server;

import java.lang.reflect.*;

public class Plugin
{
	private Class plugin;
	private Object instance;
	
	//Methods
	private Method pluginOnEnable;
	private Method pluginOnDisable;
	
	public Plugin(Class plugin) throws InstantiationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException
	{
		this.plugin = plugin;
		this.instance = plugin.newInstance();
		
		pluginOnEnable = plugin.getDeclaredMethod("onEnable");
		pluginOnDisable = plugin.getDeclaredMethod("onDisable");
	}
	
	void onEnable()
	{
		try {pluginOnEnable.invoke(instance);}
		catch (Exception e) {System.out.println("onEnable error!");}
	}
	
	void onDisable()
	{
		try {pluginOnDisable.invoke(instance);}
		catch (Exception e) {System.out.println("onDisable error!");}
	}
}