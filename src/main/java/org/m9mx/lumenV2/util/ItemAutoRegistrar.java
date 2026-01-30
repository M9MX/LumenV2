package org.m9mx.lumenV2.util;

import org.m9mx.lumenV2.item.CustomItem;
import org.m9mx.lumenV2.item.ItemRegistry;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Automatically discovers and registers all CustomItem subclasses in the items package
 * This uses reflection to scan for classes extending CustomItem
 */
public class ItemAutoRegistrar {
    
    private static final String ITEMS_PACKAGE = "org.m9mx.lumenV2.items";
    private static final ItemRegistry registry = ItemRegistry.getInstance();
    
    /**
     * Auto-register all CustomItem subclasses found in the items package
     * Call this in Lumen.onEnable() instead of manually registering each item
     */
    public static void autoRegisterItems() {
        try {
            ClassLoader loader = ItemAutoRegistrar.class.getClassLoader();
            String packagePath = ITEMS_PACKAGE.replace(".", "/");
            
            // Get the resource URL for the package
            Enumeration<URL> resources = loader.getResources(packagePath);
            
            if (!resources.hasMoreElements()) {
                System.err.println("[Lumen] Could not find items package: " + ITEMS_PACKAGE);
                return;
            }
            
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                
                if (resource.getProtocol().equals("file")) {
                    File directory = new File(resource.toURI());
                    if (directory.isDirectory()) {
                        scanDirectory(directory, ITEMS_PACKAGE, loader);
                    }
                } else if (resource.getProtocol().equals("jar")) {
                    String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                    // Decode URL-encoded path (handles spaces and special characters)
                    jarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8);
                    scanJar(jarPath, ITEMS_PACKAGE);
                }
            }
        } catch (Exception e) {
            System.err.println("[Lumen] Error auto-registering items: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void scanDirectory(File directory, String packageName, ClassLoader loader) {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".class")) {
                String className = file.getName().substring(0, file.getName().length() - 6);
                String fullClassName = packageName + "." + className;
                
                try {
                    Class<?> clazz = loader.loadClass(fullClassName);
                    if (isCustomItemSubclass(clazz)) {
                        registerItem(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    // Ignore
                }
            }
        }
    }
    
    private static void scanJar(String jarPath, String packageName) {
        try (JarFile jarFile = new JarFile(jarPath)) {
            String packagePath = packageName.replace(".", "/");
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                
                if (name.startsWith(packagePath) && name.endsWith(".class")) {
                    String className = name.substring(0, name.length() - 6).replace("/", ".");
                    
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (isCustomItemSubclass(clazz)) {
                            registerItem(clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        // Ignore
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Lumen] Error scanning jar file: " + e.getMessage());
        }
    }
    
    private static boolean isCustomItemSubclass(Class<?> clazz) {
        return CustomItem.class.isAssignableFrom(clazz) && 
               !clazz.equals(CustomItem.class) &&
               !clazz.isInterface();
    }
    
    private static void registerItem(Class<?> clazz) {
        try {
            // Try to instantiate with no-arg constructor
            CustomItem item = (CustomItem) clazz.getDeclaredConstructor().newInstance();
            
            // Only register if enabled
            if (item.isEnabled()) {
                registry.register(item);
                System.out.println("[Lumen] Registered item: " + item.getId() + " (" + clazz.getSimpleName() + ")");
            } else {
                System.out.println("[Lumen] Skipped disabled item: " + item.getId() + " (" + clazz.getSimpleName() + ")");
            }
        } catch (Exception e) {
            System.err.println("[Lumen] Could not instantiate " + clazz.getName() + ": " + e.getMessage());
        }
    }
}
