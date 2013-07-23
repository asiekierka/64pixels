package server;

import com.eclipsesource.json.*;
import java.util.Scanner;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.io.File;
import java.net.URLClassLoader;
import java.net.URL;

public class PluginLoader
{
	private Plugin plugin;
	
	public PluginLoader(String jarFile)
	{
		try
		{
			File f = new File("plugins");
			if (!f.exists()) f.mkdir();
			
			String myJar = "plugins/" + jarFile;
			
			ZipFile zf = new ZipFile(myJar);
			ZipEntry ze = zf.getEntry("config.js");
			if (ze == null) throw new MalformedPluginException("Plugin " + jarFile + " has a malformed configuration!");
			
			JsonObject jo = JsonObject.readFrom(new Scanner(zf.getInputStream(ze)).useDelimiter("\\A").next());
			String classToLoad = jo.get("fullClassName").asString();
			
			URL[] a = {new File(myJar).toURL()};
			URLClassLoader child = new URLClassLoader(a, this.getClass().getClassLoader());
			Class type = Class.forName(classToLoad, true, child);
			plugin = new Plugin(type);
		}
		
		catch (Exception e)
		{
			System.out.println("Pluginload of " + jarFile + " failed!");
			e.printStackTrace();
		}
	}
	
	public Plugin getPlugin()
	{
		return plugin;
	}
}

class MalformedPluginException extends RuntimeException
{
	public MalformedPluginException(String msg)
	{
		super(msg);
	}
}