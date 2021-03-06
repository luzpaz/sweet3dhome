/*
 * FurnitureController.java 15 mai 2006
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
package com.eteks.sweethome3d.viewcontroller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * A MVC controller for the furniture table.
 * @author Emmanuel Puybaret
 */
public class FurnitureController {
  private Home                home;
  private View                furnitureView;
  private ResourceBundle      resource;
  private UndoableEditSupport undoSupport;

  /**
   * Creates the controller of home furniture view.
   * @param viewFactory factory able to create views
   * @param home the home edited by this controller and its view
   * @param preferences the preferences of the application
   */
  public FurnitureController(ViewFactory viewFactory, Home home,
                             UserPreferences preferences) {
    this(viewFactory, home, preferences, null);
  }

  /**
   * Creates the controller of home furniture view.
   * @param viewFactory factory able to create views
   * @param home the home edited by this controller and its view
   * @param preferences the preferences of the application
   * @param undoSupport support for undoable operations
   */
  public FurnitureController(ViewFactory viewFactory, Home home, 
                             UserPreferences preferences, 
                             UndoableEditSupport undoSupport) {
    this.home = home;
    this.undoSupport = undoSupport;
    this.resource    = ResourceBundle.getBundle(
        FurnitureController.class.getName());
    this.furnitureView = viewFactory.createFurnitureView(home, preferences, this);
  }

  /**
   * Returns the view associated with this controller.
   */
  public View getView() {
    return this.furnitureView;
  }

  /**
   * Controls new furniture added to home. 
   * Once added the furniture will be selected in view 
   * and undo support will receive a new undoable edit.
   * @param furniture the furniture to add.
   */
  public void addFurniture(List<HomePieceOfFurniture> furniture) {
    final List<Object> oldSelection = 
      this.home.getSelectedItems(); 
    final HomePieceOfFurniture [] newFurniture = furniture.
        toArray(new HomePieceOfFurniture [furniture.size()]);
    // Get indices of furniture add to home
    final int [] furnitureIndex = new int [furniture.size()];
    int endIndex = home.getFurniture().size();
    for (int i = 0; i < furnitureIndex.length; i++) {
      furnitureIndex [i] = endIndex++; 
    }
  
    doAddFurniture(newFurniture, furnitureIndex); 
    if (this.undoSupport != null) {
      UndoableEdit undoableEdit = new AbstractUndoableEdit() {
        @Override
        public void undo() throws CannotUndoException {
          super.undo();
          doDeleteFurniture(newFurniture); 
          home.setSelectedItems(oldSelection); 
        }
        
        @Override
        public void redo() throws CannotRedoException {
          super.redo();
          doAddFurniture(newFurniture, furnitureIndex); 
        }
        
        @Override
        public String getPresentationName() {
          return resource.getString("undoAddFurnitureName");
        }
      };
      this.undoSupport.postEdit(undoableEdit);
    }
  }
  
  private void doAddFurniture(HomePieceOfFurniture [] furniture,
                              int [] furnitureIndex) { 
    for (int i = 0; i < furnitureIndex.length; i++) {
      this.home.addPieceOfFurniture (furniture [i], 
                                     furnitureIndex [i]);
    }
    this.home.setSelectedItems(Arrays.asList(furniture)); 
  }
  
  /**
   * Controls the deletion of the current selected furniture in home.
   * Once the selected furniture is deleted, undo support will receive a new undoable edit.
   */
  public void deleteSelection() {
    List<HomePieceOfFurniture> homeFurniture = 
        this.home.getFurniture(); 
    // Sort the selected furniture in the ascending order of their index in home
    Map<Integer, HomePieceOfFurniture> sortedMap = 
        new TreeMap<Integer, HomePieceOfFurniture>(); 
    for (Object item : this.home.getSelectedItems()) {
      if (item instanceof HomePieceOfFurniture) {
        HomePieceOfFurniture piece = (HomePieceOfFurniture)item;
        sortedMap.put(homeFurniture.indexOf(piece), piece); 
      }
    }
    final HomePieceOfFurniture [] furniture = sortedMap.values().
        toArray(new HomePieceOfFurniture [sortedMap.size()]); 
    final int [] furnitureIndex = new int [furniture.length];
    int i = 0;
    for (int index : sortedMap.keySet()) {
      furnitureIndex [i++] = index; 
    }
    doDeleteFurniture(furniture); 
    if (this.undoSupport != null) {
      UndoableEdit undoableEdit = new AbstractUndoableEdit() {
        @Override
        public void undo() throws CannotUndoException {
          super.undo();
          doAddFurniture(furniture, furnitureIndex); 
        }
        
        @Override
        public void redo() throws CannotRedoException {
          super.redo();
          home.setSelectedItems(Arrays.asList(furniture));
          doDeleteFurniture(furniture); 
        }
        
        @Override
        public String getPresentationName() {
          return resource.getString("undoDeleteSelectionName");
        }
      };
      this.undoSupport.postEdit(undoableEdit);
    }
  }
  
  private void doDeleteFurniture(
                      HomePieceOfFurniture [] furniture) { 
    for (HomePieceOfFurniture piece : furniture) {
  
      this.home.deletePieceOfFurniture(piece);
    }
  }

  /**
   * Updates the selected furniture in home.
   */
  public void setSelectedFurniture(
           List<HomePieceOfFurniture> selectedFurniture) {
    this.home.setSelectedItems(selectedFurniture);
  }
}
