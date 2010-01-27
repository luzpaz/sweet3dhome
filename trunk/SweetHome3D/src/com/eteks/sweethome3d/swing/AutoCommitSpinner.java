/*
 * AutoSelectSpinner.java 10 sept. 2008
 *
 * Copyright (c) 2008 Emmanuel PUYBARET / eTeks <info@eteks.com>. All Rights Reserved.
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
package com.eteks.sweethome3d.swing;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.Format.Field;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

/**
 * A spinner which commits its value during edition and selects 
 * the value displayed in its editor when it gains focus.
 * @author Emmanuel Puybaret
 */
public class AutoCommitSpinner extends JSpinner {
  /**
   * Creates a spinner with a given <code>model</code>.
   */
  public AutoCommitSpinner(SpinnerModel model) {
    super(model);    
    JComponent editor = getEditor();
    if (editor instanceof JSpinner.DefaultEditor) {
      final JFormattedTextField textField = ((JSpinner.DefaultEditor)editor).getTextField();      
      SwingTools.addAutoSelectionOnFocusGain(textField);
      
      // Commit text during edition
      if (textField.getFormatterFactory() instanceof DefaultFormatterFactory) {
        DefaultFormatterFactory formatterFactory = (DefaultFormatterFactory)textField.getFormatterFactory();
        AbstractFormatter defaultFormatter = formatterFactory.getDefaultFormatter();
        if (defaultFormatter instanceof DefaultFormatter) {
          ((DefaultFormatter)defaultFormatter).setCommitsOnValidEdit(true);
        }
        if (defaultFormatter instanceof NumberFormatter) {
          final NumberFormatter numberFormatter = (NumberFormatter)defaultFormatter;
          final DecimalFormat defaultFormat = (DecimalFormat)numberFormatter.getFormat();
          final DecimalFormat noGroupingFormat = (DecimalFormat)defaultFormat.clone();
          noGroupingFormat.setGroupingUsed(false);
          // Create a delegate of default formatter to change value returned by getFormat
          NumberFormatter editFormatter = new NumberFormatter() {
              @Override
              public Format getFormat() {
                // Use a different format depending on whether the text field has focus or not
                if (textField.hasFocus()) {
                  // No grouping when text field has focus 
                  return noGroupingFormat;
                } else {
                  return defaultFormat;
                }
              }
            
              @Override
              public boolean getAllowsInvalid() {
                return numberFormatter.getAllowsInvalid();
              }
              
              @Override
              public boolean getCommitsOnValidEdit() {
                return numberFormatter.getCommitsOnValidEdit();
              }
              
              @Override
              public Field [] getFields(int offset) {
                return numberFormatter.getFields(offset);
              }
              
              @SuppressWarnings("unchecked")
              @Override
              public Comparable getMaximum() {
                return numberFormatter.getMaximum();
              }
              
              @SuppressWarnings("unchecked")
              @Override
              public Comparable getMinimum() {
                return numberFormatter.getMinimum();
              }
              
              @Override
              public boolean getOverwriteMode() {
                return numberFormatter.getOverwriteMode();
              }
              
              @Override
              public Class<?> getValueClass() {
                return numberFormatter.getValueClass();
              }
              
              @Override
              public void install(JFormattedTextField formattedTextField) {
                numberFormatter.install(formattedTextField);
              }
              
              @Override
              public void setAllowsInvalid(boolean allowsInvalid) {
                numberFormatter.setAllowsInvalid(allowsInvalid);
              }
              
              @SuppressWarnings("unchecked")
              @Override
              public void setMaximum(Comparable max) {
                numberFormatter.setMaximum(max);
              }
              
              @SuppressWarnings("unchecked")
              @Override
              public void setMinimum(Comparable minimum) {
                numberFormatter.setMinimum(minimum);
              }
              
              @Override
              public void setOverwriteMode(boolean overwriteMode) {
                numberFormatter.setOverwriteMode(overwriteMode);
              }
              
              @Override
              public void setCommitsOnValidEdit(boolean commit) {
                super.setCommitsOnValidEdit(commit);
              }
              
              @Override
              public void setValueClass(Class<?> valueClass) {
                numberFormatter.setValueClass(valueClass);
              }
              
              @Override
              public void uninstall() {
                numberFormatter.uninstall();
              }
              
              @Override
              public Object stringToValue(String text) throws ParseException {
                return super.stringToValue(text);
              }
            };
          textField.setFormatterFactory(new DefaultFormatterFactory(editFormatter));
        }
      }
    }
  }
}
