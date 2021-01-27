/******************************************************************************                                                                                                                                                                                                
 * Copyright (C) 2019 Blindmotion & Adaxa         							  *                                                                                                                                                                                                
 * This program is free software; you can redistribute it and/or modify it    *                                                                                                                                                                                                
 * under the terms version 2 of the GNU General Public License as published   *                                                                                                                                                                                                
 * by the Free Software Foundation. This program is distributed in the hope   *                                                                                                                                                                                                
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *                                                                                                                                                                                                
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *                                                                                                                                                                                                
 * See the GNU General Public License for more details.                       *                                                                                                                                                                                                
 * You should have received a copy of the GNU General Public License along    *                                                                                                                                                                                                
 * with this program; if not, write to the Free Software Foundation, Inc.,    *                                                                                                                                                                                                
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *                                                                                                                                                                                                
 *****************************************************************************/  
package au.blindmot.print.factory;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;
import org.compiere.util.CLogger;

import au.blindmot.remittanceadviceprint.RemittanceAdvicesPrint;

/**
 * @author Nikunj
 * Idempiere port Phil Barnett
 *
 */
public class PrintRemittanceFactory implements IProcessFactory {

	CLogger log = CLogger.getCLogger(PrintRemittanceFactory.class);

	/**
	 * 
	 */
	public PrintRemittanceFactory() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.adempiere.base.IProcessFactory#newProcessInstance(java.lang.String)
	 */
	@Override
	public ProcessCall newProcessInstance(String className) {
		if(className.equals("au.blindmot.remittanceadviceprint.RemittanceAdvicesPrint"))
			{
				log.warning("---------> au.blindmot.remittancefactory is loaded");
				return new RemittanceAdvicesPrint();
			}
		return null;
	}

}
