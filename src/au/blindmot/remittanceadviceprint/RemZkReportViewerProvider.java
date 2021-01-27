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
package au.blindmot.remittanceadviceprint;

import org.zkoss.zk.ui.Desktop;
import java.io.FileInputStream;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.window.SimplePDFViewer;
import org.adempiere.webui.window.ZkReportViewerProvider;
import org.zkoss.zk.ui.Executions;
import org.compiere.util.CLogger;

/**
 * @author phil
 * Code copied from Adempiere org.compiere.print.ReportCtl
 *
 */

public class RemZkReportViewerProvider extends ZkReportViewerProvider {
	
	public static CLogger log;
	
	
	public static void openViewer(String  title, FileInputStream input)
	{
		  boolean inUIThread = Executions.getCurrent() != null;
		  boolean desktopActivated = false;
		  
		  Desktop desktop = null;
		  try {
		      if (!inUIThread) {
		    	  desktop = AEnv.getDesktop();
		    	  
		       if (desktop == null)
		       {
		        log.warning("desktop is null");
		        return;
		       }
		       //1 second timeout
		       if (Executions.activate(desktop, 1000)) {
		        desktopActivated = true;
		       } else {
		        log.warning("could not activate desktop");
		        return;
		       }
		      }
		   Window win = new SimplePDFViewer(title, input);
		   SessionManager.getAppDesktop().showWindow(win, "center");
		  } catch (Exception e) {
		     } finally {
		      if (!inUIThread && desktopActivated) {
		       Executions.deactivate(desktop);
		      }
		     }		
	}

}
