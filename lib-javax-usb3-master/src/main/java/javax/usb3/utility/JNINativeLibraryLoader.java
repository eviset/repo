/*
 * Copyright (C) 2014 Jesse Caulfield 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package javax.usb3.utility;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to load the lib USB native (JNI) library.
 *
 * @author Jesse Caulfield
 */
public final class JNINativeLibraryLoader {

  /**
   * The JNI library base name. This is a platform-specific compiled version of
   * the USB4Java JNI library.
   *
   * @see <a href="https://github.com/usb4java/libusb4java">libusb4java</a>
   */
  private static final String LIB_NAME = "libusb4java";

  //<editor-fold defaultstate="collapsed" desc="Various Constant Values">
  /**
   * Constant for OS X operating system.
   */
  private static final String OS_OSX = "osx";
  /**
   * Constant for OS X operating system.
   */
  private static final String OS_MACOSX = "macosx";
  /**
   * Constant for Linux operating system.
   */
  private static final String OS_LINUX = "linux";
  /**
   * Constant for Windows operating system.
   */
  private static final String OS_WINDOWS = "windows";
  /**
   * Constant for FreeBSD operating system.
   */
  private static final String OS_FREEBSD = "freebsd";
  /**
   * Constant for SunOS operating system.
   */
  private static final String OS_SUNOS = "sunos";
  /**
   * Constant for i386 architecture.
   */
  private static final String ARCH_I386 = "i386";
  /**
   * Constant for x86 architecture.
   */
  private static final String ARCH_X86 = "x86";
  /**
   * Constant for x86_64 architecture.
   */
  private static final String ARCH_X86_64 = "x86_64";
  /**
   * Constant for amd64 architecture.
   */
  private static final String ARCH_AMD64 = "amd64";

  /**
   * Constant for so file extension.
   */
  private static final String EXT_SO = "so";
  /**
   * Constant for dll file extension.
   */
  private static final String EXT_DLL = "dll";
  /**
   * Constant for dylib file extension.
   */
  private static final String EXT_DYLIB = "dylib";//</editor-fold>

  /**
   * Private constructor to prevent instantiation.
   */
  private JNINativeLibraryLoader() {
    // Nothing to do here
  }

  /**
   * Load the JNI USB interface native library.
   */

  private static void copyLibAndLoad(Path destination, String fileName){
    /**
     * If the (extracted) destination file does not exist then extract the
     * library from the jar file into a temp directory.
     */
    try {
      Files.createDirectories(destination.getParent());
      URL url = JNINativeLibraryLoader.class.getClassLoader().getResource("META-INF/nativelib/" + getOSName() + "/" + getOSArch() + "/" + fileName);
      if (url == null) {
        Logger.getLogger(JNINativeLibraryLoader.class.getName()).log(Level.SEVERE, "USB JNI library {0} not found for {1}", new Object[]{fileName, getOSArch()});
        throw new RuntimeException("USB JNI library " + getLibraryFilename() + " not found for " + getOSArch());
      }
      /**
       * Copy the (binary) file from within the JAR to the temporary directory.
       */
      Path source = null;
      try {
        source = Paths.get(url.toURI());
      }catch (FileSystemNotFoundException e){
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        FileSystem zipfs = FileSystems.newFileSystem(url.toURI(), env);
        source = Paths.get(url.toURI());
      }

      Logger.getLogger(JNINativeLibraryLoader.class.getName()).log(Level.FINE, "Copy USB native library from {0} to {1}", new Object[]{source, destination});
      Logger.getLogger(JNINativeLibraryLoader.class.getName()).log(Level.INFO, "Loading native lib {0}", source);
      Files.copy(source, destination);
      /**
       * Mark the file to be deleted upon exit to leave no trace.
       */
//      destination.toFile().deleteOnExit();
      /**
       * Load the native library and done.
       */
      System.load(destination.toString());
    } catch (URISyntaxException | IOException e) {
      Logger.getLogger(JNINativeLibraryLoader.class.getName()).log(Level.SEVERE, null, e);
    }

  }


  public static void load() {
    /**
     * If the library is already extracted then no work is required.
     */
    String libUsbName = "libusb-1.0.dll";
    Path libusbPath  = Paths.get(System.getProperty("java.io.tmpdir"), "javaxusb", getOSName(), getOSArch(), "libusb-1.0.dll");
    if (libusbPath .toFile().exists() && libusbPath .toFile().length() > 0) {
      System.load(libusbPath.toString());
    }else {
      copyLibAndLoad(libusbPath,libUsbName );
    }

    Path usb3libPath = Paths.get(System.getProperty("java.io.tmpdir"), "javaxusb", getOSName(), getOSArch(),getLibraryFilename());
    if (usb3libPath.toFile().exists() && libusbPath .toFile().length() > 0) {
      System.load(usb3libPath.toString());
    }else {
      copyLibAndLoad(usb3libPath,getLibraryFilename() );
    }


  }

  //<editor-fold defaultstate="collapsed" desc="Internal Helper Methods">
  /**
   * Returns the operating system name. This could be "linux", "windows" or
   * "osx" or (for any other non-supported platform) the value of the "os.name"
   * property converted to lower case and with removed space characters.
   *
   * @return The operating system name.
   */
  private static String getOSName() {
    final String os = System.getProperty("os.name").toLowerCase(Locale.US).replace(" ", "");
    if (os.contains(OS_WINDOWS)) {
      return OS_WINDOWS;
    }
    if (os.equals(OS_MACOSX)) {
      return OS_OSX;
    }
    return os;
  }

  /**
   * Returns the CPU architecture. This will be "x86" or "x86_64" (Platform
   * names i386 und amd64 are converted accordingly) or (when platform is
   * unsupported) the value of os.arch converted to lower-case and with removed
   * space characters.
   *
   * @return The CPU architecture
   */
  private static String getOSArch() {
    final String arch = System.getProperty("os.arch").toLowerCase(Locale.US).replace(" ", "");
    if (arch.equals(ARCH_I386)) {
      return ARCH_X86;
    }
    if (arch.equals(ARCH_AMD64)) {
      return ARCH_X86_64;
    }
    return arch;
  }

  /**
   * Returns the shared library extension name.
   *
   * @return The shared library extension name.
   */
  private static String getLibrarySuffix() {
    switch (getOSName()) {
      case OS_LINUX:
      case OS_FREEBSD:
      case OS_SUNOS:
        return EXT_SO;
      case OS_WINDOWS:
        return EXT_DLL;
      case OS_OSX:
        return EXT_DYLIB;
      default:
        throw new RuntimeException("Unrecognized or unsupported operating system: " + getOSName());
    }
  }

  /**
   * Returns the name of the JNA usb4java native library. This could be
   * "libusb4java.dll" for example.
   *
   * @return The usb4java native library name. Never null.
   */
  private static String getLibraryFilename() {
    return LIB_NAME + "." + getLibrarySuffix();
  }//</editor-fold>

}
