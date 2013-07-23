package server;

import java.io.File;
import java.util.*;

public class PluginHandler
{
	private ArrayList<Plugin> plugins;
	
	public PluginHandler()
	{
		plugins = new ArrayList<Plugin>();
	}
	
	public void reloadPlugins()
	{
		File dir = new File("plugins");
		File[] files = dir.listFiles();
		
		File f;
		System.out.println(files);
		for (int i = 0; i < files.length; i++)
		{
			f = files[i];
			System.out.println("Loading plugin " + f.getName());
			Plugin p = new PluginLoader(f.getName()).getPlugin();
			p.onEnable();
			plugins.add(p);
		}
	}
}