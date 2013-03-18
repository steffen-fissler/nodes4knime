/*
 * Copyright (C) 2003 - 2013 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
 * 
 * This file is part of the KNIME CDK plugin.
 * 
 * The KNIME CDK plugin is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The KNIME CDK plugin is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with the plugin. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.openscience.cdk.knime.hydrogen;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.knime.base.node.parallel.appender.AppendColumn;
import org.knime.base.node.parallel.appender.ColumnDestination;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ReplaceColumn;
import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * This is the model for the hydrogen node that performs all computation by using CDK functionality.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class HydrogenAdderNodeModel extends ThreadedColAppenderNodeModel {

	private static final Map<String, String> NO_PROP_2D;

	static {
		Map<String, String> temp = new TreeMap<String, String>();
		temp.put(CDKCell.COORD2D_AVAILABLE, "false");
		NO_PROP_2D = Collections.unmodifiableMap(temp);
	}

	private final HydrogenAdderSettings m_settings = new HydrogenAdderSettings();

	/**
	 * Creates a new model having one input and one output node.
	 */
	public HydrogenAdderNodeModel() {

		super(1, 1);

		setMaxThreads(CDKNodeUtils.getMaxNumOfThreads());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		int molCol = inSpecs[0].findColumnIndex(m_settings.molColumnName());
		if (molCol == -1) {
			for (DataColumnSpec dcs : inSpecs[0]) {
				if (dcs.getType().isCompatible(CDKValue.class)) {
					if (molCol >= 0) {
						molCol = -1;
						break;
					} else {
						molCol = inSpecs[0].findColumnIndex(dcs.getName());
					}
				}
			}

			if (molCol != -1) {
				String name = inSpecs[0].getColumnSpec(molCol).getName();
				setWarningMessage("Using '" + name + "' as molecule column");
				m_settings.molColumnName(name);
			}
		}

		if (molCol == -1) {
			throw new InvalidSettingsException("Molecule column '" + m_settings.molColumnName() + "' does not exist");
		}

		DataColumnSpec[] colSpecs;
		if (m_settings.replaceColumn()) {
			colSpecs = new DataColumnSpec[inSpecs[0].getNumColumns()];
			for (int i = 0; i < colSpecs.length; i++) {
				colSpecs[i] = inSpecs[0].getColumnSpec(i);
			}
			DataColumnSpec newcol = createColSpec(inSpecs[0]);
			colSpecs[molCol] = newcol;
		} else {
			colSpecs = new DataColumnSpec[inSpecs[0].getNumColumns() + 1];
			for (int i = 0; i < colSpecs.length - 1; i++) {
				colSpecs[i] = inSpecs[0].getColumnSpec(i);
			}
			String name = m_settings.appendColumnName();
			if (name == null || name.length() == 0) {
				throw new InvalidSettingsException("Invalid name for appended column");
			}
			if (inSpecs[0].containsName(name)) {
				throw new InvalidSettingsException("Duplicate column name: " + name);
			}
			DataColumnSpec dc = createColSpec(inSpecs[0]);
			colSpecs[colSpecs.length - 1] = dc;

		}
		return new DataTableSpec[] { new DataTableSpec(colSpecs) };
	}

	/**
	 * Creates the column spec that is used to represent the new column, either replaced or appended.
	 * 
	 * @param in The original input spec.
	 * @return The single column spec to use.
	 */
	private DataColumnSpec createColSpec(final DataTableSpec in) {

		if (m_settings.replaceColumn()) {
			DataColumnSpec original = in.getColumnSpec(m_settings.molColumnName());
			DataColumnSpecCreator creator = new DataColumnSpecCreator(original);
			creator.setProperties(original.getProperties().cloneAndOverwrite(NO_PROP_2D));
			creator.setType(CDKCell.TYPE);
			return creator.createSpec();
		}
		DataColumnSpecCreator creator = new DataColumnSpecCreator(m_settings.appendColumnName(), CDKCell.TYPE);
		return creator.createSpec();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_settings.loadSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		m_settings.saveSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		HydrogenAdderSettings s = new HydrogenAdderSettings();
		s.loadSettings(settings);
		if ((s.molColumnName() == null) || (s.molColumnName().length() == 0)) {
			throw new InvalidSettingsException("No molecule column chosen");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExtendedCellFactory[] prepareExecute(final DataTable[] data) throws Exception {

		final int molColIndex = data[0].getDataTableSpec().findColumnIndex(m_settings.molColumnName());
		ExtendedCellFactory cf = new ExtendedCellFactory() {

			@Override
			public DataCell[] getCells(final DataRow row) {

				DataCell molCell = row.getCell(molColIndex);
				if (molCell.isMissing()) {
					return new DataCell[] { DataType.getMissingCell() };
				}

				try {
					IAtomContainer newMol = (IAtomContainer) ((CDKValue) molCell).getAtomContainer();
					if (m_settings.excludeStereo()) newMol = AtomContainerManipulator.removeNonChiralHydrogens(newMol);
					else newMol = AtomContainerManipulator.removeHydrogens(newMol);
					CDKNodeUtils.getStandardMolecule(newMol);
					CDKCell newCell = new CDKCell(newMol);
					return new DataCell[] { newCell };
				} catch (Throwable t) {
					return new DataCell[] { DataType.getMissingCell() };
				}
			}

			@Override
			public ColumnDestination[] getColumnDestinations() {

				if (m_settings.replaceColumn()) {
					return new ColumnDestination[] { new ReplaceColumn(molColIndex) };
				} else {
					return new ColumnDestination[] { new AppendColumn() };
				}
			}

			@Override
			public DataColumnSpec[] getColumnSpecs() {

				return new DataColumnSpec[] { createColSpec(data[0].getDataTableSpec()) };
			}
		};

		return new ExtendedCellFactory[] { cf };
	}
}
