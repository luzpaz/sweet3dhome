/*
 * HomeFileRecorder.java 30 aout 2006
 *
 * Copyright (c) 2006 Emmanuel PUYBARET / eTeks <info@eteks.com>. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.eteks.sweethome3d.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeRecorder;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.tools.URLContent;

/**
 * Recorder that stores homes in files.
 * @author Emmanuel Puybaret
 */
public class HomeFileRecorder implements HomeRecorder {
  /**
   * Writes home data.
   * @throws RecorderException if a probleme occured while writing home.
   */
  public void writeHome(Home home, String name) throws RecorderException {
    HomeOutputStream out = null;
    try {
      // Open a stream on file name
      out = new HomeOutputStream(new FileOutputStream(name));
      // Write home with HomeOuputStream
      out.writeHome(home);
    } catch (IOException ex) {
      throw new RecorderException("Can't save home " + name, ex);
    } finally {
      try {
        if (out != null)
          out.close();
      } catch (IOException ex) {
        throw new RecorderException("Can't close file " + name, ex);
      }
    }
  }

  /**
   * Returns a home instance read from its file <code>name</code>.
   * @throws RecorderException if a probleme occured while reading home, 
   *   or if file <code>name</code> doesn't exist.
   */
  public Home readHome(String name) throws RecorderException {
    HomeInputStream in = null;
    try {
      // Open a buffered stream on file
      in = new HomeInputStream(new FileInputStream(name));
      // Read home with HomeInputStream
      Home home = in.readHome();
      return home;
    } catch (IOException ex) {
      throw new RecorderException("Can't read home from " + name, ex);
    } catch (ClassNotFoundException ex) {
      throw new RecorderException("Missing classes to read home from " + name, ex);
    } finally {
      try {
        if (in != null)
          in.close();
      } catch (IOException ex) {
        throw new RecorderException("Can't close file " + name, ex);
      }
    }
  }

  /**
   * Returns <code>true</code> if the file <code>name</code> exists.
   */
  public boolean exists(String name) throws RecorderException {
    return new File(name).exists();
  }

  /**
   * <code>OutputStream</code> filter that writes a home in a stream 
   * at .sh3d file format. 
   */
  private static class HomeOutputStream extends FilterOutputStream {
    private List<Content> contents = new ArrayList<Content>();
    
    public HomeOutputStream(OutputStream out) throws IOException {
      super(out);
    }

    /**
     * Writes home in a zipped stream followed by <code>Content</code> objets 
     * it points to.
     */
    public void writeHome(Home home) throws IOException {
      // Create a zip output on out stream 
      ZipOutputStream zipOut = new ZipOutputStream(this.out);
      zipOut.setLevel(0);
      // Write home in first entry in a file "Home"
      zipOut.putNextEntry(new ZipEntry("Home"));
      // Use an ObjectOutputStream that keeps track of Content objects
      ObjectOutputStream objectOut = new HomeObjectOutputStream(zipOut);
      objectOut.writeObject(home);
      objectOut.flush();
      zipOut.closeEntry();
      byte [] buffer = new byte [8096];
      // Write Content objects in files "0" to "n"
      for (int i = 0, n = contents.size(); i < n; i++) {
        InputStream contentIn = null;
        try {
          zipOut.putNextEntry(new ZipEntry(String.valueOf(i)));
          contentIn = contents.get(i).openStream();          
          int size; 
          while ((size = contentIn.read(buffer)) != -1) {
            zipOut.write(buffer, 0, size);
          }
          zipOut.closeEntry();  
        } finally {
          if (contentIn != null) {          
            contentIn.close();
          }
        }
      }  
      // Finish zip writing
      zipOut.finish();
    }

    /**
     * <code>ObjectOutputStream</code> that replaces <code>Content</code> objects
     * by temporary <code>URLContent</code> objects and stores them in a list.
     */
    private class HomeObjectOutputStream extends ObjectOutputStream {
      public HomeObjectOutputStream(OutputStream out) throws IOException {
        super(out);
        enableReplaceObject(true);
      }
  
      @Override
      protected Object replaceObject(Object obj) throws IOException {
        if (obj instanceof Content) {
          // Add obj to Content objects list
          contents.add((Content)obj);
          // Return a temporary URL that points to content object 
          return new URLContent(new URL("jar:file:temp!/" + (contents.size() - 1)));
        } else {
          return obj;
        }
      }
    }
  }

  /**
   * <code>InputStream</code> filter that reads a home from a stream 
   * at .sh3d file format. 
   */
  private static class HomeInputStream extends FilterInputStream {
    private File tempFile;

    /**
     * Creates a home input stream filter able to read a home and its content
     * from <code>in</code>.
     */
    public HomeInputStream(InputStream in) throws IOException {
      super(in);
    }

    /**
     * Reads home from a zipped stream.
     */
    public Home readHome() throws IOException, ClassNotFoundException {
      // Copy home stream in a temporary file 
      this.tempFile = File.createTempFile("open", ".sweethome3d");
      this.tempFile.deleteOnExit();
      OutputStream tempOut = null;
      try {
        tempOut = new FileOutputStream(this.tempFile);
        byte [] buffer = new byte [8096];
        int size; 
        while ((size = this.in.read(buffer)) != -1) {
          tempOut.write(buffer, 0, size);
        }
      } finally {
        if (tempOut != null) {
          tempOut.close();
        }
      }
      
      ZipInputStream zipIn = null;
      try {
        // Open a zip input from temp file
        zipIn = new ZipInputStream(new FileInputStream(this.tempFile));
        // Read home in first entry
        zipIn.getNextEntry();
        // Use an ObjectInputStream that replaces temporary URLs of Content objects 
        // by URLs relative to file 
        ObjectInputStream objectStream = new HomeObjectInputStream(zipIn);
        return (Home)objectStream.readObject();
      } finally {
        if (zipIn != null) {
          zipIn.close();
        }
      }
    }

    /**
     * <code>ObjectInputStream</code> that replaces temporary <code>URLContent</code> 
     * objects by <code>URLContent</code> objects that points to file.
     */
    private class HomeObjectInputStream extends ObjectInputStream {
      public HomeObjectInputStream(InputStream in) throws IOException {
        super(in);
        enableResolveObject(true);
      }
  
      @Override
      protected Object resolveObject(Object obj) throws IOException {
        if (obj instanceof URLContent) {
          URL tmpURL = ((URLContent)obj).getURL();
          // Replace "temp" in URL by current temporary file
          URL fileURL = new URL(tmpURL.toString().replace("temp", tempFile.toString()));
          return new URLContent(fileURL);
        } else {
          return obj;
        }
      }
    }
  }
}
